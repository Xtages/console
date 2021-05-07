package xtages.console.controller.api

import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.controller.api.model.CreateOrgReq
import xtages.console.controller.api.model.Organization
import xtages.console.controller.model.organizationPojoToOrganizationConverter
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.service.UserService

@Controller
class OrganizationApiController(
    val organizationDao: OrganizationDao,
    val userService: UserService,
) :
    OrganizationApiControllerBase {

    override fun createOrganization(createOrgReq: CreateOrgReq): ResponseEntity<Organization> {
        val organization = xtages.console.query.tables.pojos.Organization(
            name = createOrgReq.organizationName,
            subscriptionStatus = OrganizationSubscriptionStatus.UNCONFIRMED,
        )
        organizationDao.insert(organization)
        userService.createUser(
            cognitoUserId = createOrgReq.ownerCognitoUserId,
            organizationName = createOrgReq.organizationName,
            isOwner = true
        )
        return ResponseEntity.status(CREATED)
            .body(organizationPojoToOrganizationConverter.convert(organization))
    }


}
