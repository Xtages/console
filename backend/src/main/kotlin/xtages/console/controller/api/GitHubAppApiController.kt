package xtages.console.controller.api

import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.codec.Hex
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import xtages.console.config.ConsoleProperties
import xtages.console.service.GitHubWebhookEventType
import xtages.console.service.GitHubService
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val GIT_HUB_WEBHOOK_EVENT_HEADER = "X-GitHub-Event"
private const val GIT_HUB_SIGNATURE_HEADER = "X-HUB-Signature"
private const val HMAC_SHA1_ALGORITHM = "HmacSHA1"

private val logger = KotlinLogging.logger { }

@Controller
@RequestMapping("\${api.base-path:/api/v1/github}")
class GitHubAppApiController(
    private val consoleProperties: ConsoleProperties,
    private val gitHubService: GitHubService
) {

    @PostMapping("/webhook")
    fun webhook(
        @RequestBody body: String,
        @RequestHeader(GIT_HUB_WEBHOOK_EVENT_HEADER) eventTypeHeader: String,
        @RequestHeader(GIT_HUB_SIGNATURE_HEADER) gitHubSignatureHeader: String,
    ): ResponseEntity<String> {
        logger.trace { "Received GitHub webhook request." }
        if (!verifyRequest(body = body, gitHubSignatureHeader = gitHubSignatureHeader)) {
            logger.trace { "GitHub request failed validation." }
            return ResponseEntity.badRequest().body("Failed to validate webhook request.")
        }

        gitHubService.handleWebhookRequest(GitHubWebhookEventType.fromGitHubWebhookEventName(eventTypeHeader), body)
        return ResponseEntity.ok("")
    }

    private fun verifyRequest(body: String, gitHubSignatureHeader: String): Boolean {
        val sha1 = gitHubSignatureHeader.split('=')[1]
        val gitHubsDigest = Hex.decode(sha1)
        val secretKeySpec = SecretKeySpec(consoleProperties.gitHubApp.webhookSecret.toByteArray(), HMAC_SHA1_ALGORITHM)
        val hmac = Mac.getInstance(HMAC_SHA1_ALGORITHM)
        hmac.init(secretKeySpec)
        val ourDigest = hmac.doFinal(body.toByteArray())
        return MessageDigest.isEqual(gitHubsDigest, ourDigest)
    }
}

