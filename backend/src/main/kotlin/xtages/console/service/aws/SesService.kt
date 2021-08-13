package xtages.console.service.aws

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model.*
import xtages.console.config.ConsoleProperties
import xtages.console.email.EmailContents
import xtages.console.service.EmailRecipients

@Service
class SesService(private val consoleProperties: ConsoleProperties, private val sesClient: SesAsyncClient) {

    /**
     * Sends email to [recipients] with [contents]. If [replyTo] is `null` then
     * [ConsoleProperties.server.noReplyAddress] will be used.
     */
    fun sendEmail(recipients: EmailRecipients, contents: EmailContents, replyTo: String? = null) {
        val replyAddress = replyTo ?: consoleProperties.server.noReplyAddress
        sesClient.sendEmail(
            SendEmailRequest.builder()
                .source(replyAddress)
                .replyToAddresses(replyAddress)
                .returnPath(consoleProperties.server.emailReturnPath)
                .destination(
                    Destination.builder()
                        .toAddresses(recipients.toAddresses)
                        .ccAddresses(recipients.ccAddresses)
                        .bccAddresses(recipients.bccAddresses)
                        .build()
                )
                .message(
                    Message.builder()
                        .subject(buildContent(contents.subject))
                        .body(
                            Body.builder()
                                .html(buildContent(contents.html.toString()))
                                .text(buildContent(contents.plain.toString()))
                                .build()
                        )
                        .build()
                )
                .build()
        ).get()
    }

    private fun buildContent(str: String) = Content.builder().data(str).charset("UTF-8").build()
}
