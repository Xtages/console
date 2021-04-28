package xtages.console.service

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.codebuild.CodeBuildAsyncClient
import software.amazon.awssdk.services.codebuild.model.*
import software.amazon.awssdk.services.ecr.EcrAsyncClient
import software.amazon.awssdk.services.ecr.model.CreateRepositoryRequest
import software.amazon.awssdk.services.ecr.model.ImageTagMutability
import xtages.console.config.ConsoleProperties
import xtages.console.pojo.codeBuildCiImageName
import xtages.console.pojo.codeBuildCiLogsGroupName
import xtages.console.pojo.codeBuildCiLogsStreamName
import xtages.console.pojo.codeBuildCiProjectName
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

@Service
class AwsService(
    private val ecrAsyncClient: EcrAsyncClient,
    private val codeBuildAsyncClient: CodeBuildAsyncClient,
    private val organizationDao: OrganizationDao,
    private val projectDao: ProjectDao,
    private val consoleProperties: ConsoleProperties
) {
    fun registerProject(project: Project, organization: Organization) {
        createEcrRepositoryForOrganization(organization = organization)
        createCodeBuildCiProject(project = project)
    }

    private fun createCodeBuildCiProject(project: Project) {
        val createProResponse = codeBuildAsyncClient.createProject(
            CreateProjectRequest.builder()
                .name(project.codeBuildCiProjectName)
                .badgeEnabled(false)
                .environment(
                    ProjectEnvironment.builder()
                        .image("${consoleProperties.aws.ecrRepository}/${project.codeBuildCiImageName}")
                        .computeType(ComputeType.BUILD_GENERAL1_SMALL)
                        .type(EnvironmentType.LINUX_CONTAINER)
                        .environmentVariables(envVars)
                        .privilegedMode(false) // in CI we don't create images
                        .build()
                )
                .logsConfig(
                    LogsConfig.builder()
                        .cloudWatchLogs(
                            CloudWatchLogsConfig.builder()
                                .status(LogsConfigStatusType.ENABLED)
                                .groupName(project.codeBuildCiLogsGroupName)
                                .streamName(project.codeBuildCiLogsStreamName)
                                .build()
                        )
                        .build()
                )
                .tags(xtagesCodeBuildTag)
                .build()
        ).get()
        project.codebuildCiProjectArn = createProResponse.project().arn()
        projectDao.merge(project)
    }

    private fun createEcrRepositoryForOrganization(organization: Organization) {
        if (organization.ecrRepositoryArn == null) {
            val createRepositoryResponse = ecrAsyncClient.createRepository(
                CreateRepositoryRequest.builder()
                    .repositoryName(organization.name)
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
