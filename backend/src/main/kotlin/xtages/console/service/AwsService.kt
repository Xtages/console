package xtages.console.service

import com.amazonaws.services.sns.message.SnsMessageManager
import com.amazonaws.services.sns.message.SnsNotification
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy
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
import xtages.console.dao.fetchOneByBuildArnAndNameAndStatus
import xtages.console.exception.ensure
import xtages.console.pojo.*
import xtages.console.query.tables.daos.BuildEventDao
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.pojos.BuildEvent
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import java.time.*
import java.time.format.DateTimeFormatter
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
    private val buildEventDao: BuildEventDao,
    private val consoleProperties: ConsoleProperties,
    private val authenticationService: AuthenticationService,
    private val objectMapper: ObjectMapper,
) {
    private val snsMessageManager = SnsMessageManager()

    @SqsListener("build-updates-queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    fun codeBuildEventListener(eventStr: String) {
        val notification = ensure.ofType<SnsNotification>(
            value = snsMessageManager.parseMessage(eventStr.byteInputStream()),
            valueDesc = "received message"
        )
        logger.info { "Processing SNS Notification id [${notification.messageId}]" }
        val event = objectMapper.readValue(notification.message, CodeBuildEvent::class.java)
        ensure.isEqual(event.account, expected = consoleProperties.aws.accountId, valueDesc = "event.account")
        ensure.isEqual(event.source, expected = "aws.codebuild", valueDesc = "event.source")

        // We only care about changes to the `STATUS_CHANGE`s and not `PHASE_CHANGE`s because
        // in `STATUS_CHANGE` events we can see phase changes.
        if (event.detailType == CodeBuildEventDetailType.CODE_BUILD_STATUS_CHANGE) {
            // Make sure that we only process the [CodeBuildEvent] once, even if it's delivered multiple times.
            if (buildEventDao.fetchByNotificationId(notification.messageId).isNotEmpty()) {
                logger.info { "SNS Notification id [${notification.messageId}] has already been processed, bailing out early" }
                return
            }

            val sentToBuildEvent = buildEventDao.fetchOneByBuildArnAndNameAndStatus(
                buildArn = event.detail.buildId,
                name = "SENT_TO_BUILD",
                status = "IN_PROGRESS"
            )

            logger.info { "Processing CodeBuildEvent with event.detail.currentPhase [${event.detail.currentPhase}] for build [${event.detail.buildId}]" }
            if (event.detail.currentPhase == "COMPLETED") {
                // When handling a "COMPLETED" build status we take all the phases from the event and persist them as
                // [BuildEvent]s.
                val notificationId = notification.messageId
                val phases = ensure.notNull(
                    value = event.detail.additionalInformation.phases,
                    valueDesc = "event.detail.additionalInformation.phases"
                )
                handleCompletedBuildStatusChange(
                    notificationId,
                    sentToBuildEvent,
                    phases,
                )
            } else {
                buildEventDao.insert(
                    BuildEvent(
                        notificationId = notification.messageId,
                        name = event.detail.currentPhase,
                        status = event.detail.buildStatus,
                        startTime = LocalDateTime.ofInstant(event.time, ZoneOffset.UTC),
                        endTime = LocalDateTime.ofInstant(event.time, ZoneOffset.UTC),
                        operation = sentToBuildEvent.operation,
                        user = sentToBuildEvent.user,
                        environment = sentToBuildEvent.environment,
                        projectId = sentToBuildEvent.projectId,
                        commit = sentToBuildEvent.commit,
                        buildArn = sentToBuildEvent.buildArn,
                    )
                )
            }
        } else {
            logger.debug { "Dropping CodeBuildEvent of detailType [${event.detailType}] for build [${event.detail.buildId}]" }
        }
    }

    private fun handleCompletedBuildStatusChange(
        notificationId: String,
        sentToBuildEvent: BuildEvent,
        phases: List<CodeBuildPhase>
    ) {
        // Sort the phases based on their end time, the last phase with `phase-type` == "COMPLETED" has a `null`
        // `end-time` order it last.
        val sortedPhases = phases.sortedWith { a, b ->
            when {
                a.endTime == null -> 1
                b.endTime == null -> -1
                else -> a.endTime.compareTo(b.endTime)
            }
        }
        ensure.isEqual(
            actual = sortedPhases.last().phaseType,
            expected = "COMPLETED",
            valueDesc = "event.detail.additionalInformation.phases.last.phaseType"
        )
        // For each `phase` we create a `BuildEvent`. The last `phase` (`phase-type` == "COMPLETED") will have a
        // `null` `end-time` so we default it to it's `start-time`.
        val buildEvents = sortedPhases.map { phase ->
            BuildEvent(
                notificationId = notificationId,
                name = phase.phaseType,
                status = if (phase.phaseType == "COMPLETED") "SUCCEEDED" else phase.phaseStatus,
                startTime = LocalDateTime.ofInstant(phase.startTime, ZoneOffset.UTC),
                endTime = LocalDateTime.ofInstant(phase.endTime ?: phase.startTime, ZoneOffset.UTC),
                message = phase.message,
                operation = sentToBuildEvent.operation,
                user = sentToBuildEvent.user,
                environment = sentToBuildEvent.environment,
                projectId = sentToBuildEvent.projectId,
                commit = sentToBuildEvent.commit,
                buildArn = sentToBuildEvent.buildArn,
            )
        }
        buildEventDao.insert(buildEvents)
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
    ): StartBuildResponse {
        logger.info { "running CodeBuild: $codeBuildType for project : ${project.name} commit: $commit organization: ${organization.name}" }
        val token = gitHubService.appToken(organization)
        val cbProjectName = if (codeBuildType == CodeBuildType.CI)
            project.codeBuildCiProjectName
        else
            project.codeBuildCdBuildSpecName

        val startBuildResponse = userSessionCodeBuildClient().startBuild(
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
        ).get()!!
        logger.info { "started CodeBuild project: $cbProjectName" }
        return startBuildResponse
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
            .serviceRole("${consoleProperties.aws.aimRoleArnPrefix}/$serviceRoleName")
            .source(
                ProjectSource.builder()
                    .type(SourceType.NO_SOURCE)
                    .buildspec("${consoleProperties.aws.codeBuild.buildSpecsS3BucketArn}/$buildSpecLocation")
                    .build()
            )
            .artifacts(
                ProjectArtifacts.builder()
                    .type(ArtifactsType.NO_ARTIFACTS)
                    .build()
            )
            .environment(
                ProjectEnvironment.builder()
                    .image("${consoleProperties.aws.codeBuild.ecrRepository}/$imageName")
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
        if (concurrentBuildLimit != null) {
            builder.concurrentBuildLimit(concurrentBuildLimit)
        }
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


private enum class CodeBuildEventDetailType(private val internalName: String) {
    CODE_BUILD_PHASE_CHANGE("CodeBuild Build Phase Change"),
    CODE_BUILD_STATUS_CHANGE("CodeBuild Build State Change");

    companion object {
        fun fromInternalName(internalName: String): CodeBuildEventDetailType {
            return values().single { it.internalName.equals(internalName, ignoreCase = true) }
        }
    }
}

private data class CodeBuildEvent(
    val account: String,
    val region: String,
    @JsonDeserialize(using = CodeBuildEventDetailTypeDeserializer::class)
    val detailType: CodeBuildEventDetailType,
    val source: String,
    val version: String?,
    val time: Instant,
    val id: String?,
    val resources: List<String>,
    val detail: CodeBuildEventDetail,
)

@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy::class)
private data class CodeBuildEventDetail(
    val buildStatus: String?,
    val projectName: String,
    val buildId: String,
    val additionalInformation: CodeBuildAdditionalInformation,
    val currentPhase: String?,
    val currentPhaseContext: String?,
    val version: String?,
    val completedPhaseStatus: String?,
    val completedPhase: String?,
    val completedPhaseContext: String?,
    @JsonDeserialize(using = TimeZonelessInstantDeserializer::class)
    val completedPhaseStart: Instant?,
    @JsonDeserialize(using = TimeZonelessInstantDeserializer::class)
    val completedPhaseEnd: Instant?,
)

@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy::class)
private data class CodeBuildAdditionalInformation(
    val timeout: Duration?,
    val buildComplete: Boolean,
    val initiator: String,
    @JsonDeserialize(using = TimeZonelessInstantDeserializer::class)
    val buildStartTime: Instant,
    val logs: CodeBuildLogs,
    val phases: List<CodeBuildPhase>?,
)

@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy::class)
private data class CodeBuildLogs(
    val groupName: String?,
    val streamName: String?,
    val deepLink: String,
)

