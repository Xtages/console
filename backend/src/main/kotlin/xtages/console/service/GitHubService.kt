package xtages.console.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.kohsuke.github.GHAppInstallationToken
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.springframework.stereotype.Service
import xtages.console.config.ConsoleProperties
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.reflect.KProperty

@Service
class GitHubService(consoleProperties: ConsoleProperties, val objectMapper: ObjectMapper) {
    private val gitHubClient: GitHub by GitHubClientDelegate(consoleProperties)

    fun handleWebhookRequest(eventType: GitHubWebhookEventType?, eventJson: String) {
        when (eventType) {
            GitHubWebhookEventType.INSTALLATION -> onInstallation(eventJson)
            GitHubWebhookEventType.INSTALLATION_REPOSITORIES -> TODO()
            GitHubWebhookEventType.REPOSITORY -> TODO()
        }
    }

    private fun onInstallation(eventJson: String) {
        // Ideally we'd use the follow `parseEventPayload` method but for some reason the contents of the
        // repositories present in the event payload don't have an `url` field populate and that makes this
        // method fail.
        // val event = gitHubClient.parseEventPayload(eventJson.reader(), GHEventPayload.Installation::class.java)
        val jsonBody = objectMapper.readTree(eventJson)
        val installationId = jsonBody.get("installation").get("id").asLong()
        val appGitHubClient =
            buildAppGitHubClient(gitHubClient.app.getInstallationById(installationId).createToken().create())
        println(appGitHubClient.isCredentialValid)
    }

    fun buildAppGitHubClient(installationToken: GHAppInstallationToken): GitHub {
        return GitHubBuilder().withAppInstallationToken(installationToken.token).build()!!
    }

    private class GitHubClientDelegate(val consoleProperties: ConsoleProperties) {
        lateinit var client: GitHub
        lateinit var expirationTime: Instant
        private val jwk: JWK = JWK.parseFromPEMEncodedObjects(consoleProperties.gitHubApp.privateKey)

        // `true` if the client hasn't been instantiated or the expirationTime claim inside the JWT has is about to
        // expire (1 minute).
        val needsNewClient: Boolean
            get() = !this::client.isInitialized || !this::expirationTime.isInitialized || expirationTime.until(
                Instant.now(),
                ChronoUnit.SECONDS
            ) >= 60

        operator fun getValue(thisRef: GitHubService, property: KProperty<*>): GitHub {
            if (needsNewClient) {
                client = buildGitHubClient()
            }
            return client
        }

        private fun buildGitHubClient(): GitHub {
            expirationTime = Date().toInstant().plus(10, ChronoUnit.MINUTES)
            val claimsSet = JWTClaimsSet.Builder()
                .issuer(consoleProperties.gitHubApp.identifier)
                .issueTime(Date())
                .expirationTime(Date.from(expirationTime))
                .build()
            val rsaKey = jwk.toRSAKey()
            val signer = RSASSASigner(rsaKey)
            val signedJwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).build(), claimsSet)
            signedJwt.sign(signer)
            val jwtString = signedJwt.serialize()
            return GitHubBuilder().withJwtToken(jwtString).build()!!
        }
    }
}

enum class GitHubWebhookEventType {
    INSTALLATION,
    INSTALLATION_REPOSITORIES,
    REPOSITORY;

    companion object {
        fun fromGitHubWebhookEventName(stripeEvent: String) =
            values().find { it.name.equals(stripeEvent, ignoreCase = true) }
    }
}
