package xtages.console.controller.api

import mu.KotlinLogging
import org.springframework.http.HttpStatus.*
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import software.amazon.awssdk.services.codebuild.model.Build
import xtages.console.controller.api.model.*
import xtages.console.controller.model.projectPojoToProjectConverter
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.dao.fetchOneByNameAndOrganization
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
import xtages.console.query.tables.records.BuildEventRecord
import xtages.console.query.tables.references.BUILD_EVENT
import xtages.console.service.AuthenticationService
import xtages.console.service.AwsService
import xtages.console.service.CodeBuildType
import xtages.console.service.GitHubService
import java.time.LocalDateTime
import java.time.ZoneOffset
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
    private val buildEventDao: BuildEventDao,
) : ProjectApiControllerBase {

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

        val buildEvent = createSentToBuildEvent(
            user = user,
            project = project,
            type = CodeBuildType.CI,
            commitId = ciReq.commitId,
            status = "STARTED",
            startTime = LocalDateTime.now(ZoneOffset.UTC),
            endTime = LocalDateTime.now(ZoneOffset.UTC),
        )

        val buildEventsRecord = buildEventDao.ctx().newRecord(BUILD_EVENT, buildEvent)
        buildEventsRecord.store()
        logger.debug { "Build Event created with id: ${buildEventsRecord.id}" }

        val startCodeBuildResponse = awsService.startCodeBuildProject(
            project = project, organization = organization,
            commit = ciReq.commitId, codeBuildType = CodeBuildType.CI
        )

        persistSentToBuildOutcome(startCodeBuildResponse.build(), buildEventsRecord, buildEvent)

        return ResponseEntity.ok(CI(id = buildEventsRecord.id))
    }

    override fun cd(projectName: String, cdReq: CDReq): ResponseEntity<CD> {
        val (user, organization, project) = checkRepoBelongsToOrg(projectName)

        val sentToBuildStartedEvent = createSentToBuildEvent(
            user = user,
            project = project,
            type = CodeBuildType.CD,
            commitId = cdReq.commitId,
            status = "STARTED",
            env = cdReq.env,
            startTime = LocalDateTime.now(ZoneOffset.UTC),
            endTime = LocalDateTime.now(ZoneOffset.UTC),
        )

        val sentToBuildStartedEventRecord = buildEventDao.ctx().newRecord(BUILD_EVENT, sentToBuildStartedEvent)
        sentToBuildStartedEventRecord.store()
        logger.debug { "Build Event created with id: ${sentToBuildStartedEventRecord.id}" }

        val startCodeBuildResponse = awsService.startCodeBuildProject(
            project = project, organization = organization,
            commit = cdReq.commitId, codeBuildType = CodeBuildType.CD
        )

        persistSentToBuildOutcome(
            startCodeBuildResponse.build(),
            sentToBuildStartedEventRecord,
            sentToBuildStartedEvent
        )

        return ResponseEntity.ok(CD(id = sentToBuildStartedEventRecord.id))
    }

    override fun logs(projectName: String, ciId: Long): ResponseEntity<CILogs> {
        val (user, organization, project) = checkRepoBelongsToOrg(projectName)

        val buildEvent = buildEventDao.fetchById(ciId).first()

        val logs = awsService.getLogsFrom(CodeBuildType.CI, buildEvent, project, organization)

        return ResponseEntity.ok( )
    }

    private fun persistSentToBuildOutcome(
        build: Build,
        sentToBuildStartedEventRecord: BuildEventRecord,
        sentToBuildStartedEvent: BuildEvent
    ) {
        logger.debug { "StartBuildResponse Object: ${build.arn()}" }
        sentToBuildStartedEventRecord.buildArn = build.arn()
        sentToBuildStartedEventRecord.update()

        val outcomeBuildEvent = sentToBuildStartedEvent.copy(
            id = null,
            status = build.buildStatus().toString(),
            buildArn = build.arn(),
            startTime = LocalDateTime.now(ZoneOffset.UTC),
            endTime = LocalDateTime.now(ZoneOffset.UTC),
        )
        buildEventDao.insert(outcomeBuildEvent)
    }

    private fun checkRepoBelongsToOrg(projectName: String): Triple<XtagesUser, Organization, ProjectPojo> {
        val user = ensure.foundOne(
            operation = { userDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId.id) },
            code = USER_NOT_FOUND,
            message = "User not found"
        )
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val project = projectDao.fetchOneByNameAndOrganization(projectName, organization.name!!)
        return Triple(user, organization, project)
    }
}

private fun createSentToBuildEvent(
    user: XtagesUser,
    project: xtages.console.query.tables.pojos.Project,
    type: CodeBuildType,
    commitId: String,
    status: String,
    env: String? = "dev",
    startTime: LocalDateTime,
    endTime: LocalDateTime,
): BuildEvent {
    return BuildEvent(
        environment = env,
        operation = type.name,
        name = "SENT_TO_BUILD",
        status = status,
        user = user.id,
        projectId = project.id,
        commit = commitId,
        startTime = startTime,
        endTime = endTime,
    )
}
