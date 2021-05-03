package xtages.console.controller.api

import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.controller.api.model.RunCI
import xtages.console.controller.api.model.RunCIReq
import xtages.console.dao.findByCognitoUserId
import xtages.console.dao.findByNameAndOrganization
import xtages.console.exception.ExceptionCode.*
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.BuildEventsDao
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.query.tables.pojos.BuildEvents
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.pojos.XtagesUser
import xtages.console.query.tables.references.BUILD_EVENTS
import xtages.console.service.AuthenticationService
import xtages.console.service.AwsService
import xtages.console.service.CodeBuildStarterRequest
import xtages.console.service.CodeBuildType

private val logger = KotlinLogging.logger { }

@Controller
class RunCIApiController(
    private val userDao: XtagesUserDao,
    private val organizationDao: OrganizationDao,
    private val projectDao: ProjectDao,
    private val authenticationService: AuthenticationService,
    private val buildEventsDao: BuildEventsDao,
    private val awsService: AwsService,
) : RunciApiControllerBase {

    override fun runCI(runCIReq: RunCIReq): ResponseEntity<RunCI> {
        val (user, organization, project) = checkRepoBelongsToOrg(runCIReq)

        val buildEventsPojo = BuildEvents(
            environment = "DEV",
            operation = "CI",
            status = "starting",
            user = user.id,
            projectId = project.id,
            commit = runCIReq.commitId
        )

        val buildEventsRecord = buildEventsDao.ctx().newRecord(BUILD_EVENTS, buildEventsPojo)
        buildEventsRecord.store();
        logger.debug { "Build Event created with id: ${buildEventsRecord.id}" }

        val codeBuildStarter = CodeBuildStarterRequest(project = project, organization = organization,
            commit = runCIReq.commitId, codeBuildType = CodeBuildType.CI)
        awsService.startCodeBuildProject(codeBuildStarter)
        return ResponseEntity.ok(RunCI(id = buildEventsRecord.id))
    }

    private fun checkRepoBelongsToOrg(runCIReq: RunCIReq): Triple<XtagesUser, Organization, Project> {
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
            operation = { projectDao.findByNameAndOrganization(runCIReq.repo, organization.name!!) },
            code = PROJECT_NOT_FOUND,
            message = "Could not find project in organization"
        )
        return Triple(user, organization, project)
    }

}

