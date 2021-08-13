package xtages.console.email.template

import com.fizzed.rocker.RockerOutput
import com.fizzed.rocker.runtime.StringBuilderOutput
import org.springframework.web.util.UriComponentsBuilder
import xtages.console.config.ConsoleProperties
import xtages.console.controller.api.model.Build
import xtages.console.controller.api.model.BuildType
import xtages.console.controller.api.model.Project
import xtages.console.email.CdnAssets.thumbsDownImgUrl
import xtages.console.email.CdnAssets.thumbsUpImgUrl
import xtages.console.email.EmailContents
import xtages.console.email.templates.BuildStatusChangedHtml
import xtages.console.email.templates.BuildStatusChangedPlain
import xtages.console.email.templates.partials.ButtonVariant
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import java.net.URI

fun buildStatusChangedTemplate(
    consoleProperties: ConsoleProperties,
    project: Project,
    build: Build,
    commitDesc: String
): EmailContents {
    ensure.isTrue(
        value = build.type == BuildType.CI || (build.env == "staging" && build.status == Build.Status.FAILED),
        code = ExceptionCode.INVALID_BUILD_TYPE,
        message = "Only CI Builds expected or failed CD Builds"
    )
    return EmailContents(
        subject = buildTitle(build, project),
        html = buildStatusChangedTemplateHtml(
            consoleProperties = consoleProperties,
            project = project,
            build = build,
            commitDesc = commitDesc
        ),
        plain = buildStatusChangedTemplatePlain(
            consoleProperties = consoleProperties,
            project = project,
            build = build,
            commitDesc = commitDesc
        )
    )
}

private fun buildStatusChangedTemplatePlain(
    consoleProperties: ConsoleProperties,
    project: Project,
    build: Build,
    commitDesc: String
): StringBuilderOutput {
    val title = buildTitle(build, project)
    return BuildStatusChangedPlain.template(
        title,
        getProjectUrl(consoleProperties, project),
        URI.create(build.commitUrl),
        build.commitHash,
        build.initiatorName,
        build.initiatorEmail,
        commitDesc
    ).render(StringBuilderOutput.FACTORY)
}

private fun buildStatusChangedTemplateHtml(
    consoleProperties: ConsoleProperties,
    project: Project,
    build: Build,
    commitDesc: String
): StringBuilderOutput {
    val title = buildTitle(build, project)
    val statusIconUrl = when (build.status) {
        Build.Status.SUCCEEDED -> thumbsUpImgUrl
        else -> thumbsDownImgUrl
    }

    val buttonVariant = if (build.status == Build.Status.SUCCEEDED) ButtonVariant.SUCCESS else ButtonVariant.DANGER
    return BuildStatusChangedHtml.template(
        title,
        statusIconUrl,
        getProjectUrl(consoleProperties, project),
        buttonVariant,
        URI.create(build.commitUrl),
        build.commitHash,
        build.initiatorName,
        build.initiatorEmail,
        commitDesc
    ).render(StringBuilderOutput.FACTORY)
}

private fun getProjectUrl(consoleProperties: ConsoleProperties, project: Project) =
    UriComponentsBuilder.fromUri(URI(consoleProperties.server.basename))
        .pathSegment("project")
        .pathSegment(project.name)
        .build()
        .toUri()

private fun buildTitle(build: Build, project: Project): String {
    return when (build.type) {
        BuildType.CI ->
            when (build.status) {
                Build.Status.SUCCEEDED -> "Build(#${build.buildNumber}) for ${project.name} was fixed"
                else -> "Build(#${build.buildNumber}) for ${project.name} failed"
            }
        BuildType.CD -> when (build.status) {
            Build.Status.SUCCEEDED -> throw IllegalArgumentException("Build status: ${build.status}")
            else -> "Deployment to ${build.env} for ${project.name} failed"
        }
    }
}
