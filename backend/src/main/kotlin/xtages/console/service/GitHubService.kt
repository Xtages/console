package xtages.console.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import mu.KotlinLogging
import org.kohsuke.github.*
import org.springframework.context.annotation.Lazy
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import xtages.console.config.ConsoleProperties
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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.reflect.KProperty

private val logger = KotlinLogging.logger { }

@Service
class GitHubService(
    private val consoleProperties: ConsoleProperties,
    private val objectMapper: ObjectMapper,
    private val webClientBuilder: WebClient.Builder,
    private val organizationDao: OrganizationDao,
    private val projectDao: ProjectDao,
    private val githubUserDao: GithubUserDao,
    private val authenticationService: AuthenticationService,
    private val userService: UserService,
    @param:Lazy private val codeBuildService: CodeBuildService,
    private val organizationService: OrganizationService,
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
        val push = GitHub.offline().parseEventPayload(eventJson.reader(), GHEventPayload.Push::class.java)
        logger.debug { "Handling GitHub Push event for commit [${push.head}]" }
        if (push.ref.endsWith("/main") || push.ref.endsWith("/master")) {
            if (push.commits.isNotEmpty()) {
                val organization = ensure.foundOne(
                    operation = { organizationDao.fetchOneByGithubAppInstallationId(push.installation.id) },
                    code = ORG_NOT_FOUND
                )
                val appToken = appToken(organization)
                val project = projectDao.fetchOneByNameAndOrganization(
                    organization = organization,
                    projectName = push.repository.name
                )
                val recipe = ensure.foundOne(
                    operation = { recipeDao.fetchOneById(project.recipe!!) },
                    code = RECIPE_NOT_FOUND
                )

                val committer = push.commits.single { commit -> commit.sha == push.head }.author
                val committerXtagesUser = userService.findUserByEmail(committer.email)
                val githubUser = saveGitHubUser(push.sender)

                codeBuildService.startCodeBuildProject(
                    gitHubAppToken = appToken,
                    user = committerXtagesUser?.user,
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
        val installationId = installation.get("id").asLong()
        val organization = ensure.foundOne(
            operation = { organizationDao.fetchOneByGithubAppInstallationId(installationId) },
            code = ORG_NOT_FOUND,
            message = "Organization with installationId [$installationId] is not registered on Xtages"
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
     * Exchanges a temporary [code] from GitHub for an [AccessTokenResponse]. The [state] is validated against the
     * currently logged in cognito user id.
     * If [codeFromOauthApp] is `true` then the `clientId` and `clientSecret` for the GitHub OAuth app are used,
     * otherwise the `clientId` and `clientSecret` for the GitHub App are used.
     */
    fun exchangeTempCodeForAuthToken(
        code: String,
        state: String,
        codeFromOauthApp: Boolean = false,
    ): AccessTokenResponse {
        ensure.isTrue(
            value = state == authenticationService.currentCognitoUserId.id,
            code = INVALID_GITHUB_APP_INSTALL_STATE
        )
        val clientId =
            if (codeFromOauthApp) consoleProperties.gitHubOauth.clientId else consoleProperties.gitHubApp.clientId
        val clientSecret =
            if (codeFromOauthApp) consoleProperties.gitHubOauth.clientSecret
            else consoleProperties.gitHubApp.clientSecret
        return webClientBuilder.baseUrl("https://github.com")
            .build()
            .post()
            .uri { uri ->
                uri.path("login/oauth/access_token")
                    .queryParam("client_id", clientId)
                    .queryParam("client_secret", clientSecret)
                    .queryParam("code", code)
                    .queryParam("state", state)
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(AccessTokenResponse::class.java)
            .block()!!
            .body!!
    }

    /**
     * Returns a [GHRepository] for the [project] in [organization].
     */
    fun getRepositoryForProject(project: Project, organization: Organization): GHRepository? {
        val installation = fetchAppInstallation(organization)
        return try {
            val gitHubAppClient = buildGitHubAppClient(organization)
            gitHubAppClient.getRepository("${installation.account.login}/${project.name}")
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
        val installation = fetchAppInstallation(organization = organization)
        val gitHubAppClient = if (installation.targetType == GHTargetType.ORGANIZATION) {
            buildGitHubAppClient(organization = organization)
        } else {
            buildGitHubClientWithAuthToken(userLogin = installation.account.login, organization = organization)
        }
        val repository = gitHubAppClient
            .createRepository(project.name)
            .owner(installation.account.login)
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
    fun tagProject(organization: Organization, project: Project, userName: String, commitHash: String): String {
        val datePattern = "yyyyMddHm"
        val gitHubAppClient = buildGitHubAppClient(organization)

        val repository = gitHubAppClient.getRepository(project.ghRepoFullName)
        val shA1Short = commitHash.substring(0, 6)
        val now = Instant.now().toUtcLocalDateTime()
        val tag = "v${now.format(DateTimeFormatter.ofPattern(datePattern))}-$shA1Short"
        val message = "Xtages automated tag for release triggered by CD operation from $userName"
        val tagSha = repository.createTag(tag, message, commitHash, "commit").sha
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

    fun fetchAppInstallation(organization: Organization): GHAppInstallation {
        val githubAppInstallationId = ensure.notNull(
            value = organization.githubAppInstallationId,
            valueDesc = "organization.githubAppInstallationId"
        )
        return gitHubClient.app.getInstallationById(githubAppInstallationId)!!
    }

    @Suppress("DEPRECATION")
    private fun buildGitHubAppClient(organization: Organization): GitHub {
        return GitHubBuilder().withAppInstallationToken(
            fetchAppInstallation(organization = organization)
                .createToken()
                .permissions(mapOf("administration" to GHPermissionType.WRITE, "contents" to GHPermissionType.WRITE))
                .create()
                .token
        ).build()!!
    }

    private fun buildGitHubClientWithAuthToken(userLogin: String, organization: Organization): GitHub {
        val githubUser = ensure.notNull(
            value = githubUserDao.fetchOneByUsername(value = userLogin),
            valueDesc = "gitHubUser $userLogin"
        )
        val oauthTokenSsmParam = ensure.notNull(
            value = githubUser.oauthTokenSsmParam,
            valueDesc = "githubUser.oauthTokenSsmParam"
        )
        val oauthTokenExpirationDate = ensure.notNull(
            value = githubUser.oauthTokenExpirationDate,
            valueDesc = "githubUser.oauthTokenExpirationDate"
        )
        ensure.isTrue(
            oauthTokenExpirationDate.isAfter(LocalDateTime.now(ZoneOffset.UTC)),
            code = GITHUB_OAUTH_TOKEN_EXPIRED
        )
        val oAuthToken =
            organizationService.getSsmParameter(organization = organization, name = oauthTokenSsmParam)
        return GitHub.connectUsingOAuth(oAuthToken)!!
    }

    /**
     * Saves a [GithubUser] to the database. If [response] is specified then the tokens are saved in SSM.
     * Note that if [response] is not null, then [organization] must not be null either.
     */
    fun saveGitHubUser(
        ghUser: GHUser,
        organization: Organization? = null,
        response: AccessTokenResponse? = null,
    ): GithubUser {
        val githubUser = githubUserDao.fetchOneByUsername(ghUser.login) ?: GithubUser(
            githubId = ghUser.id,
            username = ghUser.login,
            email = ghUser.email,
            name = if (ghUser.type == "Bot") ghUser.login else ghUser.name,
            avatarUrl = ghUser.avatarUrl,
        )
        if (response != null) {
            val org = ensure.notNull(organization, "organization")
            val cognitoUserId = ensure.notNull(authenticationService.currentCognitoUserId, "cognitoUserId")
            // If `response.refreshToken` is not `null` then we are dealing with temporary tokens that are issued to
            // GitHub Apps. See https://docs.github.com/en/developers/apps/building-github-apps/identifying-and-authorizing-users-for-github-apps#identifying-users-on-your-site
            if (response.refreshToken != null) {
                val authTokenParamName = organizationService.storeSsmParameter(
                    organization = org,
                    name = "/${cognitoUserId.id}/gh/auth_token",
                    value = response.accessToken
                )
                githubUser.authorizationTokenSsmParam = authTokenParamName
                if (response.expiresIn != null) {
                    githubUser.authorizationTokenExpirationDate =
                        LocalDateTime.now(ZoneOffset.UTC).plusSeconds(response.expiresIn)
                            .minusMinutes(10)
                }
                val refreshTokenParamName = organizationService.storeSsmParameter(
                    organization = org,
                    name = "/${cognitoUserId.id}/gh/refresh_token",
                    value = response.refreshToken
                )
                githubUser.refreshTokenSsmParam = refreshTokenParamName
                githubUser.refreshTokenExpirationDate = LocalDateTime
                    .now(ZoneOffset.UTC)
                    .plusSeconds(response.refreshTokenExpiresIn!!)
                    .minusDays(1)
            } else {
                // If `response.refreshToken` is `null` then we have an GitHub OAuth token
                val oauthTokenParamName = organizationService.storeSsmParameter(
                    organization = org,
                    name = "/${cognitoUserId.id}/gh/oauth_token",
                    value = response.accessToken
                )
                githubUser.oauthTokenSsmParam = oauthTokenParamName
                // OAuth tokens expire when it hasn't been used in one year.
                // See https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/token-expiration-and-revocation#token-expired-due-to-lack-of-use
                githubUser.oauthTokenExpirationDate = LocalDateTime.now(ZoneOffset.UTC).plusYears(1).minusDays(2)
            }
        }
        githubUserDao.merge(githubUser)
        return githubUser
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
                .issuer(consoleProperties.gitHubApp.identifier.toString())
                .issueTime(Date())
                .expirationTime(Date.from(expirationTime))
                .build()
            val rsaKey = jwk.toRSAKey()
            val signer = RSASSASigner(rsaKey)
            val signedJwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).build(), claimsSet)
            signedJwt.sign(signer)
            val jwtString = signedJwt.serialize()
            println("jwtString = $jwtString")
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

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class AccessTokenResponse(
    val accessToken: String,
    val expiresIn: Long?,
    val refreshToken: String?,
    val refreshTokenExpiresIn: Long?,
    val scope: String,
    val tokenType: String
)
