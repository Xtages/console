package xtages.console.controller.api

import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.controller.api.model.*
import xtages.console.controller.model.MD5
import xtages.console.controller.model.organizationPojoToOrganizationConverter
import xtages.console.dao.fetchAllProjectsDeployedIn
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.RecipeDao
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

    override fun projectsDeployed(): ResponseEntity<Projects> {
        val organization = ensure.foundOne(
            operation = {organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)},
            code = ExceptionCode.ORG_NOT_FOUND,
            lazyMessage = {"Organization not found"}
        )
        val projects = organizationDao.fetchAllProjectsDeployedIn(
            organization = organization
        )

        // Here I need to map the project to a model Project
//        projects.map {
//            val recipe = recipeDao.fetchById(it.recipe!!).single()
//            Project(
//                id = it.id!!,
//                name = it.name!!,
//                organization = it.organization!!,
//                ghRepoUrl = it.ghRepoFullName!!,
//                passCheckRuleEnabled = it.passCheckRuleEnabled!!,
//                type = projectPojoTypeToProjectTypeConverter.convert(
//                    recipe.projectType!!
//                )!!,
//                version = recipe.version!!,
//                builds = null,
//                deployments = "",
//                percentageOfSuccessfulBuildsInTheLastMonth = null
//            )
//        }

        return ResponseEntity.ok(Projects(listOf()))
    }
}

