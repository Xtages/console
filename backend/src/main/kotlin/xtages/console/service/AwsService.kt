package xtages.console.service

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.codebuild.CodeBuildAsyncClient
import software.amazon.awssdk.services.codebuild.model.*
import software.amazon.awssdk.services.ecr.EcrAsyncClient
import software.amazon.awssdk.services.ecr.model.CreateRepositoryRequest
import software.amazon.awssdk.services.ecr.model.ImageTagMutability
import xtages.console.config.ConsoleProperties
import xtages.console.exception.ensure
import xtages.console.pojo.*
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import software.amazon.awssdk.services.codebuild.model.Tag as CodebuildTag
import software.amazon.awssdk.services.ecr.model.Tag as EcrTag

private val xtagesEcrTag: EcrTag = EcrTag.builder().key("XTAGES_CONSOLE_CREATED").value("true").build()
private val xtagesCodeBuildTag: CodebuildTag = CodebuildTag.builder()
    .key("XTAGES_CONSOLE_CREATED")
    .value("true")
    .build()
private val envVars = listOf(
    buildPlaceholderEnvironmentVariable("XTAGES_COMMIT"),
    buildPlaceholderEnvironmentVariable("XTAGES_REPO"),
    buildPlaceholderEnvironmentVariable("XTAGES_GITHUB_TOKEN"),
)


private enum class CodeBuildType {
    CI,
    CD
}

@Service
class AwsService(
    private val ecrAsyncClient: EcrAsyncClient,
    private val codeBuildAsyncClient: CodeBuildAsyncClient,
    private val organizationDao: OrganizationDao,
    private val projectDao: ProjectDao,
    private val consoleProperties: ConsoleProperties
) {
    /**
     * Creates a [Project]-related infrastructure in AWS. Specifically creates:
     *   * an ECR repository for the [organization] if one hasn't been created already
     *   * a CodeBuild project for CI
     *   * a CodeBuild project for CD
     */
    fun registerProject(project: Project, organization: Organization) {
        createEcrRepositoryForOrganization(organization = organization)
        createCodeBuildCiProject(project = project)
        createCodeBuildCdProject(project = project)
    }

    private fun createCodeBuildCiProject(project: Project) {
        val createProResponse = codeBuildAsyncClient.createProject(
            buildCreateProjectRequest(
                project = project,
                codeBuildType = CodeBuildType.CI,
                privilegedMode = false,
                serviceRoleArn = "${consoleProperties.aws.aimRoleArnPrefix}/xtages-codebuild-ci-role",
            )
        ).get()
        project.codebuildCiProjectArn = createProResponse.project().arn()
        projectDao.merge(project)
    }

    private fun createCodeBuildCdProject(project: Project) {
        val createProResponse = codeBuildAsyncClient.createProject(
            buildCreateProjectRequest(
                project = project,
                codeBuildType = CodeBuildType.CD,
                privilegedMode = true,
                serviceRoleArn = "${consoleProperties.aws.aimRoleArnPrefix}/xtages-codebuild-cd-role",
            )
        ).get()
        project.codebuildCdProjectArn = createProResponse.project().arn()
        projectDao.merge(project)
    }

    private fun buildCreateProjectRequest(
        project: Project,
        codeBuildType: CodeBuildType,
        privilegedMode: Boolean,
        serviceRoleArn: String,
    ): CreateProjectRequest {
        fun <T> buildTypeVar(ciVar: T, cdVar: T) = if (codeBuildType == CodeBuildType.CI) ciVar else cdVar
        val imageName = buildTypeVar(project.codeBuildCiImageName, project.codeBuildCdImageName)
        val projectName = buildTypeVar(project.codeBuildCiProjectName, project.codeBuildCdProjectName)
        val logsGroupName = buildTypeVar(project.codeBuildCiLogsGroupName, project.codeBuildCdLogsGroupName)
        val logsStreamName = buildTypeVar(project.codeBuildCiLogsStreamName, project.codeBuildCdLogsStreamName)
        val buildSpecLocation = buildTypeVar(project.codeBuildCiBuildSpecName, project.codeBuildCdBuildSpecName)
        return CreateProjectRequest.builder()
            .name(projectName)
            .serviceRole(serviceRoleArn)
            .source(
                ProjectSource.builder()
                    .type(SourceType.NO_SOURCE)
                    .buildspec("${consoleProperties.aws.buildSpecsS3BucketArn}/${buildSpecLocation}")
                    .build()
            )
            .artifacts(ProjectArtifacts.builder()
                .type(ArtifactsType.NO_ARTIFACTS)
                .build())
            .environment(
                ProjectEnvironment.builder()
                    .image("${consoleProperties.aws.ecrRepository}/${imageName}")
                    .computeType(ComputeType.BUILD_GENERAL1_SMALL)
                    .type(EnvironmentType.LINUX_CONTAINER)
                    .environmentVariables(envVars)
                    .privilegedMode(privilegedMode)
                    .build()
            )
            .logsConfig(
                LogsConfig.builder()
                    .cloudWatchLogs(
                        CloudWatchLogsConfig.builder()
                            .status(LogsConfigStatusType.ENABLED)
                            .groupName(logsGroupName)
                            .streamName(logsStreamName)
                            .build()
                    )
                    .build()
            )
            .tags(xtagesCodeBuildTag)
            .badgeEnabled(false)
            .build()
    }

    private fun createEcrRepositoryForOrganization(organization: Organization) {
        if (organization.ecrRepositoryArn == null) {
            val name = ensure.notNull(value = organization.name, valueDesc = "organization.name")
            val createRepositoryResponse = ecrAsyncClient.createRepository(
                CreateRepositoryRequest.builder()
                    .repositoryName(name.toLowerCase())
                    .imageTagMutability(ImageTagMutability.IMMUTABLE)
                    .tags(xtagesEcrTag)
                    .build()
            ).get()
            organization.ecrRepositoryArn = createRepositoryResponse.repository().repositoryArn()
            organizationDao.merge(organization)
        }
    }
}

private fun buildPlaceholderEnvironmentVariable(name: String) = EnvironmentVariable.builder()
    .type(EnvironmentVariableType.PLAINTEXT)
    .name(name)
    .value("FILL_ME")
    .build()
