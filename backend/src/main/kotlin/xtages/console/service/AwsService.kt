package xtages.console.service

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.codebuild.CodeBuildAsyncClient
import software.amazon.awssdk.services.codebuild.model.*
import software.amazon.awssdk.services.codestarnotifications.CodestarNotificationsAsyncClient
import software.amazon.awssdk.services.codestarnotifications.model.CreateNotificationRuleRequest
import software.amazon.awssdk.services.codestarnotifications.model.DetailType
import software.amazon.awssdk.services.codestarnotifications.model.NotificationRuleStatus
import software.amazon.awssdk.services.codestarnotifications.model.Target
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
import java.time.Duration
import java.time.Instant
import software.amazon.awssdk.services.codebuild.model.Tag as CodebuildTag
import software.amazon.awssdk.services.ecr.model.Tag as EcrTag

private val xtagesEcrTag: EcrTag = EcrTag.builder().key("XTAGES_CONSOLE_CREATED").value("true").build()
private val xtagesCodeBuildTag = buildCodeBuildProjectTag(key = "XTAGES_CONSOLE_CREATED", value = "true")

private val envVars = listOf(
    buildEnvironmentVariable("XTAGES_COMMIT"),
    buildEnvironmentVariable("XTAGES_REPO"),
    buildEnvironmentVariable("XTAGES_GITHUB_TOKEN"),
)

enum class CodeBuildType {
    CI,
    CD
}

private val logger = KotlinLogging.logger { }

