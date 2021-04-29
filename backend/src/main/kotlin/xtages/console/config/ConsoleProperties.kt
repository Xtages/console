package xtages.console.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("xtages.console")
data class ConsoleProperties(val stripe: Stripe, val server: Server, val gitHubApp: GitHubApp, val aws: Aws) {

    data class Stripe(val apiKey: String, val webhookSecret: String)

    data class Server(val basename: String)

    data class GitHubApp(val privateKey: String, val identifier: String, val webhookSecret: String)

    data class Aws(val ecrRepository: String, val buildSpecsS3Bucket: String)
}
