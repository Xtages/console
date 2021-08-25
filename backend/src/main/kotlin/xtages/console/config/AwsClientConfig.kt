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
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher
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
import software.amazon.awssdk.services.sts.StsAsyncClient

@Configuration
class AwsClientConfig {

    @Bean
    fun cloudWatchMetricPublisher(): CloudWatchMetricPublisher {
        return CloudWatchMetricPublisher.create()
    }

    @Bean
    fun anonymousCognitoIdentityClient(): CognitoIdentityAsyncClient {
        return CognitoIdentityAsyncClient.builder()
            .overrideConfiguration(
                clientOverrideConfiguration()
            )
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .build()
    }

    @Bean
    fun acmClient(): AcmAsyncClient = AcmAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

    @Bean
    fun cognitoIdpClient(): CognitoIdentityProviderAsyncClient = CognitoIdentityProviderAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

    @Bean
    fun ecrAsyncClient(): EcrAsyncClient = EcrAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

    @Bean
    fun codeBuildClient(): CodeBuildAsyncClient = CodeBuildAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

    @Bean
    fun codestarNotificationsClient(): CodestarNotificationsAsyncClient = CodestarNotificationsAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

    @Bean
    fun cloudWatchLogsClient(): CloudWatchLogsAsyncClient = CloudWatchLogsAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

    @Bean
    fun cloudWatchClient(): CloudWatchAsyncClient = CloudWatchAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

    @Bean
    fun sesClient(): SesAsyncClient = SesAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

    @Bean
    fun stsClient(): StsAsyncClient = StsAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

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
    fun rdsAsyncClient(): RdsAsyncClient = RdsAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

    @Bean
    fun ssmAsyncClient(): SsmAsyncClient = SsmAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

    @Bean
    fun ecsAsyncClient(): EcsAsyncClient = EcsAsyncClient.builder()
        .overrideConfiguration(
            clientOverrideConfiguration()
        ).build()

    private fun clientOverrideConfiguration() = ClientOverrideConfiguration
        .builder()
        .addMetricPublisher(cloudWatchMetricPublisher())
        .build()
}
