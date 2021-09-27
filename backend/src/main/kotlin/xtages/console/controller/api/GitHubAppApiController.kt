package xtages.console.controller.api

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.kohsuke.github.GHRepositorySelection
import org.kohsuke.github.GHTargetType
import org.kohsuke.github.GitHub
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.codec.Hex
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import xtages.console.config.ConsoleProperties
import xtages.console.controller.api.model.GitHubInstallReq
import xtages.console.controller.api.model.Organization
import xtages.console.controller.model.organizationPojoToOrganizationConverter
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.enums.GithubAppInstallationStatus
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
    private val objectMapper: ObjectMapper,
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

    override fun recordInstall(gitHubInstallReq: GitHubInstallReq): ResponseEntity<Organization> {
        ensure.isTrue(
            value = gitHubInstallReq.state == authenticationService.currentCognitoUserId.id,
            code = ExceptionCode.INVALID_GITHUB_APP_INSTALL_STATE
        )
        val response = WebClient
            .create("https://github.com/login/oauth/access_token")
            .post()
            .uri { uri ->
                uri.queryParam("client_id", consoleProperties.gitHubApp.clientId)
                    .queryParam("client_secret", consoleProperties.gitHubApp.clientSecret)
                    .queryParam("code", gitHubInstallReq.code)
                    .queryParam("state", gitHubInstallReq.state)
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(String::class.java)
            .block()
        val oAuthToken = objectMapper.readTree(response!!.body).get("access_token").asText()
        val installations = GitHub.connectUsingOAuth(oAuthToken).myself.appInstallations
        val installation = installations.single { installation ->
            installation.id == gitHubInstallReq.installationId
                    && installation.appId == consoleProperties.gitHubApp.appId
        }
        ensure.isTrue(
            value = installation.targetType == GHTargetType.ORGANIZATION,
            code = ExceptionCode.INVALID_GITHUB_APP_INSTALL_TARGET
        )
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
            githubAppInstallationId = gitHubInstallReq.installationId,
            githubAppInstallationStatus = GithubAppInstallationStatus.ACTIVE
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

