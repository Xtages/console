package xtages.console.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("xtages.console")
data class ConsoleProperties(
    val stripe: Stripe,
    val server: Server,
    val gitHubApp: GitHubApp,
    val aws: Aws,
    val customerDeploymentDomain: String
) {

    data class Stripe(
        val apiKey: String,
        val webhookSecret: String,
        val starterPriceIds: String,
        val trialPeriod: Long
    )

    data class Server(val basename: String, val noReplyAddress: String, val emailReturnPath: String)

    data class GitHubApp(
        val privateKey: String,
        val identifier: Long,
        val webhookSecret: String,
        val clientId: String,
        val clientSecret: String,
        val installUrl: String
    )

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
        val rds: Rds,
    )

    data class Rds(
        val engine: RdsEngine,
        val engineVersion: RdsEngineVersion,
        val storageType: String,
        val dbSecurityGroup: String,
        val backupRetentionPeriod: Int,
        val enablePerformanceInsights: Boolean,
        val storageEncrypted: Boolean,
        val kmsKeyId: String,
        val performanceInsightsRetentionPeriod: Int,
        val publiclyAccessible: Boolean,
        val ssmPrefix: String,
        val dbSubnetGroupName: String,
        val scaling: ServerlessScaling,
    )

    data class RdsEngine(
        val postgres: String,
        val postgresServerless: String,
        val mode: String,
    )

    data class RdsEngineVersion(
        val postgres: String,
        val postgresServerless: String,
    )

    data class ServerlessScaling(
        val minCapacity: Int,
        val maxCapacity: Int,
        val secondsUntilAutoPause: Int,
        val autoPauseEnable: Boolean,
    )
}
