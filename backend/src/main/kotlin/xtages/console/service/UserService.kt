package xtages.console.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityAsyncClient
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException
import xtages.console.config.ConsoleProperties
import xtages.console.exception.ExceptionCode.USER_NOT_FOUND
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.GithubUserDao
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.query.tables.pojos.GithubUser
import xtages.console.query.tables.pojos.XtagesUser
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
) {

    /**
     * Creates new Xtages user and looks up it's Cognito Identity and stores it.
     */
    fun createUser(cognitoUserId: String, organizationName: String, isOwner: Boolean = false) {
        val idResponse = anonymousCognitoIdentityClient.getId(
            GetIdRequest.builder()
                .identityPoolId(consoleProperties.aws.cognito.identityPoolId)
                .logins(authenticationService.loginsMap)
                .accountId(consoleProperties.aws.accountId)
                .build()
        ).get()
        val owner = XtagesUser(
            cognitoUserId = cognitoUserId,
            cognitoIdentityId = idResponse.identityId(),
            organizationName = organizationName,
            isOwner = true
        )
        userDao.insert(owner)
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
            val cognitoUserAttributes = extractCognitoUserAttributes(cognitoUser)
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
            )
        } catch (e: UserNotFoundException) {
            null
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
            val attributes = extractCognitoUserAttributes(cognitoUser)
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
            )
        }
    }

    private fun extractCognitoUserAttributes(cognitoUser: AdminGetUserResponse): Map<String, String> {
        val attributes = cognitoUser.userAttributes().associate { attr -> Pair(attr.name(), attr.value()) }
        ensure.notNull(
            value = attributes["name"],
            valueDesc = "cognitoUser.name",
            message = cognitoUser.username()
        )
        ensure.notNull(
            value = attributes["email"],
            valueDesc = "cognitoUser.email",
            message = cognitoUser.username()
        )
        return attributes
    }
}

data class XtagesUserWithCognitoAttributes(
    val user: XtagesUser,
    val githubUser: GithubUser?,
    val attrs: Map<String, String>,
)
