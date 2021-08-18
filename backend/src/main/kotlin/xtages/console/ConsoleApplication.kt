package xtages.console

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sts.StsAsyncClient
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest
import xtages.console.config.ConsoleProperties

private val logger = KotlinLogging.logger { }

@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = [ConsoleProperties::class])
class ConsoleApplication

fun main(args: Array<String>) {
    // Disable the JOOQ logo from logs
    System.getProperties().setProperty("org.jooq.no-logo", "true")
    runApplication<ConsoleApplication>(*args)
}

@Component
private class CallerIdentityLogger(private val stsClient: StsAsyncClient) {
    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent) {
        val callerIdentityResponse = stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build()).get()
        logger.info {
            """

AWS UserId: ${callerIdentityResponse.userId()}
AWS Account: ${callerIdentityResponse.account()}
AWS Arn: ${callerIdentityResponse.arn()}
"""
        }
    }
}
