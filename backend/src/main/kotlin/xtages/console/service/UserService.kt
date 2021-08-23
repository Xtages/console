package xtages.console.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityAsyncClient
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType
import xtages.console.config.ConsoleProperties
import xtages.console.exception.CognitoException
import xtages.console.exception.ExceptionCode.USER_NOT_FOUND
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.GithubUserDao
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.query.tables.pojos.Build
import xtages.console.query.tables.pojos.GithubUser
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.XtagesUser
import xtages.console.query.tables.references.XTAGES_USER
import xtages.console.service.aws.CognitoService
import java.util.concurrent.ExecutionException
import java.util.stream.Stream
import kotlin.streams.toList

private val logger = KotlinLogging.logger { }

@Service
class UserService(
    private val userDao: XtagesUserDao,
    private val githubUserDao: GithubUserDao,
    private val consoleProperties: ConsoleProperties,
    @Qualifier("anonymousCognitoIdentityClient")
    private val anonymousCognitoIdentityClient: CognitoIdentityAsyncClient,
    private val cognitoIdentityProviderAsyncClient: CognitoIdentityProviderAsyncClient,
    private val authenticationService: AuthenticationService,
    private val cognitoService: CognitoService,
) {

    /**
     * List all the users that are part of [organization].
     */
    fun listOrganizationUsers(organization: Organization): List<XtagesUserWithCognitoAttributes> {
        val orgUsers = userDao.fetchByOrganizationName(organization.name!!)
        val usersInGroup = cognitoService.getUsersInGroup(groupName = organization.name!!)
        val cognitoUsersByCognitoId = usersInGroup.associateBy { user -> user.username() }
        val orgUsersByCognitoId = orgUsers.associateBy { user -> user.cognitoUserId!! }
        return orgUsersByCognitoId.map { (cognitoId, user) ->
            val cognitoUser = cognitoUsersByCognitoId[cognitoId]!!
            XtagesUserWithCognitoAttributes(
                user = user,
                attrs = extractCognitoUserAttributes(cognitoUser.attributes()),
                userStatus = cognitoUser.userStatus(),
            )
        }
    }

    /**
     * Creates new Xtages user and looks up it's Cognito Identity and stores it.
     */
    fun registerUserFromCognito(cognitoUserId: String, organization: Organization, isOwner: Boolean = false) {
        val idResponse = anonymousCognitoIdentityClient.getId(
            GetIdRequest.builder()
                .identityPoolId(consoleProperties.aws.cognito.identityPoolId)
                .logins(authenticationService.loginsMap)
                .accountId(consoleProperties.aws.accountId)
                .build()
        ).get()
        cognitoService.addUserToGroup(username = cognitoUserId, groupName = organization.name!!)
        val owner = XtagesUser(
            cognitoUserId = cognitoUserId,
            cognitoIdentityId = idResponse.identityId(),
            organizationName = organization.name!!,
            isOwner = isOwner
        )
        userDao.insert(owner)
    }

    /**
     * Invites an user to be part of [organization]. Creates a new Cognito user, adds it to an organization group and
     * saves the user data to the database.
     */
    fun inviteUser(username: String, name: String, organization: Organization): XtagesUserWithCognitoAttributes {
        val cognitoUser = cognitoService.createCognitoUser(username = username, name = name, organization = organization)
        val user = XtagesUser(
            cognitoUserId = cognitoUser.username(),
            organizationName = organization.name!!,
            isOwner = false
        )
        val userRecord = userDao.ctx().newRecord(XTAGES_USER, user)
        userRecord.store()
        return XtagesUserWithCognitoAttributes(
            user = userRecord.into(XtagesUser::class.java),
            attrs = extractCognitoUserAttributes(cognitoUser.attributes()),
            userStatus = cognitoUser.userStatus(),
        )
    }

    /**
     * Returns an [XtagesUserWithCognitoAttributes] based on their `email` (as registered in Cognito), if there's no
     * user by that `email` then `null` is returned.
     */
    fun findUserByEmail(email: String): XtagesUserWithCognitoAttributes? {
        return try {
            val cognitoUser = cognitoIdentityProviderAsyncClient.adminGetUser(
                AdminGetUserRequest.builder()
                    .userPoolId(consoleProperties.aws.cognito.userPoolId)
                    .username(email)
                    .build()
            ).get()
            val cognitoUserAttributes = extractCognitoUserAttributes(cognitoUser.userAttributes())
            val user = ensure.foundOne(
                operation = { userDao.fetchOneByCognitoUserId(cognitoUser.username()) },
                code = USER_NOT_FOUND,
                message = "Could not find Xtages user [${cognitoUser.username()}]"
            )
            val githubUser = githubUserDao.fetchByEmail(cognitoUserAttributes["email"]!!).singleOrNull()

            XtagesUserWithCognitoAttributes(
                user = user,
                githubUser = githubUser,
                attrs = cognitoUserAttributes,
                userStatus = cognitoUser.userStatus(),
            )
        } catch (e: ExecutionException) {
            if (e.cause is UserNotFoundException) {
                null
            }
            logger.error(e) { }
            throw CognitoException("There was an error while trying to find a user by email: $email")
        }
    }

    /**
     * Finds a list of [XtagesUserWithCognitoAttributes] with additional Cognito attributes (`email`, `name`, etc.)
     * for [XtagesUser]s with [ids].
     */
    fun findCognitoUsersByXtagesUserId(vararg ids: Int): List<XtagesUserWithCognitoAttributes> {
        val uniqueIds = ids.toSet()
        val cognitoUserIdToXtagesUser =
            userDao.fetchById(*uniqueIds.toIntArray()).associateBy { user -> user.cognitoUserId!! }
        val getUserRequests = cognitoUserIdToXtagesUser.keys.map { id ->
            cognitoIdentityProviderAsyncClient.adminGetUser(
                AdminGetUserRequest.builder()
                    .userPoolId(consoleProperties.aws.cognito.userPoolId)
                    .username(id)
                    .build()
            ).handle { response, error ->
                if (error != null) {
                    logger.error(error) { "An error occurred fetching cognito user details for [$id]" }
                    null
                } else {
                    response
                }
            }
        }
        val cognitoIdToCognitoUser = Stream.of(*getUserRequests.toTypedArray()).map { future -> future.join() }
            .toList()
            .filterNotNull()
            .associateBy { response -> response.username() }

        val cognitoUserIdToAttributes = cognitoIdToCognitoUser.values.associate { cognitoUser ->
            val attributes = extractCognitoUserAttributes(cognitoUser.userAttributes())
            Pair(cognitoUser.username(), attributes)
        }

        val githubUsers =
            githubUserDao.fetchByEmail(*cognitoUserIdToAttributes.values.map { attrs -> attrs["email"]!! }
                .toTypedArray())
        val emailToGithubUser = githubUsers.associateBy { githubUser -> githubUser.email }

        return cognitoUserIdToXtagesUser.values.map { user ->
            val attrs = cognitoUserIdToAttributes[user.cognitoUserId!!]!!
            val githubUser = emailToGithubUser[attrs["email"]]
            XtagesUserWithCognitoAttributes(
                user = user,
                githubUser = githubUser,
                attrs = attrs,
                userStatus = cognitoIdToCognitoUser[user.cognitoUserId!!]!!.userStatus()
            )
        }
    }

    /**
     * Finds the [XtagesUserWithCognitoAttributes] from the [builds].
     */
    fun findFromBuilds(vararg builds: Build): Map<Int, XtagesUserWithCognitoAttributes> {
        val xtagesUserIds = builds.mapNotNull { build -> build.userId }
        return when {
            xtagesUserIds.isNotEmpty() -> findCognitoUsersByXtagesUserId(*xtagesUserIds.toIntArray())
                .associateBy { user -> user.user.id!! }
            else -> emptyMap()
        }
    }

    /**
     * Finds the [XtagesUserWithCognitoAttributes] from the [builds].
     */
    fun findFromBuilds(builds: List<Build>): Map<Int, XtagesUserWithCognitoAttributes> =
        findFromBuilds(*builds.toTypedArray())

    private fun extractCognitoUserAttributes(attributeList: MutableList<AttributeType>): Map<String, String> {
        val attributes = attributeList.associate { attr -> Pair(attr.name(), attr.value()) }
        ensure.notNull(value = attributes["name"], valueDesc = "cognitoUser.name")
        ensure.notNull(value = attributes["email"], valueDesc = "cognitoUser.email")
        return attributes
    }
}

data class XtagesUserWithCognitoAttributes(
    val user: XtagesUser,
    val githubUser: GithubUser? = null,
    val attrs: Map<String, String>,
    val userStatus: UserStatusType,
)
