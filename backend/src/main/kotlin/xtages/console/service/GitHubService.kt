package xtages.console.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.kohsuke.github.GHAppInstallationToken
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.springframework.stereotype.Service
import xtages.console.config.ConsoleProperties
import xtages.console.exception.ExceptionCode.*
import xtages.console.exception.IllegalArgumentException
import xtages.console.exception.ensure
import xtages.console.pojo.templateRepoName
import xtages.console.query.enums.GithubAppInstallationStatus.*
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.reflect.KProperty

@Service
class GitHubService(
    consoleProperties: ConsoleProperties,
    val objectMapper: ObjectMapper,
    val organizationDao: OrganizationDao
) {
    private val gitHubClient: GitHub by GitHubClientDelegate(consoleProperties)

    fun handleWebhookRequest(eventType: GitHubWebhookEventType?, eventJson: String) {
        when (eventType) {
            GitHubWebhookEventType.INSTALLATION -> onInstallation(eventJson)
            GitHubWebhookEventType.INSTALLATION_REPOSITORIES -> throw IllegalArgumentException(
                code = GH_APP_INSTALLATION_INVALID,
                innerMessage = "GitHub app must be installed at the organization level."
            )
            GitHubWebhookEventType.REPOSITORY -> TODO()
        }
    }

    private fun onInstallation(eventJson: String) {
        // Ideally we'd use the follow `parseEventPayload` method but for some reason the contents of the
        // repositories present in the event payload don't have an `url` field populate and that makes this
        // method fail.
        // val event = gitHubClient.parseEventPayload(eventJson.reader(), GHEventPayload.Installation::class.java)
        val jsonBody = objectMapper.readTree(eventJson)
        val action = jsonBody.get("action").asText()
        val installation = jsonBody.get("installation")
        val organizationName = installation.get("account").get("login").asText()
        val installationId = installation.get("id").asLong()
        val organization = ensure.foundOne(
            operation = { organizationDao.fetchOneByName(organizationName) },
            code = ORG_NOT_FOUND,
            message = "Organization [$organizationName] is not registered on Xtages"
        )
        /*
        See https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads#installation
        created - Someone installs a GitHub App.
        deleted - Someone uninstalls a GitHub App
        suspend - Someone suspends a GitHub App installation.
        unsuspend - Someone unsuspends a GitHub App installation.
        new_permissions_accepted - Someone accepts new permissions for a GitHub App installation.
            When a GitHub App owner requests new permissions, the person who installed the GitHub App must accept
            the new permissions request.
        */
        when (action) {
            "created" -> {
                val repositorySelection = installation.get("repository_selection").asText()
                ensure.isTrue(
                    value = repositorySelection == "all",
                    code = GH_APP_NOT_ALL_REPOSITORIES_SELECTED,
                    message = "GitHub app was not installed on all repositories"
                )
                organizationDao.update(
                    organization.copy(
                        githubAppInstallationId = installationId,
                        githubAppInstallationStatus = ACTIVE
                    )
                )
            }
            "deleted" -> {
                organizationDao.update(
                    organization.copy(
                        githubAppInstallationId = null,
                        githubAppInstallationStatus = null
                    )
                )
            }
            "suspend" -> {
                ensure.notNull(
                    value = organization.githubAppInstallationId,
                    valueDesc = "organization.githubAppInstallationId"
                )
                organizationDao.update(
                    organization.copy(
                        githubAppInstallationStatus = SUSPENDED
                    )
                )
            }
            "unsuspend", "new_permissions_accepted" -> {
                ensure.notNull(
                    value = organization.githubAppInstallationId,
                    valueDesc = "organization.githubAppInstallationId"
                )
                organizationDao.update(
                    organization.copy(
                        githubAppInstallationStatus = ACTIVE
                    )
                )
            }
        }
    }

    fun createRepoForProject(project: Project, organization: Organization) {
        val githubAppInstallationId = ensure.notNull(
                value = organization.githubAppInstallationId,
                valueDesc = "organization.githubAppInstallationId"
            )
        val gitHubAppClient = buildGitHubAppClient(
            gitHubClient.app.getInstallationById(githubAppInstallationId).createToken().create()
        )
        gitHubAppClient
            .createRepository(project.name)
            .owner(organization.name)
            .private_(true)
            .fromTemplateRepository("Xtages", project.templateRepoName)
            .create()
    }

    fun buildGitHubAppClient(installationToken: GHAppInstallationToken): GitHub {
        return GitHubBuilder().withAppInstallationToken(installationToken.token).build()!!
    }

    private class GitHubClientDelegate(val consoleProperties: ConsoleProperties) {
        lateinit var client: GitHub
        lateinit var expirationTime: Instant
        private val jwk: JWK = JWK.parseFromPEMEncodedObjects(consoleProperties.gitHubApp.privateKey)

        // `true` if the client hasn't been instantiated or the expirationTime claim inside the JWT has is about to
        // expire (1 minute).
        val needsNewClient: Boolean
            get() = !this::client.isInitialized || !this::expirationTime.isInitialized || expirationTime.until(
                Instant.now(),
                ChronoUnit.SECONDS
            ) >= 60

        operator fun getValue(thisRef: GitHubService, property: KProperty<*>): GitHub {
            if (needsNewClient) {
                client = buildGitHubClient()
            }
            return client
        }

        private fun buildGitHubClient(): GitHub {
            expirationTime = Date().toInstant().plus(10, ChronoUnit.MINUTES)
            val claimsSet = JWTClaimsSet.Builder()
                .issuer(consoleProperties.gitHubApp.identifier)
                .issueTime(Date())
                .expirationTime(Date.from(expirationTime))
                .build()
            val rsaKey = jwk.toRSAKey()
            val signer = RSASSASigner(rsaKey)
            val signedJwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).build(), claimsSet)
            signedJwt.sign(signer)
            val jwtString = signedJwt.serialize()
            return GitHubBuilder().withJwtToken(jwtString).build()!!
        }
    }
}

enum class GitHubWebhookEventType {
    INSTALLATION,
    INSTALLATION_REPOSITORIES,
    REPOSITORY;

    companion object {
        fun fromGitHubWebhookEventName(stripeEvent: String) =
            values().find { it.name.equals(stripeEvent, ignoreCase = true) }
    }
}