@Service
class AwsService(
    private val gitHubService: GitHubService,
    private val ecrAsyncClient: EcrAsyncClient,
    private val codeBuildAsyncClient: CodeBuildAsyncClient,
    private val codestarNotificationsAsyncClient: CodestarNotificationsAsyncClient,
    private val organizationDao: OrganizationDao,
    private val projectDao: ProjectDao,
    private val consoleProperties: ConsoleProperties,
    private val authenticationService: AuthenticationService,
) {

    @SqsListener("build-updates-queue")
    fun codeBuildEventListener(event: CodeBuildEvent) {
        println(event)
    }

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

    /**
     * Starts a CodeBuild project for a specific [Project]
     * codeBuildStarterRequest provides information about the [CodeBuildType] to run and
     * al the necessary information to make the build run
     */
    fun startCodeBuildProject(
        project: Project, organization: Organization,
        commit: String, codeBuildType: CodeBuildType
    ) {
        logger.info { "running CodeBuild: ${codeBuildType} for project : ${project.name} commit: ${commit} organization: ${organization.name}" }
        val token = gitHubService.appToken(organization)
        val cbProjectName = if (codeBuildType == CodeBuildType.CI)
            project.codeBuildCiProjectName
        else
            project.codeBuildCdBuildSpecName

        userSessionCodeBuildClient().startBuild(
            StartBuildRequest.builder()
                .projectName(cbProjectName)
                .environmentVariablesOverride(
                    listOf(
                        buildEnvironmentVariable("XTAGES_COMMIT", commit),
                        buildEnvironmentVariable("XTAGES_REPO", project.ghRepoFullName),
                        buildEnvironmentVariable("XTAGES_GITHUB_TOKEN", token)
                    )
                )
                .build()
        ).get()
        logger.info { "started CodeBuild project: ${cbProjectName}" }
    }

    private fun userSessionCodeBuildClient(): CodeBuildAsyncClient {
        return CodeBuildAsyncClient.builder()
            .credentialsProvider(StaticCredentialsProvider.create(authenticationService.userAwsSessionCredentials))
            .build()
    }

    private fun createCodeBuildCiProject(project: Project) {
        val response = codeBuildAsyncClient.createProject(
            buildCreateProjectRequest(
                project = project,
                codeBuildType = CodeBuildType.CI,
                serviceRoleName = "xtages-codebuild-ci-role",
            )
        ).get()
        project.codebuildCiProjectArn = response.project().arn()
        createNotificationRule(project.codeBuildCiNotificationRuleName, response.project().arn())
        projectDao.merge(project)
    }

    private fun createCodeBuildCdProject(project: Project) {
        val response = codeBuildAsyncClient.createProject(
            buildCreateProjectRequest(
                project = project,
                codeBuildType = CodeBuildType.CD,
                privilegedMode = true,
                serviceRoleName = "xtages-codebuild-cd-role",
                concurrentBuildLimit = 2,
            )
        ).get()
        project.codebuildCdProjectArn = response.project().arn()
        createNotificationRule(project.codeBuildCdNotificationRuleName, response.project().arn())
        projectDao.merge(project)
    }

    private fun buildCreateProjectRequest(
        project: Project,
        codeBuildType: CodeBuildType,
        privilegedMode: Boolean = false,
        serviceRoleName: String,
        concurrentBuildLimit: Int? = null,
    ): CreateProjectRequest {
        fun <T> buildTypeVar(ciVar: T, cdVar: T) = if (codeBuildType == CodeBuildType.CI) ciVar else cdVar
        val imageName = buildTypeVar(project.codeBuildCiImageName, project.codeBuildCdImageName)
        val projectName = buildTypeVar(project.codeBuildCiProjectName, project.codeBuildCdProjectName)
        val projectDesc = buildTypeVar(project.codeBuildCiProjectDescription, project.codeBuildCdProjectDescription)
        val logsGroupName = buildTypeVar(project.codeBuildCiLogsGroupName, project.codeBuildCdLogsGroupName)
        val logsStreamName = buildTypeVar(project.codeBuildCiLogsStreamName, project.codeBuildCdLogsStreamName)
        val buildSpecLocation = buildTypeVar(project.codeBuildCiBuildSpecName, project.codeBuildCdBuildSpecName)
        val builder = CreateProjectRequest.builder()
            .name(projectName)
            .description(projectDesc)
            .serviceRole("${consoleProperties.aws.aimRoleArnPrefix}/${serviceRoleName}")
            .source(
                ProjectSource.builder()
                    .type(SourceType.NO_SOURCE)
                    .buildspec("${consoleProperties.aws.codeBuild.buildSpecsS3BucketArn}/${buildSpecLocation}")
                    .build()
            )
            .artifacts(
                ProjectArtifacts.builder()
                    .type(ArtifactsType.NO_ARTIFACTS)
                    .build()
            )
            .environment(
                ProjectEnvironment.builder()
                    .image("${consoleProperties.aws.codeBuild.ecrRepository}/${imageName}")
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
            .tags(xtagesCodeBuildTag, buildCodeBuildProjectTag(key = "organization", value = project.organization!!))
            .badgeEnabled(false)
        // TODO(czuniga): Uncomment when we figure out what's up with the account's limit
//        if (concurrentBuildLimit != null) {
//            builder.concurrentBuildLimit(concurrentBuildLimit)
//        }
        return builder.build()
    }

    private fun createNotificationRule(notificationRuleName: String, projectArn: String) {
        codestarNotificationsAsyncClient.createNotificationRule(
            CreateNotificationRuleRequest.builder()
                .name(notificationRuleName)
                .resource(projectArn)
                .detailType(DetailType.FULL)
                .eventTypeIds(
                    "codebuild-project-build-state-failed",
                    "codebuild-project-build-state-succeeded",
                    "codebuild-project-build-state-in-progress",
                    "codebuild-project-build-state-stopped",
                    "codebuild-project-build-phase-failure",
                    "codebuild-project-build-phase-success",
                )
                .targets(
                    Target.builder()
                        .targetAddress(consoleProperties.aws.codeBuild.buildEventsSnsTopicArn)
                        .targetType("SNS")
                        .build()
                )
                .status(NotificationRuleStatus.ENABLED)
                .build()
        ).get()
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

private fun buildEnvironmentVariable(name: String, value: String? = null) = EnvironmentVariable.builder()
    .type(EnvironmentVariableType.PLAINTEXT)
    .name(name)
    .value(value ?: "FILL_ME")
    .build()

private fun buildCodeBuildProjectTag(key: String, value: String) =
    CodebuildTag.builder().key(key).value(value).build()


data class CodeBuildEvent(
    val accountId: String,
    val region: String,
    val detailType: DetailType,
    val source: String,
    val version: String,
    val time: Instant,
    val id: String,
    val resources: List<String>,
    val detail: CodeBuildEventDetail,
)

data class CodeBuildEventDetail(
    val buildStatus: String,
    val projectName: String,
    val buildId: String,
    val additionalInformation: CodeBuildAdditionalInformation,
    val currentPhase: String,
    val currentPhaseContext: String,
    val version: String,
    val completedPhaseStatus: String,
    val completedPhase: String,
    val completedPhaseContext: String,
    val completedPhaseStart: Instant,
    val completedPhaseEnd: Instant,
)

data class CodeBuildAdditionalInformation(
    val artifact: BuildArtifacts,
    val environment: ProjectEnvironment,
    val timeout: Duration,
    val buildComplete: Boolean,
    val initiator: String,
    val buildStartTime: Instant,
    val source: ProjectSource,
    val logs: CodeBuildLogs,
    val phases: List<BuildPhase>,
)

data class CodeBuildLogs(
    val groupName: String,
    val streamName: String,
    val deepLink: String,
)
