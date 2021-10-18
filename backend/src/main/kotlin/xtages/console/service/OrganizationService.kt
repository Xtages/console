package xtages.console.service

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.*
import xtages.console.config.ConsoleProperties
import xtages.console.controller.model.MD5
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.enums.GithubAppInstallationStatus
import xtages.console.query.enums.GithubOrganizationType
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
    private val ssmAsyncClient: SsmAsyncClient,
    private val consoleProperties: ConsoleProperties,
) {
    /** Creates an [Organization], with [ownerCognitoUserId] as owner. */
    fun create(
        organizationName: String,
        ownerCognitoUserId: String,
        stripeCustomerId: String?,
        subscriptionStatus: OrganizationSubscriptionStatus?,
        githubAppInstallationId: Long?,
        githubAppInstallationStatus: GithubAppInstallationStatus?,
        githubOrganizationType: GithubOrganizationType?,
        githubOauthAuthorized: Boolean,
    ): Organization {
        cognitoService.createGroup(groupName = organizationName)
        val organization = Organization(
            name = organizationName,
            hash = MD5.md5(UUID.randomUUID().toString()),
            stripeCustomerId = stripeCustomerId,
            subscriptionStatus = subscriptionStatus,
            githubAppInstallationId = githubAppInstallationId,
            githubAppInstallationStatus = githubAppInstallationStatus,
            githubOrganizationType = githubOrganizationType,
            githubOauthAuthorized =
            if (githubOrganizationType == GithubOrganizationType.ORGANIZATION) null else githubOauthAuthorized
        )
        organizationDao.insert(organization)
        userService.registerUserFromCognito(
            cognitoUserId = ownerCognitoUserId,
            organization = organization,
            isOwner = true
        )
        return organization
    }

    fun storeSsmParameter(organization: Organization, name: String, value: String): String {
        val fqParamName = buildParamFqn(organization = organization, name = name)
        val putParameterRequest = PutParameterRequest.builder()
            .name(fqParamName)
            .type(ParameterType.SECURE_STRING)
            .keyId("alias/aws/ssm")
            .value(value)
            .overwrite(true)
            .build()
        ssmAsyncClient.putParameter(putParameterRequest).get()

        // Amazon doesn't allow setting `overwrite = true` in the `PutParameter` request above and adding tags at the
        // same time. So we have to create the parameter first and then add the tags using `AddTagsToResource`.
        val addTagsToResourceRequest = AddTagsToResourceRequest.builder()
            .resourceId(fqParamName)
            .resourceType(ResourceTypeForTagging.PARAMETER)
            .tags(
                buildSsmTag("organization", organization.name!!),
                buildSsmTag("organization-hash", organization.hash!!)
            )
            .build()
        ssmAsyncClient.addTagsToResource(addTagsToResourceRequest).get()
        return fqParamName
    }

    fun getSsmParameter(organization: Organization, name: String): String {
        val fqParamName = buildParamFqn(organization = organization, name = name)
        return ssmAsyncClient.getParameter(
            GetParameterRequest.builder()
                .name(fqParamName)
                .withDecryption(true)
                .build()
        ).get().parameter().value()
    }

    private fun buildParamFqn(
        organization: Organization,
        name: String
    ): String {
        ensure.isTrue(value = name.startsWith('/'), code = ExceptionCode.INVALID_SSM_PARAMETER_NAME)
        val orgConfigPrefix = "${consoleProperties.aws.ssm.orgConfigPrefix}${organization.hash!!}"
        if (name.startsWith(orgConfigPrefix)) {
            ensure.isTrue(value = name.length > orgConfigPrefix.length, code = ExceptionCode.INVALID_SSM_PARAMETER_NAME)
            return name
        }
        return "$orgConfigPrefix$name"
    }
}

private fun buildSsmTag(key: String, value: String) = Tag.builder().key(key).value(value).build()
