package xtages.console.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("xtages.console")
data class ConsoleProperties(val stripe: Stripe, val server: Server, val gitHubApp: GitHubApp, val aws: Aws) {

    data class Stripe(val apiKey: String, val webhookSecret: String)

    data class Server(val basename: String)

    data class GitHubApp(val privateKey: String, val identifier: String, val webhookSecret: String)

    data class Cognito(val identityProviderName: String, val identityPoolId: String, val userPoolId: String)

    data class CodeBuild(
        val ecrRepository: String,
        val buildSpecsS3BucketArn: String,
        val buildEventsSnsTopicArn: String,
        val buildEventsSqsQueueArn: String
    )

    data class CloudWatch(val egressBytesMetricName: String)

    data class Aws(
        val accountId: String,
        val aimRoleArnPrefix: String,
        val cognito: Cognito,
        val codeBuild: CodeBuild,
        val cloudWatch: CloudWatch,
    )
}
