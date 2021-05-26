package xtages.console.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import mu.KotlinLogging
import org.kohsuke.github.*
import org.springframework.stereotype.Service
import xtages.console.config.ConsoleProperties
import xtages.console.controller.model.CodeBuildType
import xtages.console.dao.fetchOneByNameAndOrganization
import xtages.console.dao.fetchOrganizationsOwner
import xtages.console.exception.ExceptionCode.*
import xtages.console.exception.IllegalArgumentException
import xtages.console.exception.ensure
import xtages.console.pojo.templateRepoName
import xtages.console.query.enums.GithubAppInstallationStatus.ACTIVE
import xtages.console.query.enums.GithubAppInstallationStatus.SUSPENDED
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import xtages.console.service.GitHubWebhookEventType.*
import xtages.console.service.aws.CodeBuildService
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.reflect.KProperty

private val logger = KotlinLogging.logger { }

@Service
class GitHubService(
    consoleProperties: ConsoleProperties,
    private val objectMapper: ObjectMapper,
    private val organizationDao: OrganizationDao,
    private val projectDao: ProjectDao,
    private val userDao: XtagesUserDao,
    private val codeBuildService: CodeBuildService,
) {
    private val gitHubClient by GitHubClientDelegate(consoleProperties)

    fun handleWebhookRequest(eventType: GitHubWebhookEventType?, eventJson: String) {
        when (eventType) {
            INSTALLATION -> onInstallation(eventJson)
            INSTALLATION_REPOSITORIES -> throw IllegalArgumentException(
                code = GH_APP_INSTALLATION_INVALID,
                innerMessage = "GitHub app must be installed at the organization level."
            )
            PUSH -> onPush(eventJson)
        }
    }

    private fun onPush(eventJson: String) {
        val push = gitHubClient.parseEventPayload(eventJson.reader(), GHEventPayload.Push::class.java)
        logger.debug { "Handling GitHub Push event for commit [${push.head}]" }
        if (push.ref.endsWith("/main") || push.ref.endsWith("/master")) {
            val organization = ensure.foundOne(
                operation = { organizationDao.fetchOneByName(push.organization.login) },
                code = ORG_NOT_FOUND
            )
            val appToken = appToken(organization)
            val project = projectDao.fetchOneByNameAndOrganization(
                orgName = push.organization.login,
                projectName = push.repository.name
            )
            // TODO(czuniga): This is a stop-gap measure, because we currently don't have a way to associate a GitHub
            // user to an Xtages user. We should be taking the user information from the Push event itself.
            val owner = userDao.fetchOrganizationsOwner(organization)
            codeBuildService.startCodeBuildProject(
                gitHubAppToken = appToken,
                user = owner,
                project = project,
                organization = organization,
                commitHash = push.head,
                codeBuildType = CodeBuildType.CI,
                fromGitHubApp = true,
            )
        } else {
            logger.debug { "Commit happened on branch [${push.ref}] and therefore we are ignoring it" }
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

    /**
     * Returns a [GHRepository] for the [project] in [organization].
     */
    fun getRepositoryForProject(project: Project, organization: Organization): GHRepository? {
        val githubAppInstallationId = ensure.notNull(
            value = organization.githubAppInstallationId,
            valueDesc = "organization.githubAppInstallationId"
        )
        val gitHubAppClient = buildGitHubAppClient(
            gitHubClient.app.getInstallationById(githubAppInstallationId).createToken().create()
        )
        return try {
            gitHubAppClient.getRepository("${organization.name}/${project.name}")
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Creates a new GitHub repository for [project]. It will use a template repository as a starting point, the
     * template repository will be selected based on [Project.type] and [Project.version].
     */
    fun createRepoForProject(project: Project, organization: Organization) {
        val githubAppInstallationId = ensure.notNull(
            value = organization.githubAppInstallationId,
            valueDesc = "organization.githubAppInstallationId"
        )
        val gitHubAppClient = buildGitHubAppClient(
            gitHubClient.app.getInstallationById(githubAppInstallationId).createToken().create()
        )
        val repository = gitHubAppClient
            .createRepository(project.name)
            .owner(organization.name)
            .private_(true)
            .fromTemplateRepository("Xtages", project.templateRepoName)
            .create()
        project.ghRepoFullName = repository.fullName
        projectDao.merge(project)
    }

    /**
     * Returns the app token assigned to the GH app for that [Organization]
     * Note: this is not a JWT, however allows the app to use the token to authenticate itself against GH
     */
    fun appToken(organization: Organization): String {
        val githubAppInstallationId = ensure.notNull(
            value = organization.githubAppInstallationId,
            valueDesc = "organization.githubAppInstallationId"
        )
        return ensure.notNull(
            gitHubClient.app.getInstallationById(githubAppInstallationId)
                .createToken().create().token,
            "gitHubClient.token",
            "GitHub App Token"
        )
    }

    private fun buildGitHubAppClient(installationToken: GHAppInstallationToken): GitHub {
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
    PUSH;

    companion object {
        fun fromGitHubWebhookEventName(stripeEvent: String) =
            values().find { it.name.equals(stripeEvent, ignoreCase = true) }
    }
}
