package xtages.console.service

import org.springframework.stereotype.Service
import xtages.console.controller.model.MD5
import xtages.console.query.enums.GithubAppInstallationStatus
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.service.aws.CognitoService
import java.util.*

@Service
class OrganizationService(
    private val organizationDao: OrganizationDao,
    private val userService: UserService,
    private val cognitoService: CognitoService,
) {
    /** Creates an [Organization], with [ownerCognitoUserId] as owner. */
    fun create(
        organizationName: String,
        ownerCognitoUserId: String,
        stripeCustomerId: String?,
        subscriptionStatus: OrganizationSubscriptionStatus?,
        githubAppInstallationId: Long?,
        githubAppInstallationStatus: GithubAppInstallationStatus?
    ): Organization {
        cognitoService.createGroup(groupName = organizationName)
        val organization = Organization(
            name = organizationName,
            hash = MD5.md5(UUID.randomUUID().toString()),
            stripeCustomerId = stripeCustomerId,
            subscriptionStatus = subscriptionStatus,
            githubAppInstallationId = githubAppInstallationId,
            githubAppInstallationStatus = githubAppInstallationStatus,
        )
        organizationDao.insert(organization)
        userService.registerUserFromCognito(
            cognitoUserId = ownerCognitoUserId,
            organization = organization,
            isOwner = true
        )
        return organization
    }
}
