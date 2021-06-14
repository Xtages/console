package xtages.console.service.aws

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.codestarnotifications.CodestarNotificationsAsyncClient
import software.amazon.awssdk.services.codestarnotifications.model.CreateNotificationRuleRequest
import software.amazon.awssdk.services.codestarnotifications.model.DetailType
import software.amazon.awssdk.services.codestarnotifications.model.NotificationRuleStatus
import software.amazon.awssdk.services.codestarnotifications.model.Target
import xtages.console.config.ConsoleProperties
import xtages.console.exception.ensure
import xtages.console.query.tables.pojos.Organization

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
        organization: Organization,
        eventTypeIds: List<String>,
    ): String {
        val organizationName = ensure.notNull(organization.name, valueDesc = "organization.name")
        val organizationHash = ensure.notNull(organization.hash, valueDesc = "organization.hash")
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
                .tags(mapOf(
                    "organization" to organizationHash,
                    "organizatio-name" to organizationName
                ))
                .build()
        ).get().arn()
    }
}
