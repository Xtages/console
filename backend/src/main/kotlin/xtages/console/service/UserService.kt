package xtages.console.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityAsyncClient
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest
import xtages.console.config.ConsoleProperties
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.query.tables.pojos.XtagesUser

@Service
class UserService(
    private val userDao: XtagesUserDao,
    private val consoleProperties: ConsoleProperties,
    @Qualifier("anonymousCognitoIdentityClient")
    private val anonymousCognitoIdentityClient: CognitoIdentityAsyncClient,
    private val authenticationService: AuthenticationService,
) {

    /**
     * Creates new Xtages user and looks up it's Cognito Identity and stores it.
     */
    fun createUser(cognitoUserId: String, organizationName: String, isOwner: Boolean = false) {
        val idResponse = anonymousCognitoIdentityClient.getId(
            GetIdRequest.builder()
                .identityPoolId(consoleProperties.aws.cognitoIdentityPoolId)
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
}
