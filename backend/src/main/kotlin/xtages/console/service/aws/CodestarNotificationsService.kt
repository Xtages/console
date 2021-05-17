package xtages.console.service.aws

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.codestarnotifications.CodestarNotificationsAsyncClient
import software.amazon.awssdk.services.codestarnotifications.model.CreateNotificationRuleRequest
import software.amazon.awssdk.services.codestarnotifications.model.DetailType
import software.amazon.awssdk.services.codestarnotifications.model.NotificationRuleStatus
import software.amazon.awssdk.services.codestarnotifications.model.Target
import xtages.console.config.ConsoleProperties

/**
 * A [Service] to handle the logic to communicate to the AWS Codestar service.
 */
@Service
class CodestarNotificationsService(
    private val codestarNotificationsAsyncClient: CodestarNotificationsAsyncClient,
    private val consoleProperties: ConsoleProperties
) {

    /**
     * Creates an AWS CodeStar notification rule.
     */
    fun createNotificationRule(
        notificationRuleName: String,
        projectArn: String,
        organizationName: String,
        eventTypeIds: List<String>,
    ): String {
        return codestarNotificationsAsyncClient.createNotificationRule(
            CreateNotificationRuleRequest.builder()
                .name(notificationRuleName)
                .resource(projectArn)
                .detailType(DetailType.FULL)
                .eventTypeIds(
                    eventTypeIds,
                )
                .targets(
                    Target.builder()
                        .targetAddress(consoleProperties.aws.codeBuild.buildEventsSnsTopicArn)
                        .targetType("SNS")
                        .build()
                )
                .status(NotificationRuleStatus.ENABLED)
                .tags(mapOf("organization" to organizationName))
                .build()
        ).get().arn()
    }
}
