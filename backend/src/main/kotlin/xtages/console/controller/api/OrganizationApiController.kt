package xtages.console.controller.api

import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import xtages.console.controller.api.model.CreateOrgReq
import xtages.console.controller.api.model.Organization
import xtages.console.controller.model.organizationPojoToOrganizationConverter
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.query.tables.pojos.XtagesUser

@Controller
class OrganizationApiController(
    val userDao: XtagesUserDao,
    val organizationDao: OrganizationDao,
) :
    OrganizationApiControllerBase {

    override fun createOrganization(createOrgReq: CreateOrgReq): ResponseEntity<Organization> {
        val organization = xtages.console.query.tables.pojos.Organization(
            name = createOrgReq.organizationName,
            subscriptionStatus = OrganizationSubscriptionStatus.UNCONFIRMED,
        )
        organizationDao.insert(organization)
        val owner = XtagesUser(
            cognitoUserId = createOrgReq.ownerCognitoUserId,
            organizationName = organization.name,
            isOwner = true
        )
        userDao.insert(owner)
        return ResponseEntity.status(CREATED)
            .body(organizationPojoToOrganizationConverter.convert(organization))
    }


}
