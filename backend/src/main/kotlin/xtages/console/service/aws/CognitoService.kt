package xtages.console.service.aws

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.*
import xtages.console.config.ConsoleProperties
import xtages.console.query.tables.pojos.Organization

/**
 * A [Service] to interact with Cognito.
 */
@Service
class CognitoService(
    val cognitoIdentityProviderClient: CognitoIdentityProviderAsyncClient,
    val consoleProperties: ConsoleProperties
) {

    /**
     * Creates a new Cognito group.
     */
    fun createGroup(groupName: String) {
        cognitoIdentityProviderClient.createGroup(
            CreateGroupRequest.builder()
                .userPoolId(consoleProperties.aws.cognito.userPoolId)
                .groupName(groupName)
                .build()
        )
    }

    /**
     * Returns all the Cognito users that are part of [groupName].
     */
    fun getUsersInGroup(groupName: String): MutableList<UserType> {
        fun listUsers(groupName: String, nextToken: String? = null): ListUsersInGroupResponse {
            val builder = ListUsersInGroupRequest.builder()
                .groupName(groupName)
                .userPoolId(consoleProperties.aws.cognito.userPoolId)
            if (nextToken != null) {
                builder.nextToken(nextToken)
            }
            return cognitoIdentityProviderClient.listUsersInGroup(builder.build()).get()
        }

        val allUsers = mutableListOf<UserType>()
        var nextToken: String? = null
        do {
            val response = listUsers(groupName = groupName, nextToken = nextToken)
            nextToken = response.nextToken()
            allUsers.addAll(response.users())
        } while (nextToken != null)
        return allUsers
    }

    /**
     * Adds a user (by [username]) to [groupName].
     */
    fun addUserToGroup(username: String, groupName: String) {
        cognitoIdentityProviderClient.adminAddUserToGroup(
            AdminAddUserToGroupRequest
                .builder()
                .userPoolId(consoleProperties.aws.cognito.userPoolId)
                .groupName(groupName)
                .username(username)
                .build()
        )
    }

    /**
     * Creates a Cognito user with [username], [name] and adds the user to a Cognito group named [Organization#name].
     */
    fun createCognitoUser(username: String, name: String, organization: Organization): UserType {
        val response = cognitoIdentityProviderClient.adminCreateUser(
            AdminCreateUserRequest.builder()
                .username(username)
                .userAttributes(
                    buildAttribute(name = "email", value = username),
                    buildAttribute(name = "name", value = username),
                    buildAttribute(name = "custom:organization", value = organization.name!!)
                )
                .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                .messageAction(MessageActionType.RESEND)
                .build()
        ).get()
        addUserToGroup(username = username, groupName = organization.name!!)
        return response.user()
    }

    private fun buildAttribute(name: String, value: String) = AttributeType.builder().name(name).value(value).build()
}
