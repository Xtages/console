package xtages.console.service

import org.springframework.stereotype.Service
import xtages.console.config.ConsoleProperties
import xtages.console.controller.api.model.Build
import xtages.console.controller.api.model.Organization
import xtages.console.controller.api.model.Project
import xtages.console.email.EmailContents
import xtages.console.email.template.buildStatusChangedTemplate
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.service.aws.SesService

/**
 * Service to sends notifications of various events.
 *
 * **Currently only email notifications are supported.**
 */
@Service
class NotificationService(private val consoleProperties: ConsoleProperties, private val sesService: SesService) {

    /**
     * Sends a notification, that the status of the [build] has changed, to the [recipients].
     */
    fun sendBuildStatusChangedNotification(
        recipients: List<Recipients>,
        organization: Organization,
        project: Project,
        build: Build,
        commitDesc: String
    ) {
        val notification = Notification(
            emailContents = buildStatusChangedTemplate(
                consoleProperties = consoleProperties,
                project = project,
                build = build,
                commitDesc = commitDesc
            )
        )
        sendEmail(recipients = recipients, notification = notification)
    }

    private fun sendEmail(recipients: List<Recipients>, notification: Notification) {
        ensure.notEmpty(value = recipients, valueDesc = "recipients")
        val recipientsByType = recipients.groupBy { it::class }
        recipientsByType.keys.forEach { recipientClass ->
            when (recipientClass) {
                EmailRecipients::class -> {
                    val recipient = ensure.foundOne(
                        operation = {
                            recipientsByType[recipientClass]?.singleOrNull()
                        },
                        code = ExceptionCode.INVALID_TYPE,
                        message = "More than one email recipient object found"
                    )
                    sesService.sendEmail(
                        recipients = recipient as EmailRecipients,
                        contents = notification.emailContents
                    )
                }
                else -> throw UnsupportedOperationException("Trying to send notification of type $recipientClass")
            }
        }
    }
}

private data class Notification(val emailContents: EmailContents)

interface Recipients

data class EmailRecipients(
    val toAddresses: List<String>? = null,
    val ccAddresses: List<String>? = null,
    val bccAddresses: List<String>? = null,
) : Recipients

data class WebPushRecipients(val tokens: List<String>) : Recipients
