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
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import xtages.console.config.ConsoleProperties
import xtages.console.controller.GitHubAvatarUrl
import xtages.console.controller.model.CodeBuildType
import xtages.console.dao.fetchOneByNameAndOrganization
import xtages.console.exception.ExceptionCode.*
import xtages.console.exception.ensure
import xtages.console.pojo.templateRepoName
import xtages.console.query.enums.GithubAppInstallationStatus.ACTIVE
import xtages.console.query.enums.GithubAppInstallationStatus.SUSPENDED
import xtages.console.query.tables.daos.GithubUserDao
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.daos.RecipeDao
import xtages.console.query.tables.pojos.GithubUser
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.pojos.Recipe
import xtages.console.service.GitHubWebhookEventType.INSTALLATION
import xtages.console.service.GitHubWebhookEventType.PUSH
import xtages.console.service.aws.CodeBuildService
import xtages.console.time.toUtcLocalDateTime
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeFormatter
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
    private val githubUserDao: GithubUserDao,
    private val userService: UserService,
    @param:Lazy private val codeBuildService: CodeBuildService,
    private val recipeDao: RecipeDao,
) {
    private val gitHubClient by GitHubClientDelegate(consoleProperties)

    fun handleWebhookRequest(eventType: GitHubWebhookEventType?, eventId: String, eventJson: String) {
        when (eventType) {
            INSTALLATION -> onInstallation(eventJson)
            PUSH -> onPush(eventId = eventId, eventJson = eventJson)
        }
    }

    private fun onPush(eventId: String, eventJson: String) {
        val push = gitHubClient.parseEventPayload(eventJson.reader(), GHEventPayload.Push::class.java)
        logger.debug { "Handling GitHub Push event for commit [${push.head}]" }
        if (push.ref.endsWith("/main") || push.ref.endsWith("/master")) {
            if (push.commits.isNotEmpty()) {
                val organization = ensure.foundOne(
                    operation = { organizationDao.fetchOneByName(push.organization.login) },
                    code = ORG_NOT_FOUND
                )
                val appToken = appToken(organization)
                val project = projectDao.fetchOneByNameAndOrganization(
                    orgName = push.organization.login,
                    projectName = push.repository.name
                )
                val recipe = ensure.foundOne(
                    operation = { recipeDao.fetchOneById(project.recipe!!) },
                    code = RECIPE_NOT_FOUND
                )

                val committer = push.commits.single { commit -> commit.sha == push.head }.author
                val pusherXtagesUser = userService.findUserByEmail(committer.email)
                val githubUser = GithubUser(
                    email = committer.email,
                    name = committer.name,
                    username = committer.username,
                    avatarUrl = GitHubAvatarUrl(username = committer.username).toUriString(),
                )
                githubUserDao.merge(githubUser)

                codeBuildService.startCodeBuildProject(
                    gitHubAppToken = appToken,
                    user = pusherXtagesUser?.user,
                    githubUser = githubUser,
                    project = project,
                    recipe = recipe,
                    organization = organization,
                    commitHash = push.head,
                    codeBuildType = CodeBuildType.CI,
                    environment = "dev",
                    fromGitHubApp = true,
                )
            } else {
                logger.debug { "Push with id [$eventId] not handled because it doesn't contain commit" }
            }
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
        val gitHubAppClient = buildGitHubAppClient(organization)
        return try {
            gitHubAppClient.getRepository("${organization.name}/${project.name}")
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Creates a new GitHub repository for [project]. It will use a template repository as a starting point, the
     * template repository will be selected based on the [Recipe] associated to the [Project].
     */
    @Suppress("DEPRECATION")
    fun createRepoForProject(project: Project, recipe: Recipe, organization: Organization, description: String?) {
        val gitHubAppClient = buildGitHubAppClient(organization)
        val repository = gitHubAppClient
            .createRepository(project.name)
            .owner(organization.name)
            .private_(true)
            .description(description ?: "")
            .fromTemplateRepository("Xtages", recipe.templateRepoName)
            .create()
        project.ghRepoFullName = repository.fullName
        projectDao.merge(project)
    }

    /**
     * Returns the app token assigned to the GH app for that [Organization]
     * Note: this is not a JWT, however allows the app to use the token to authenticate itself against GH
     */
    @Suppress("DEPRECATION")
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

    /**
     * Tags a [Project] (repository) in the default branch as default
     * The tag format is the time in UTC yyyyMddHm-(short sha1)
     *
     * Note: this method assumes that in a previous method there is a check to make sure that the [Project]
     * belongs to the [Organization]
     */
    fun tagProject(organization: Organization, project: Project, userName: String): String {
        val datePattern = "yyyyMddHm"
        val gitHubAppClient = buildGitHubAppClient(organization)

        val repository = gitHubAppClient.getRepository(project.ghRepoFullName)
        val defaultBranch = repository.defaultBranch
        val shA1Short = repository.getBranch(defaultBranch).shA1!!.substring(0, 6)
        val now = Instant.now().toUtcLocalDateTime()
        val tag = "v${now.format(DateTimeFormatter.ofPattern(datePattern))}-$shA1Short"
        val message = "Xtages automated tag for release triggered by CD operation from $userName"
        val tagSha = repository.createTag(tag, message, repository.getBranch(defaultBranch).shA1, "commit").sha
        repository.createRef("refs/tags/$tag", tagSha)
        return tag
    }

    /**
     * Finds the GitHub commit for [commitHash].
     */
    fun findCommit(organization: Organization, project: Project, commitHash: String): GHCommit? {
        val gitHubAppClient = buildGitHubAppClient(organization)
        return gitHubAppClient.getRepository(project.ghRepoFullName).getCommit(commitHash)
    }

    /**
     * Finds the revision of the `HEAD` commit for the default branch for [project].
     */
    fun findHeadCommitRevision(organization: Organization, project: Project): String {
        val gitHubAppClient = buildGitHubAppClient(organization)
        val repository = gitHubAppClient.getRepository(project.ghRepoFullName)
        return repository.getBranch(repository.defaultBranch).shA1
    }

    @Suppress("DEPRECATION")
    private fun buildGitHubAppClient(organization: Organization): GitHub {
        val githubAppInstallationId = ensure.notNull(
            value = organization.githubAppInstallationId,
            valueDesc = "organization.githubAppInstallationId"
        )
        return GitHubBuilder().withAppInstallationToken(
            gitHubClient.app.getInstallationById(githubAppInstallationId).createToken().create()
                .token
        ).build()!!
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
    PUSH;

    companion object {
        fun fromGitHubWebhookEventName(stripeEvent: String) =
            values().find { it.name.equals(stripeEvent, ignoreCase = true) }
    }
}
