package xtages.console.email.template

import xtages.console.config.ConsoleProperties
import xtages.console.config.ConsoleProperties.*
import xtages.console.controller.api.model.Build
import xtages.console.controller.api.model.BuildType
import xtages.console.controller.api.model.Project
import java.io.File

fun mainX() {
    val project = Project(
        id = 1,
        name = "<TestProject>",
        organization = "Acme",
        ghRepoUrl = "https://github.com/acme/textproject",
        type = Project.Type.NODE,
        version = "15",
        passCheckRuleEnabled = false,
        builds = emptyList(),
        deployments = emptyList(),
        percentageOfSuccessfulBuildsInTheLastMonth = 0.0,
    )
    val build = Build(
        id = 70,
        buildNumber = 13,
        type = BuildType.CI,
        env = "dev",
        status = Build.Status.SUCCEEDED,
        initiatorName = "Rick James",
        initiatorEmail = "rjames@acme.net",
        commitHash = "abcdef",
        commitUrl = "https://github.com/acme/testproject/commit/abcdef",
        startTimestampInMillis = 0,
        endTimestampInMillis = 0,
        phases = emptyList(),
        actions = emptyList(),
    )
    val commitDesc = """fix: correct minor typos in code

see the issue for details

on typos fixed.

Reviewed-by: Z
Refs #133 """
    val properties = ConsoleProperties(
        server = Server(basename = "https://console.xtages.com", noReplyAddress = "", emailReturnPath = ""),
        stripe = Stripe(apiKey = "", webhookSecret = "", starterPriceIds = "", trialPeriod = 10),
        gitHubApp = GitHubApp(
            privateKey = "",
            identifier = 3,
            webhookSecret = "",
            clientId = "",
            clientSecret = "",
            installUrl = "",
        ),
        gitHubOauth = GitHubOauth(
            clientId = "",
            clientSecret = "",
        ),
        aws = Aws(
            accountId = "",
            aimRoleArnPrefix = "",
            cognito = Cognito(identityProviderName = "", identityPoolId = "", userPoolId = ""),
            codeBuild = CodeBuild(
                ecrRepository = "",
                buildSpecsS3BucketArn = "",
                buildEventsSnsTopicArn = "",
                buildEventsSqsQueueArn = ""
            ),
            cloudWatch = CloudWatch(egressBytesMetricName = "", customerNamespace = ""),

            rds = Rds(
                postgres = Postgres(
                    serverless = AuroraServerless(
                        engine = "", engineVersion = "", engineMode = "",
                        scaling = Scaling(
                            minCapacity = 1,
                            maxCapacity = 1,
                            secondsUntilAutoPause = 10,
                            autoPauseEnable = true
                        )
                    ),
                    instance = DbInstance(engineVersion = "", engine = "")
                ),
                storageType = "",
                dbSecurityGroup = "",
                backupRetentionPeriod = 10,
                enablePerformanceInsights = false,
                storageEncrypted = false,
                kmsKeyId = "",
                performanceInsightsRetentionPeriod = 10,
                publiclyAccessible = false,
                dbSubnetGroupName = "",
            ),
            vpc = Vpc("", emptyList()),
            ssm = Ssm(orgConfigPrefix = "")
        ),
        customerDeploymentDomain = ""
    )

    val outDir = File("/tmp", "templates")
    outDir.mkdir()
    val content = buildStatusChangedTemplate(
        consoleProperties = properties,
        project = project,
        build = build,
        commitDesc = commitDesc
    )
    writeFile(
        outDir = outDir,
        name = "buildStatusChangedTemplate.html",
        content = content.html.toString()
    )
    writeFile(
        outDir = outDir,
        name = "buildStatusChangedTemplate.txt",
        content = content.plain.toString()
    )
}

private fun writeFile(outDir: File, name: String, content: String) {
    val out = File(outDir, name)
    out.writeText(content)
}
