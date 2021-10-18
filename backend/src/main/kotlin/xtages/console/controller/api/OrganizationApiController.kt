package xtages.console.controller.api

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.config.ConsoleProperties
import xtages.console.controller.api.model.OrgEligibleReq
import xtages.console.controller.api.model.Organization
import xtages.console.controller.api.model.Project
import xtages.console.controller.api.model.Projects
import xtages.console.controller.model.buildPojoToDeployment
import xtages.console.controller.model.organizationPojoToOrganizationConverter
import xtages.console.controller.model.projectPojoTypeToProjectTypeConverter
import xtages.console.dao.fetchLatestDeploymentsByOrg
import xtages.console.dao.findFromBuilds
import xtages.console.dao.maybeFetchOneByCognitoUserId
import xtages.console.query.tables.daos.*
import xtages.console.service.AuthenticationService
import xtages.console.service.GitHubService
import xtages.console.service.UserService

private val logger = KotlinLogging.logger { }

@Controller
class OrganizationApiController(
    val organizationDao: OrganizationDao,
    val userService: UserService,
    val authenticationService: AuthenticationService,
    val recipeDao: RecipeDao,
    val buildDao: BuildDao,
    val projectDeploymentDao: ProjectDeploymentDao,
    val projectDao: ProjectDao,
    val githubUserDao: GithubUserDao,
    private val gitHubService: GitHubService,
    val consoleProperties: ConsoleProperties,
) :
    OrganizationApiControllerBase {

    override fun getOrganization(): ResponseEntity<Organization> {
        val organization = organizationDao.maybeFetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        organization ?: run {
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
        val installation = gitHubService.fetchAppInstallation(organization)
        println(installation.permissions)
        return ResponseEntity.ok(organizationPojoToOrganizationConverter.convert(organization))
    }

    override fun getOrganizationEligibility(orgEligibleReq: OrgEligibleReq): ResponseEntity<Unit> {
        val organization = organizationDao.fetchOneByName(orgEligibleReq.name)
        organization ?: run {
            ResponseEntity.ok(Unit)
        }
        return ResponseEntity(HttpStatus.CONFLICT)
    }

    override fun projectsDeployed(): ResponseEntity<Projects> {
        val organization = organizationDao.maybeFetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        organization ?: run {
            logger.warn { "No project deployed as the user doesn't have an organization linked" }
            return ResponseEntity.ok(Projects(emptyList()))
        }
        val latestDeployments = projectDeploymentDao.fetchLatestDeploymentsByOrg(organization = organization)
        val deploysPerProjectId = latestDeployments.groupBy { deployment -> deployment.projectId }
        val projectPojos = projectDao.fetchById(*deploysPerProjectId.keys.filterNotNull().toIntArray())
        val recipes = recipeDao.fetchById(*projectPojos.mapNotNull { project -> project.recipe }.toIntArray())
        val recipesById = recipes.associateBy { recipe -> recipe.id }
        val builds =
            buildDao.fetchById(*latestDeployments.mapNotNull { deployment -> deployment.buildId }.toLongArray())
        val buildsById = builds.associateBy { build -> build.id }
        val usernameToGithubUser = githubUserDao.findFromBuilds(builds)
        val idToXtagesUser = userService.findFromBuilds(builds)

        val projects = projectPojos.map { project ->
            val recipe = recipesById[project.recipe]!!
            Project(
                id = project.id!!,
                name = project.name!!,
                organization = project.organization!!,
                ghRepoUrl = project.ghRepoFullName!!,
                passCheckRuleEnabled = project.passCheckRuleEnabled!!,
                type = projectPojoTypeToProjectTypeConverter.convert(
                    recipe.projectType!!
                )!!,
                version = recipe.version!!,
                builds = emptyList(),
                deployments = deploysPerProjectId[project.id]?.map { deployment ->
                    val build = buildsById[deployment.buildId]
                    buildPojoToDeployment(
                        source = build!!,
                        projectDeployment = deployment,
                        organization = organization,
                        project = project,
                        usernameToGithubUser = usernameToGithubUser,
                        idToXtagesUser = idToXtagesUser,
                        customerDeploymentDomain = consoleProperties.customerDeploymentDomain
                    )
                } ?: emptyList(),
                percentageOfSuccessfulBuildsInTheLastMonth = null
            )
        }

        return ResponseEntity.ok(Projects(projects))
    }
}

