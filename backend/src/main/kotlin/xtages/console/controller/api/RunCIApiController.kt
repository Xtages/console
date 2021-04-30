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
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.pojos.XtagesUser
import xtages.console.query.tables.references.BUILD_EVENTS
import xtages.console.service.*
import java.time.LocalDateTime
import xtages.console.query.tables.pojos.BuildEvents as BuildEventsPojo

private val logger = KotlinLogging.logger { }

@Controller
class RunCIApiController(
    private val userDao: XtagesUserDao,
    private val organizationDao: OrganizationDao,
    private val projectDao: ProjectDao,
    private val authenticationService: AuthenticationService,
    private val buildEventsDao: BuildEventsDao,
    private val awsService: AwsService,
    private val gitHubService: GitHubService
) : RunciApiControllerBase {

    override fun runCI(runCIReq: RunCIReq): ResponseEntity<RunCI> {

//        val (user, project) = checkRepoBelongsToOrg(runCIReq)

//        val buildEventsPojo = BuildEventsPojo(
//            environment = "DEV",
//            operation = "CI",
//            time = LocalDateTime.now(),
//            status = "starting",
//            user = user.id,
//            projectId = project.id,
//            commit = runCIReq.commitId
//        )
//
//        val buildEventsRecord = buildEventsDao.ctx().newRecord(BUILD_EVENTS, buildEventsPojo)
//        buildEventsRecord.store();
//        logger.debug { "Build Event created with id: ${buildEventsRecord.id}" }
//        return ResponseEntity.ok(RunCI(id = buildEventsRecord.id))

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

        logger.info { "token ${gitHubService.token(organization)}" }

        val codeBuildStarter = CodeBuildStarterRequest(project, organization, runCIReq.commitId)
        awsService.startCodeBuildProject(codeBuildStarter)
        return ResponseEntity.ok(RunCI(id = 1))
    }

    private fun checkRepoBelongsToOrg(runCIReq: RunCIReq): Pair<XtagesUser, Project> {
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
        return Pair(user, project)
    }
}

