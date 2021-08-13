package xtages.console.config

import com.amazonaws.auth.*
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.core.credentials.CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory
import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.converter.MessageConverter
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.acm.AcmAsyncClient
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient
import software.amazon.awssdk.services.codebuild.CodeBuildAsyncClient
import software.amazon.awssdk.services.codestarnotifications.CodestarNotificationsAsyncClient
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityAsyncClient
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient
import software.amazon.awssdk.services.ecr.EcrAsyncClient
import software.amazon.awssdk.services.ecs.EcsAsyncClient
import software.amazon.awssdk.services.rds.RdsAsyncClient
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ssm.SsmAsyncClient

@Configuration
class AwsClientConfig {

    @Bean(CREDENTIALS_PROVIDER_BEAN_NAME)
    @Primary
    @Profile("dev")
    fun devCredentialsProvider(): AWSCredentialsProvider = DevAwsCredentialsProvider("local-dev")

    @Bean
    fun anonymousCognitoIdentityClient(): CognitoIdentityAsyncClient {
        return CognitoIdentityAsyncClient.builder()
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .build()
    }

    @Bean
    fun acmClient(): AcmAsyncClient = AcmAsyncClient.create()

    @Bean
    fun cognitoIdpClient(): CognitoIdentityProviderAsyncClient = CognitoIdentityProviderAsyncClient.create()

    @Bean
    fun ecrAsyncClient(): EcrAsyncClient = EcrAsyncClient.create()

    @Bean
    fun codeBuildClient(): CodeBuildAsyncClient = CodeBuildAsyncClient.create()

    @Bean
    fun codestarNotificationsClient(): CodestarNotificationsAsyncClient = CodestarNotificationsAsyncClient.create()

    @Bean
    fun cloudWatchLogsClient(): CloudWatchLogsAsyncClient = CloudWatchLogsAsyncClient.create()

    @Bean
    fun cloudWatchClient(): CloudWatchAsyncClient = CloudWatchAsyncClient.create()

    @Bean
    fun sesClient(): SesAsyncClient = SesAsyncClient.create()

    @Bean
    fun queueMessagingTemplate(amazonSQSAsync: AmazonSQSAsync): QueueMessagingTemplate =
        QueueMessagingTemplate(amazonSQSAsync)

    @Bean
    fun queueMessageHandlerFactory(mapper: ObjectMapper, amazonSQSAsync: AmazonSQSAsync): QueueMessageHandlerFactory {
        val queueHandlerFactory = QueueMessageHandlerFactory()
        queueHandlerFactory.setAmazonSqs(amazonSQSAsync)
        queueHandlerFactory.setArgumentResolvers(
            listOf(PayloadMethodArgumentResolver(jackson2MessageConverter(mapper)))
        )
        return queueHandlerFactory
    }

    private fun jackson2MessageConverter(mapper: ObjectMapper): MessageConverter {
        val converter = MappingJackson2MessageConverter()
        // set strict content type match to false to enable the listener to handle AWS events
        converter.isStrictContentTypeMatch = false
        converter.objectMapper = mapper
        return converter
    }

    @Bean
    fun rdsAsyncClient(): RdsAsyncClient = RdsAsyncClient.create()

    @Bean
    fun ssmAsyncClient(): SsmAsyncClient = SsmAsyncClient.create()

    @Bean
    fun ecsAsyncClient(): EcsAsyncClient = EcsAsyncClient.create()
}

/**
 * This an AWS SDK v1 adapter around [ProfileCredentialsProvider] (which is an AWS SDK v2 class).
 * This adapter is necessary because the AWS SDK v1 has a [bug](https://github.com/aws/aws-sdk-java/issues/803) around
 * how it reads profiles from `~./aws/config` that makes it impossible for `cloud.aws.credentials.profile-name` to work
 * correctly, however the in the AWS SDK v2 it has been fixed. Unfortunately awspring uses v1 of the SDK and hence we
 * need this adapter.
 */
private class DevAwsCredentialsProvider(profileName: String) : AWSCredentialsProvider {
    private val profileCredentialsProvider = ProfileCredentialsProvider.create(profileName)

    override fun getCredentials(): AWSCredentials {
        val credentials = profileCredentialsProvider.resolveCredentials()
        if (credentials is AwsSessionCredentials) {
            return BasicSessionCredentials(
                credentials.accessKeyId(),
                credentials.secretAccessKey(),
                credentials.sessionToken()
            )
        }

        return BasicAWSCredentials(
            credentials.accessKeyId(),
            credentials.secretAccessKey()
        )
    }

    override fun refresh() {
        profileCredentialsProvider.resolveCredentials()
    }
}
