package xtages.console.controller.api

import mu.KotlinLogging
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.controller.GitHubUrl
import xtages.console.controller.api.model.*
import xtages.console.controller.api.model.Build.Status
import xtages.console.controller.model.CodeBuildType
import xtages.console.controller.model.buildEventPojoToBuildPhaseConverter
import xtages.console.controller.model.projectPojoToProjectConverter
import xtages.console.dao.fetchLatestBuildEventsOfProjects
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.dao.fetchOneByNameAndOrganization
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ExceptionCode.USER_NOT_FOUND
import xtages.console.exception.ensure
import xtages.console.query.enums.ProjectType
import xtages.console.query.tables.daos.BuildEventDao
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.query.tables.pojos.BuildEvent
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.XtagesUser
import xtages.console.service.AuthenticationService
import xtages.console.service.GitHubService
import xtages.console.service.aws.AwsService
import xtages.console.service.aws.CodeBuildService
import xtages.console.time.toUtcMillis
import xtages.console.query.tables.pojos.Project as ProjectPojo

private val logger = KotlinLogging.logger { }

@Controller
class ProjectApiController(
    private val userDao: XtagesUserDao,
    private val organizationDao: OrganizationDao,
    private val projectDao: ProjectDao,
    private val authenticationService: AuthenticationService,
    private val gitHubService: GitHubService,
    private val awsService: AwsService,
    private val codeBuildService: CodeBuildService,
    private val buildEventDao: BuildEventDao,
) : ProjectApiControllerBase {

    override fun getProjects(listProjectsReq: ListProjectsReq?): ResponseEntity<List<ProjectAndLastBuild>> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val projects = projectDao.fetchByOrganization(organization.name!!).toMutableList()
        if (listProjectsReq?.includeLastBuild == true) {
            val latestBuildEventsPerProject = buildEventDao.fetchLatestBuildEventsOfProjects(projects)
            val projectsWithBuild = latestBuildEventsPerProject.mapValues {
                val project = it.key
                projects.remove(project)
                val events = it.value
                val initialEvent =
                    events.single { event -> event.name == "SENT_TO_BUILD" && event.status == "STARTED" }
                val lastEvent = events.last()
                // We create a "virtual" Build object. This object has the id of the first BuildEvent and we use the
                // BuildEvents for a given CodeBuild build to determine if the "Build" as a whole was successful or has
                // some other status. We use the first BuildEvent's start time as the startTimestamp and the last
                // BuildEvent's end time as the endTimestamp
                Build(
                    buildId = initialEvent.id,
                    buildType = Build.BuildType.valueOf(initialEvent.operation!!),
                    status = determineBuildStatus(events),
                    initiatorName = "Carlos",
                    initiatorEmail = "czuniga@xtages.com",
                    commitHash = initialEvent.commit,
                    commitUrl = GitHubUrl(
                        organizationName = organization.name!!,
                        repoName = project.name,
                        commitHash = initialEvent.commit
                    ).toUriString(),
                    startTimestampInMillis = initialEvent.startTime!!.toUtcMillis(),
                    endTimestampInMillis = lastEvent.endTime!!.toUtcMillis(),
                    phases = events.map { event -> buildEventPojoToBuildPhaseConverter.convert(event)!! }
                )
            }.mapKeys { entry ->
                projectPojoToProjectConverter.convert(entry.key)
            }.map { entry ->
                ProjectAndLastBuild(
                    project = entry.key,
                    lastBuild = entry.value
                )
            }
            val projectsWithoutBuild = projects
                .sortedBy { it.name }
                .map { projectPojoToProjectConverter.convert(it)!! }
                .map { ProjectAndLastBuild(project = it) }
            return ResponseEntity.ok(projectsWithBuild + projectsWithoutBuild)
        }
        return ResponseEntity.ok(
            projectDao.findAll().map(projectPojoToProjectConverter::convert).map { ProjectAndLastBuild(project = it) }
        )
    }

    private fun determineBuildStatus(events: List<BuildEvent>) =
        when {
            events.any { event -> event.status == "FAILED" } -> Status.FAILED
            events.none { event -> event.name == "COMPLETED" } -> Status.RUNNING
            events.any { event -> event.name == "COMPLETED" && event.status == "SUCCEEDED" } -> Status.SUCCEEDED
            else -> Status.UNKNOWN
        }

    override fun createProject(createProjectReq: CreateProjectReq): ResponseEntity<Project> {
        val user = ensure.foundOne(
            operation = { userDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId.id) },
            code = USER_NOT_FOUND,
            message = "User not found"
        )
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val projectPojo = ProjectPojo(
            name = createProjectReq.name,
            organization = organization.name,
            type = ProjectType.valueOf(createProjectReq.type.name),
            version = createProjectReq.version,
            passCheckRuleEnabled = createProjectReq.passCheckRuleEnabled,
            user = user.id,
        )
        if (gitHubService.getRepositoryForProject(project = projectPojo, organization = organization) == null) {
            projectDao.insert(projectPojo)
            gitHubService.createRepoForProject(project = projectPojo, organization = organization)
            awsService.registerProject(project = projectPojo, organization = organization)
            return ResponseEntity.status(CREATED).body(projectPojoToProjectConverter.convert(projectPojo))
        }
        logger.error { "Cannot create project [${projectPojo.name}] for organization [${organization.name}]. The repo already exists in GitHub" }
        return ResponseEntity.status(CONFLICT).build()
    }

    override fun ci(projectName: String, ciReq: CIReq): ResponseEntity<CI> {
        val (user, organization, project) = checkRepoBelongsToOrg(projectName)

        val startCodeBuildResponse = codeBuildService.startCodeBuildProject(
            gitHubAppToken = gitHubService.appToken(organization),
            user = user,
            project = project,
            organization = organization,
            commit = ciReq.commitId,
            codeBuildType = CodeBuildType.CI
        )

        return ResponseEntity.ok(CI(id = startCodeBuildResponse.second.id))
    }

    override fun cd(projectName: String, cdReq: CDReq): ResponseEntity<CD> {
        val (user, organization, project) = checkRepoBelongsToOrg(projectName)

        val startCodeBuildResponse = codeBuildService.startCodeBuildProject(
            gitHubAppToken = gitHubService.appToken(organization),
            user = user,
            project = project,
            organization = organization,
            commit = cdReq.commitId,
            codeBuildType = CodeBuildType.CD,
            environment = cdReq.env,
        )

        return ResponseEntity.ok(CD(id = startCodeBuildResponse.second.id))
    }

    /**
     * Retrieve logs from CloudWatch given a [Project] an [BuildEvent] id and
     * an operation id ([CodeBuildType])
     */
    override fun logs(projectName: String, logsReq: LogsReq): ResponseEntity<CILogs> {
        val (user, organization, project) = checkRepoBelongsToOrg(projectName)
        val buildType = CodeBuildType.valueOf(logsReq.buildType.name)
        val buildEvent = ensure.foundOne(
            operation = { buildEventDao.fetchById(logsReq.buildId).first() },
            code = ExceptionCode.OPERATION_NOT_FOUND,
            lazyMessage = { "Operation [$buildType] with id [${logsReq.buildId}] was not found" }
        )

        val logs = codeBuildService.getLogsFor(buildType, buildEvent, project, organization)
        return ResponseEntity.ok(CILogs(events = logs))
    }

    private fun checkRepoBelongsToOrg(projectName: String): Triple<XtagesUser, Organization, ProjectPojo> {
        val user = ensure.foundOne(
            operation = { userDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId.id) },
            code = USER_NOT_FOUND,
            message = "User not found"
        )
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val project = projectDao.fetchOneByNameAndOrganization(organization.name!!, projectName)
        return Triple(user, organization, project)
    }
}