@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy::class)
private data class CodeBuildPhase(
    val phaseContext: List<Any?>?,
    @JsonDeserialize(using = TimeZonelessInstantDeserializer::class)
    val startTime: Instant,
    @JsonDeserialize(using = TimeZonelessInstantDeserializer::class)
    val endTime: Instant?,
    val durationInSeconds: Duration?,
    val phaseType: String,
    val phaseStatus: String?,
) {
    private val context = phaseContext ?: emptyList()
    val message = if (phaseStatus == "FAILED") context.joinToString(separator = "\n") { "${it ?: ""}" } else null
}

/**
 * An [StdDeserializer] for [Instant] that assumes that the source string is formatted using `MMM dd, yyyy h:mm:ss a`
 * with the UTC time zone.
 */
private object TimeZonelessInstantDeserializer : StdDeserializer<Instant>(Instant::class.java) {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm:ss a")
        .withZone(ZoneId.of("UTC"))

    override fun deserialize(parser: JsonParser, context: DeserializationContext): Instant {
        return Instant.from(dateTimeFormatter.parse(parser.text.trim()))
    }
}

private object CodeBuildEventDetailTypeDeserializer :
    StdDeserializer<CodeBuildEventDetailType>(CodeBuildEventDetailType::class.java) {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): CodeBuildEventDetailType {
        return CodeBuildEventDetailType.fromInternalName(parser.text.trim())
    }
}
