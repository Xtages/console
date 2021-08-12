package xtages.console.config

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory
import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.converter.MessageConverter
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.services.acm.AcmAsyncClient
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient
import software.amazon.awssdk.services.codebuild.CodeBuildAsyncClient
import software.amazon.awssdk.services.codestarnotifications.CodestarNotificationsAsyncClient
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityAsyncClient
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient
import software.amazon.awssdk.services.ecr.EcrAsyncClient
import software.amazon.awssdk.services.rds.RdsAsyncClient
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ssm.SsmAsyncClient

@Configuration
class AwsClientConfig {

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
}
