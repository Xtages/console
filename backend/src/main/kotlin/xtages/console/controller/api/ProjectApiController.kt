package xtages.console.controller.api

import mu.KotlinLogging
import org.springframework.http.HttpStatus.*
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.controller.api.model.*
import xtages.console.controller.model.projectPojoToProjectConverter
import xtages.console.dao.findByCognitoUserId
import xtages.console.dao.findByNameAndOrganization
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ExceptionCode.ORG_NOT_FOUND
import xtages.console.exception.ExceptionCode.USER_NOT_FOUND
import xtages.console.exception.ensure
import xtages.console.query.enums.ProjectType
import xtages.console.query.tables.daos.BuildEventsDao
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.query.tables.pojos.BuildEvents
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.XtagesUser
import xtages.console.query.tables.references.BUILD_EVENTS
import xtages.console.service.AuthenticationService
import xtages.console.service.AwsService
import xtages.console.service.CodeBuildType
import xtages.console.service.GitHubService
import kotlin.math.log
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
    private val buildEventsDao: BuildEventsDao,
) : ProjectApiControllerBase {

    override fun createProject(createProjectReq: CreateProjectReq): ResponseEntity<Project> {
        val user = ensure.foundOne(
            operation = { userDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId.id) },
            code = USER_NOT_FOUND,
            message = "User not found"
        )
        val organization = ensure.foundOne(
            operation = { organizationDao.findByCognitoUserId(authenticationService.currentCognitoUserId) },
            code = ORG_NOT_FOUND,
            "Could not find organization associated to user"
        )
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

        val buildEvent = createBuildEvent(user, project, CodeBuildType.CI, ciReq.commitId)

        val buildEventsRecord = buildEventsDao.ctx().newRecord(BUILD_EVENTS, buildEvent)
        buildEventsRecord.store()
        logger.debug { "Build Event created with id: ${buildEventsRecord.id}" }

        awsService.startCodeBuildProject(
            project = project, organization = organization,
            commit = ciReq.commitId, codeBuildType = CodeBuildType.CI
        )
        return ResponseEntity.ok(CI(id = buildEventsRecord.id))
    }

    override fun cd(projectName: String, cdReq: CDReq): ResponseEntity<CD> {
        val (user, organization, project) = checkRepoBelongsToOrg(projectName)

        val buildEvent = createBuildEvent(
            user, project, CodeBuildType.CD,
            cdReq.commitId, cdReq.env
        )

        val buildEventsRecord = buildEventsDao.ctx().newRecord(BUILD_EVENTS, buildEvent)
        buildEventsRecord.store()
        logger.debug { "Build Event created with id: ${buildEventsRecord.id}" }

        awsService.startCodeBuildProject(
            project = project, organization = organization,
            commit = cdReq.commitId, codeBuildType = CodeBuildType.CD
        )
        return ResponseEntity.ok(CD(id = buildEventsRecord.id))
    }

    private fun checkRepoBelongsToOrg(projectName: String): Triple<XtagesUser, Organization, xtages.console.query.tables.pojos.Project> {
        val user = ensure.foundOne(
            operation = { userDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId.id) },
            code = USER_NOT_FOUND,
            message = "User not found"
        )
        val organization = ensure.foundOne(
            operation = { organizationDao.findByCognitoUserId(authenticationService.currentCognitoUserId) },
            code = ORG_NOT_FOUND,
            "Could not find organization associated to user"
        )
        val project = ensure.foundOne(
            operation = { projectDao.findByNameAndOrganization(projectName, organization.name!!) },
            code = ExceptionCode.PROJECT_NOT_FOUND,
            message = "Could not find project in organization"
        )
        return Triple(user, organization, project)
    }
}

private fun createBuildEvent(
    user: XtagesUser,
    project: xtages.console.query.tables.pojos.Project,
    type: CodeBuildType,
    commitId: String,
    env: String? = "dev"
): BuildEvents {
    return BuildEvents(
        environment = env,
        operation = type.name,
        status = "starting",
        user = user.id,
        projectId = project.id,
        commit = commitId
    )
}
