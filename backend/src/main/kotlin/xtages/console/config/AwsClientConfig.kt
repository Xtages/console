package xtages.console.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.services.codebuild.CodeBuildAsyncClient
import software.amazon.awssdk.services.codestarnotifications.CodestarNotificationsAsyncClient
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityAsyncClient
import software.amazon.awssdk.services.ecr.EcrAsyncClient

@Configuration
class AwsClientConfig {

    @Bean
    fun anonymousCognitoIdentityClient(): CognitoIdentityAsyncClient {
        return CognitoIdentityAsyncClient.builder()
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .build()
    }

    @Bean
    fun ecrAsyncClient(): EcrAsyncClient = EcrAsyncClient.create()

    @Bean
    fun codeBuildClient(): CodeBuildAsyncClient = CodeBuildAsyncClient.create()

    @Bean
    fun codestarNotificationsClient(): CodestarNotificationsAsyncClient = CodestarNotificationsAsyncClient.create()
}
