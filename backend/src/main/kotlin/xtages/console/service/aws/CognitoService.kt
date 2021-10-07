package xtages.console.service.aws

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.*
import xtages.console.config.ConsoleProperties
import xtages.console.query.tables.pojos.Organization
import java.util.concurrent.ExecutionException

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
        try {
            cognitoIdentityProviderClient.createGroup(
                CreateGroupRequest.builder()
                    .userPoolId(consoleProperties.aws.cognito.userPoolId)
                    .groupName(groupName)
                    .build()
            ).get()
        } catch (e: ExecutionException) {
            if (e.cause !is GroupExistsException) {
                throw e
            }
        }
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
     * Updates the [attributes] for [username].
     */
    fun setUserAttributes(username: String, attributes: Map<String, String>) {
        cognitoIdentityProviderClient.adminUpdateUserAttributes(
            AdminUpdateUserAttributesRequest
                .builder()
                .userPoolId(consoleProperties.aws.cognito.userPoolId)
                .username(username)
                .userAttributes(attributes.map { entry ->
                    AttributeType.builder().name(entry.key).value(entry.value).build()
                })
                .build()
        )
    }

    /**
     * Creates a Cognito user with [username], [name] and adds the user to a Cognito group named [Organization#name].
     */
    fun createCognitoUser(username: String, name: String, organization: Organization): UserType {
        val response = cognitoIdentityProviderClient.adminCreateUser(
            AdminCreateUserRequest.builder()
                .userPoolId(consoleProperties.aws.cognito.userPoolId)
                .username(username)
                .userAttributes(
                    buildAttribute(name = "email", value = username),
                    buildAttribute(name = "name", value = name),
                    buildAttribute(name = "custom:organization-hash", value = organization.hash!!)
                )
                .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                .build()
        ).get()
        addUserToGroup(username = username, groupName = organization.name!!)
        return response.user()
    }

    private fun buildAttribute(name: String, value: String) = AttributeType.builder().name(name).value(value).build()
}
