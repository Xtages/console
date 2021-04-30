package xtages.console.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.services.codebuild.CodeBuildAsyncClient
import software.amazon.awssdk.services.ecr.EcrAsyncClient

@Configuration
class AwsClientConfig {

    @Bean
    fun ecrAsyncClient(): EcrAsyncClient {
        return EcrAsyncClient.create()
    }

    @Bean
    fun codeBuildClient(): CodeBuildAsyncClient {
        return CodeBuildAsyncClient.create()
    }
}
