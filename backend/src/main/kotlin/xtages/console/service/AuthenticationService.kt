package xtages.console.service

import com.github.benmanes.caffeine.cache.Cache
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityAsyncClient
import software.amazon.awssdk.services.cognitoidentity.model.Credentials
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import xtages.console.config.CognitoUserId
import xtages.console.config.ConsoleProperties
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.XtagesUserDao

@Service
class AuthenticationService(
    private val consoleProperties: ConsoleProperties,
    private val userDao: XtagesUserDao,
    @Qualifier("anonymousCognitoIdentityClient")
    private val anonymousCognitoIdentityClient: CognitoIdentityAsyncClient,
    private val cache: Cache<String, Credentials>,
) {

    /**
     * Returns the [Jwt] from the current authentication principal.
     */
    val jwt: Jwt
        get() = SecurityContextHolder.getContext().authentication.principal as Jwt

    /**
     * Returns the cognito id of the currently signed-in user.
     */
    val currentCognitoUserId: CognitoUserId
        get() {
            return CognitoUserId(jwt.subject)
        }

    /**
     * Returns a [Map<String,String>] of `cognitoIdentityProviderName` to `JWT` id token.
     */
    val loginsMap: Map<String, String>
        get() = mapOf(consoleProperties.aws.cognito.identityProviderName to jwt.tokenValue)

    /**
     * Returns the [AwsSessionCredentials] for the currently logged in user.
     */
    val userAwsSessionCredentials: AwsSessionCredentials
        get() {
            val user = ensure.foundOne(
                operation = { userDao.fetchOneByCognitoUserId(jwt.subject) },
                code = ExceptionCode.USER_NOT_FOUND
            )
            val cognitoIdentityId = ensure.notNull(value = user.cognitoIdentityId, valueDesc = "user.cognitoIdentityId")
            val cachedCredentials: Credentials =
                ensure.notNull(
                    value = cache.get(cognitoIdentityId) {
                        val credentialsForIdentity = anonymousCognitoIdentityClient.getCredentialsForIdentity(
                            GetCredentialsForIdentityRequest.builder()
                                .identityId(user.cognitoIdentityId)
                                .logins(loginsMap)
                                .build()
                        ).get()
                        credentialsForIdentity.credentials()
                    },
                    valueDesc = "cachedCredentials"
                )
            return AwsSessionCredentials.create(
                cachedCredentials.accessKeyId(),
                cachedCredentials.secretKey(),
                cachedCredentials.sessionToken()
            )
        }
}
