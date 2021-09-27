package xtages.console.email.template

import xtages.console.config.ConsoleProperties
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
        server = ConsoleProperties.Server("https://console.xtages.com", "", ""),
        stripe = ConsoleProperties.Stripe("", "", "", 10),
        gitHubApp = ConsoleProperties.GitHubApp("", "", "", 0, "", "", ""),
        aws = ConsoleProperties.Aws(
            "",
            "",
            ConsoleProperties.Cognito("", "", ""),
            ConsoleProperties.CodeBuild("", "", "", ""),
            ConsoleProperties.CloudWatch(""),
            ConsoleProperties.Rds(
                "", "", "", "", 10, false, false, "", 10, false, "", ""
            )
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
