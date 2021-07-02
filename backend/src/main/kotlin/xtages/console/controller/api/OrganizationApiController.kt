package xtages.console.controller.api

import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.controller.api.model.*
import xtages.console.controller.model.MD5
import xtages.console.controller.model.organizationPojoToOrganizationConverter
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.service.UserService
import xtages.console.service.aws.CognitoService
import java.util.*
import xtages.console.query.tables.pojos.Organization as OrganizationPojo

@Controller
class OrganizationApiController(
    val organizationDao: OrganizationDao,
    val userService: UserService,
    val cognitoService: CognitoService,
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
        return super.projectsDeployed()
    }
}
