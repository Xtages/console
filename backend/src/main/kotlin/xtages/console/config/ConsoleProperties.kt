package xtages.console.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("xtages.console")
data class ConsoleProperties(
    val stripe: Stripe,
    val server: Server,
    val gitHubApp: GitHubApp,
    val gitHubOauth: GitHubOauth,
    val aws: Aws,
    val customerDeploymentDomain: String
) {

    data class Stripe(
        val apiKey: String,
        val webhookSecret: String,
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

    data class GitHubOauth(
        val clientId: String,
        val clientSecret: String,
    )

    data class Cognito(val identityProviderName: String, val identityPoolId: String, val userPoolId: String)

    data class CodeBuild(
        val ecrRepository: String,
        val buildSpecsS3BucketArn: String,
        val buildEventsSnsTopicArn: String,
        val buildEventsSqsQueueArn: String,
    )

    data class CloudWatch(val egressBytesMetricName: String, val customerNamespace: String)

    data class Aws(
        val accountId: String,
        val aimRoleArnPrefix: String,
        val cognito: Cognito,
        val codeBuild: CodeBuild,
        val cloudWatch: CloudWatch,
        val rds: Rds,
        val vpc: Vpc,
        val ssm: Ssm,
    )

    data class Vpc(
        val id: String,
        val privateSubnets: List<String>
    )

    data class Rds(
        val postgres: Postgres,
        val storageType: String,
        val dbSecurityGroup: String,
        val backupRetentionPeriod: Int,
        val enablePerformanceInsights: Boolean,
        val storageEncrypted: Boolean,
        val kmsKeyId: String,
        val performanceInsightsRetentionPeriod: Int,
        val publiclyAccessible: Boolean,
        val dbSubnetGroupName: String,
    )

    data class Postgres(
        val serverless: AuroraServerless,
        val instance: DbInstance,
    )

    data class AuroraServerless(
        val engine: String,
        val engineVersion: String,
        val engineMode: String,
        val scaling: Scaling,
    )

    data class DbInstance(
        val engineVersion: String,
        val engine: String
    )

    data class Scaling(
        val minCapacity: Int,
        val maxCapacity: Int,
        val secondsUntilAutoPause: Int,
        val autoPauseEnable: Boolean,
    )

    data class Ssm(
        val orgConfigPrefix: String,
    )
}
