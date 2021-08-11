package xtages.console.controller.api

import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.config.ConsoleProperties
import xtages.console.controller.api.model.*
import xtages.console.controller.model.MD5
import xtages.console.controller.model.buildPojoToDeployment
import xtages.console.controller.model.organizationPojoToOrganizationConverter
import xtages.console.controller.model.projectPojoTypeToProjectTypeConverter
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.dao.findFromBuilds
import xtages.console.dao.findLatestCdBuilds
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.daos.*
import xtages.console.service.AuthenticationService
import xtages.console.service.UserService
import xtages.console.service.aws.CognitoService
import java.util.*
import xtages.console.query.tables.pojos.Organization as OrganizationPojo

@Controller
class OrganizationApiController(
    val organizationDao: OrganizationDao,
    val userService: UserService,
    val cognitoService: CognitoService,
    val authenticationService: AuthenticationService,
    val recipeDao: RecipeDao,
    val buildDao: BuildDao,
    val projectDao: ProjectDao,
    val githubUserDao: GithubUserDao,
    val consoleProperties: ConsoleProperties,
) :
    OrganizationApiControllerBase {

    override fun createOrganization(createOrgReq: CreateOrgReq): ResponseEntity<Organization> {
        cognitoService.createGroup(groupName = createOrgReq.organizationName)
        val organization = OrganizationPojo(
            name = createOrgReq.organizationName,
            subscriptionStatus = OrganizationSubscriptionStatus.UNCONFIRMED,
            hash = MD5.md5(UUID.randomUUID().toString())
        )
        organizationDao.insert(organization)
        userService.registerUserFromCognito(
            cognitoUserId = createOrgReq.ownerCognitoUserId,
            organization = organization,
            isOwner = true
        )
        return ResponseEntity.status(CREATED)
            .body(organizationPojoToOrganizationConverter.convert(organization))
    }

    override fun getOrganization(): ResponseEntity<Organization> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        return ResponseEntity.ok(organizationPojoToOrganizationConverter.convert(organization))
    }

    override fun projectsDeployed(): ResponseEntity<Projects> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val latestCdBuilds = buildDao.findLatestCdBuilds(organizationName = organization.name!!)
        val buildsPerProjectId = latestCdBuilds.groupBy { build -> build.projectId }
        val projectPojos = projectDao.fetchById(*buildsPerProjectId.keys.filterNotNull().toIntArray())
        val recipes = recipeDao.fetchById(*projectPojos.mapNotNull { project -> project.recipe }.toIntArray())
        val recipesById = recipes.associateBy { recipe -> recipe.id }
        val usernameToGithubUser = githubUserDao.findFromBuilds(latestCdBuilds)
        val idToXtagesUser = userService.findFromBuilds(latestCdBuilds)

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
                deployments = buildsPerProjectId[project.id]?.map { build ->
                    buildPojoToDeployment(
                        source = build,
                        organization = organization,
                        project = project,
                        usernameToGithubUser = usernameToGithubUser,
                        idToXtagesUser = idToXtagesUser,
                        domain = consoleProperties.domain
                    )
                } ?: emptyList(),
                percentageOfSuccessfulBuildsInTheLastMonth = null
            )
        }

        return ResponseEntity.ok(Projects(projects))
    }
}

