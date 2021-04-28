package xtages.console.controller.api

import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.controller.api.model.CreateProjectReq
import xtages.console.controller.api.model.Project
import xtages.console.controller.model.projectPojoToProjectConverter
import xtages.console.dao.findByCognitoUserId
import xtages.console.exception.ExceptionCode.ORG_NOT_FOUND
import xtages.console.exception.ExceptionCode.USER_NOT_FOUND
import xtages.console.exception.ensure
import xtages.console.query.enums.ProjectType
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.service.AuthenticationService
import xtages.console.service.AwsService
import xtages.console.service.GitHubService
import xtages.console.query.tables.pojos.Project as ProjectPojo

@Controller
class ProjectApiController(
    private val userDao: XtagesUserDao,
    private val organizationDao: OrganizationDao,
    private val projectDao: ProjectDao,
    private val authenticationService: AuthenticationService,
    private val gitHubService: GitHubService,
    private val awsService: AwsService,
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
        projectDao.insert(projectPojo)
        gitHubService.createRepoForProject(project = projectPojo, organization = organization)
        awsService.registerProject(project = projectPojo, organization = organization)
        return ResponseEntity.status(CREATED).body(projectPojoToProjectConverter.convert(projectPojo))
    }
}
