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
}
