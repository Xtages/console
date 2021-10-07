package xtages.console.controller.api

import mu.KotlinLogging
import org.kohsuke.github.GHRepositorySelection
import org.kohsuke.github.GHTargetType
import org.kohsuke.github.GitHub
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.codec.Hex
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.util.UriComponentsBuilder
import xtages.console.config.ConsoleProperties
import xtages.console.controller.api.model.GitHubAppInstallReq
import xtages.console.controller.api.model.GitHubOauthInstallReq
import xtages.console.controller.api.model.Organization
import xtages.console.controller.model.organizationPojoToOrganizationConverter
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.enums.GithubAppInstallationStatus
import xtages.console.query.enums.GithubOrganizationType
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.service.AuthenticationService
import xtages.console.service.GitHubService
import xtages.console.service.GitHubWebhookEventType
import xtages.console.service.OrganizationService
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val GIT_HUB_WEBHOOK_EVENT_HEADER = "X-GitHub-Event"
private const val GIT_HUB_SIGNATURE_HEADER = "X-HUB-Signature"
private const val GIT_HUB_DELIVERY_HEADER = "X-GitHub-Delivery"
private const val HMAC_SHA1_ALGORITHM = "HmacSHA1"

private val logger = KotlinLogging.logger { }

@Controller
@RequestMapping("\${api.base-path:/api/v1}")
class GitHubAppApiController(
    private val consoleProperties: ConsoleProperties,
    private val gitHubService: GitHubService,
    private val organizationService: OrganizationService,
    private val authenticationService: AuthenticationService,
    private val organizationDao: OrganizationDao,
) : GithubApiControllerBase {

    override fun getInstallUrl(): ResponseEntity<String> {
        return ResponseEntity.ok(
            UriComponentsBuilder
                .fromUriString(consoleProperties.gitHubApp.installUrl)
                .queryParam("state", authenticationService.currentCognitoUserId.id)
                .toUriString()
        )
    }

    @Transactional
    override fun recordInstall(gitHubAppInstallReq: GitHubAppInstallReq): ResponseEntity<Organization> {
        val accessTokenResponse = gitHubService.exchangeTempCodeForAuthToken(
            code = gitHubAppInstallReq.code,
            state = gitHubAppInstallReq.state,
        )
        val gitHubUser = GitHub.connectUsingOAuth(accessTokenResponse.accessToken).myself
        val installations = gitHubUser.appInstallations
        val installation = installations.single { installation ->
            installation.id == gitHubAppInstallReq.installationId
                    && installation.appId == consoleProperties.gitHubApp.identifier
        }
        ensure.isTrue(
            value = installation.repositorySelection == GHRepositorySelection.ALL,
            code = ExceptionCode.INVALID_GITHUB_APP_INSTALL_NOT_ALL_REPOS_SELECTED,
        )
        val organizationName = installation.account.login
        ensure.isTrue(
            value = organizationDao.fetchOneByName(organizationName) == null,
            code = ExceptionCode.ORG_ALREADY_EXISTS
        )
        val organization = organizationService.create(
            organizationName = organizationName,
            ownerCognitoUserId = authenticationService.currentCognitoUserId.id,
            stripeCustomerId = null,
            subscriptionStatus = OrganizationSubscriptionStatus.UNCONFIRMED,
            githubAppInstallationId = gitHubAppInstallReq.installationId,
            githubAppInstallationStatus = GithubAppInstallationStatus.ACTIVE,
            githubOrganizationType = if (installation.targetType == GHTargetType.ORGANIZATION)
                GithubOrganizationType.ORGANIZATION else GithubOrganizationType.INDIVIDUAL,
        )
        gitHubService.saveGitHubUser(
            ghUser = gitHubUser,
            organization = organization,
            response = accessTokenResponse
        )
        return ResponseEntity.ok(organizationPojoToOrganizationConverter.convert(organization))
    }

    @Transactional
    override fun recordOauthInstall(gitHubOauthInstallReq: GitHubOauthInstallReq): ResponseEntity<Organization> {
        val accessTokenResponse = gitHubService.exchangeTempCodeForAuthToken(
            code = gitHubOauthInstallReq.code,
            state = gitHubOauthInstallReq.state,
            codeFromOauthApp = true,
        )
        val gitHubUser = GitHub.connectUsingOAuth(accessTokenResponse.accessToken).myself
        val organization =
            organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        gitHubService.saveGitHubUser(
            ghUser = gitHubUser,
            organization = organization,
            response = accessTokenResponse
        )
        return ResponseEntity.ok(organizationPojoToOrganizationConverter.convert(organization))
    }

    @PostMapping("/github/webhook")
    fun webhook(
        @RequestBody body: String,
        @RequestHeader(GIT_HUB_WEBHOOK_EVENT_HEADER) eventTypeHeader: String,
        @RequestHeader(GIT_HUB_SIGNATURE_HEADER) gitHubSignatureHeader: String,
        @RequestHeader(GIT_HUB_DELIVERY_HEADER) gitHubDeliveryHeader: String,
    ): ResponseEntity<String> {
        logger.trace { "Received GitHub webhook request." }
        if (!verifyRequest(body = body, gitHubSignatureHeader = gitHubSignatureHeader)) {
            logger.trace { "GitHub request failed validation." }
            return ResponseEntity.badRequest().body("Failed to validate webhook request.")
        }

        gitHubService.handleWebhookRequest(
            eventType = GitHubWebhookEventType.fromGitHubWebhookEventName(eventTypeHeader),
            eventId = gitHubDeliveryHeader,
            eventJson = body
        )
        return ResponseEntity.ok().build()
    }

    private fun verifyRequest(body: String, gitHubSignatureHeader: String): Boolean {
        val sha1 = gitHubSignatureHeader.split('=')[1]
        val gitHubsDigest = Hex.decode(sha1)
        val secretKeySpec = SecretKeySpec(consoleProperties.gitHubApp.webhookSecret.toByteArray(), HMAC_SHA1_ALGORITHM)
        val hmac = Mac.getInstance(HMAC_SHA1_ALGORITHM)
        hmac.init(secretKeySpec)
        val ourDigest = hmac.doFinal(body.toByteArray())
        return MessageDigest.isEqual(gitHubsDigest, ourDigest)
    }
}

