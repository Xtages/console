package xtages.console.service

import com.amazonaws.arn.Arn
import com.amazonaws.services.sns.message.SnsMessageManager
import com.amazonaws.services.sns.message.SnsNotification
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.awspring.cloud.autoconfigure.context.properties.AwsRegionProperties
import io.awspring.cloud.core.naming.AmazonResourceName
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy
import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent
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
import xtages.console.controller.api.model.LogEvent
import xtages.console.controller.model.CodeBuildType
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

private val xtagesEcrTag: EcrTag = buildEcrTag(key = "XTAGES_CONSOLE_CREATED", value = "true")
private val xtagesCodeBuildTag = buildCodeBuildProjectTag(key = "XTAGES_CONSOLE_CREATED", value = "true")

private val envVars = listOf(
    buildEnvironmentVariable("XTAGES_COMMIT"),
    buildEnvironmentVariable("XTAGES_REPO"),
    buildEnvironmentVariable("XTAGES_GITHUB_TOKEN"),
)

private val logger = KotlinLogging.logger { }

@Service
class AwsService(
    private val gitHubService: GitHubService,
    private val ecrAsyncClient: EcrAsyncClient,
    private val cloudWatchLogsAsyncClient: CloudWatchLogsAsyncClient,
    private val codeBuildAsyncClient: CodeBuildAsyncClient,
    private val codestarNotificationsAsyncClient: CodestarNotificationsAsyncClient,
    private val organizationDao: OrganizationDao,
    private val projectDao: ProjectDao,
    private val buildEventDao: BuildEventDao,
    private val awsRegionProperties: AwsRegionProperties,
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
     *   * Cloud Watch Log groups for the [organization] if they haven't been created already
     *   * a CodeBuild project for CI
     *   * a CodeBuild project for CD
     */
    fun registerProject(project: Project, organization: Organization) {
        registerOrganization(organization)
        createCodeBuildCiProject(organization = organization, project = project)
        createCodeBuildCdProject(organization = organization, project = project)
    }

    private fun registerOrganization(organization: Organization) {
        maybeCreateEcrRepositoryForOrganization(organization = organization)
        maybeCreateLogGroupForOrganization(organization = organization)
    }

    fun unregisterProject(project: Project, organization: Organization) {
        TODO("(czuniga): Use the list below to implement this")
        // unregisterOrganization(organization = organization)
        // Remove CI CodeBuild Project
        //   * Remove Notification Rule (use project.codebuildCiNotificationRuleArn)
        //   * Remove Project  (use project.codebuildCiProjectArn)
        // Remove CD CodeBuild Project
        //   * Remove Notification Rule (use project.codebuildCdNotificationRuleArn)
        //   * Remove Project  (use project.codebuildCdProjectArn)
    }

    private fun unregisterOrganization(organization: Organization) {
        TODO("(czuniga): Use the list below to implement this")
        // Remove ECR repository
        // Remove CI log group
        // Remove CD log group
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

    /**
     * This function retrieve the logs from CloudWatch.
     * The log group name is build using the name of the [Organization] and the type of run from [CodeBuildType]
     * The log stream name is build using the [Project], [BuildEvent.buildArn] and [CodeBuildType]
     * This method is currently not paginated and relying in the 10k (1MB) events that returns
     * TODO(mdellamerlina): Fast-follow add pagination for this method
     */
    fun getLogsFor(codeBuildType: CodeBuildType, buildEvent: BuildEvent, project: Project, organization: Organization, ) : List<LogEvent> {
        val logGroupName = organization.codeBuildLogsGroupNameFor(codeBuildType)
        val logStreamName = buildLogStreamName(project, codeBuildType, buildEvent)
        logger.info { "logGroupName: $logGroupName logStreamName: $logStreamName" }
        val logEventRequest = GetLogEventsRequest.builder()
            .logGroupName(logGroupName)
            .logStreamName(logStreamName)
            .startFromHead(true)
            .build()
        val logEvents = cloudWatchLogsAsyncClient.getLogEvents(logEventRequest).get().events()
        return logEvents.map { it -> it.toLogEvent() }
    }

    private fun buildLogStreamName(
        project: Project,
        codeBuildType: CodeBuildType,
        buildEvent: BuildEvent
    ) =
        "${project.codeBuildLogsStreamNameFor(codeBuildType)}/${AmazonResourceName.fromString(buildEvent.buildArn).resourceName}"

    private fun OutputLogEvent.toLogEvent() = LogEvent(
        message = message(),
        timestamp = timestamp(),
    )

    private fun userSessionCodeBuildClient(): CodeBuildAsyncClient {
        return CodeBuildAsyncClient.builder()
            .credentialsProvider(StaticCredentialsProvider.create(authenticationService.userAwsSessionCredentials))
            .build()
    }

    private fun createCodeBuildCiProject(organization: Organization, project: Project) {
        val response = codeBuildAsyncClient.createProject(
            buildCreateProjectRequest(
                organization = organization,
                project = project,
                codeBuildType = CodeBuildType.CI,
                serviceRoleName = "xtages-codebuild-ci-role",
            )
        ).get()
        val arn = response.project().arn()
        project.codebuildCiProjectArn = arn
        val organizationName = ensure.notNull(project.organization, valueDesc = "project.organization")
        project.codebuildCiNotificationRuleArn = createNotificationRule(
            notificationRuleName = project.codeBuildCiNotificationRuleName,
            projectArn = arn,
            organizationName = organizationName
        )
        projectDao.merge(project)
    }

    private fun createCodeBuildCdProject(organization: Organization, project: Project) {
        val response = codeBuildAsyncClient.createProject(
            buildCreateProjectRequest(
                organization = organization,
                project = project,
                codeBuildType = CodeBuildType.CD,
                privilegedMode = true,
                serviceRoleName = "xtages-codebuild-cd-role",
                concurrentBuildLimit = 2,
            )
        ).get()
        val arn = response.project().arn()
        project.codebuildCdProjectArn = arn
        val organizationName = ensure.notNull(project.organization, valueDesc = "project.organization")
        project.codebuildCdNotificationRuleArn = createNotificationRule(
            notificationRuleName = project.codeBuildCdNotificationRuleName,
            projectArn = arn,
            organizationName = organizationName
        )
        projectDao.merge(project)
    }

    private fun buildCreateProjectRequest(
        organization: Organization,
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
        val logsGroupName = buildTypeVar(
            organization.codeBuildLogsGroupNameFor(CodeBuildType.CI),
            organization.codeBuildLogsGroupNameFor(CodeBuildType.CD)
        )
        val logsStreamName = buildTypeVar(
            project.codeBuildLogsStreamNameFor(CodeBuildType.CI),
            project.codeBuildLogsStreamNameFor(CodeBuildType.CD))
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

    private fun createNotificationRule(
        notificationRuleName: String,
        projectArn: String,
        organizationName: String
    ): String {
        return codestarNotificationsAsyncClient.createNotificationRule(
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
                .tags(mapOf("organization" to organizationName))
                .build()
        ).get().arn()
    }

    private fun maybeCreateEcrRepositoryForOrganization(organization: Organization) {
        if (organization.ecrRepositoryArn == null) {
            val name = ensure.notNull(value = organization.name, valueDesc = "organization.name")
            val createRepositoryResponse = ecrAsyncClient.createRepository(
                CreateRepositoryRequest.builder()
                    .repositoryName(name.toLowerCase())
                    .imageTagMutability(ImageTagMutability.IMMUTABLE)
                    .tags(xtagesEcrTag, buildEcrTag("organization", name))
                    .build()
            ).get()
            organization.ecrRepositoryArn = createRepositoryResponse.repository().repositoryArn()
            organizationDao.merge(organization)
        }
    }

    private fun maybeCreateLogGroupForOrganization(organization: Organization) {
        if (organization.cdLogGroupArn == null || organization.ciLogGroupArn == null) {
            val name = ensure.notNull(value = organization.name, valueDesc = "organization.name")
            cloudWatchLogsAsyncClient.createLogGroup(
                CreateLogGroupRequest.builder()
                    .logGroupName(organization.codeBuildLogsGroupNameFor(CodeBuildType.CI))
                    .tags(mapOf("organization" to name))
                    .build()
            ).get()
            cloudWatchLogsAsyncClient.createLogGroup(
                CreateLogGroupRequest.builder()
                    .logGroupName(organization.codeBuildLogsGroupNameFor(CodeBuildType.CD))
                    .tags(mapOf("organization" to name))
                    .build()
            ).get()
            // For some reason the `createLogGroup` call doesn't return anything in it's response, so we have to create the
            // ARNs for the LogGroups by hand.
            val cdLogGroupArn = Arn.builder()
                .withPartition("aws")
                .withService("logs")
                .withRegion(awsRegionProperties.static)
                .withAccountId(consoleProperties.aws.accountId)
                .withResource("log-group:${organization.cdLogGroupArn}")
                .build()
            organization.cdLogGroupArn = cdLogGroupArn.toString()
            val ciLogGroupArn = cdLogGroupArn.toBuilder()
                .withResource("log-group:${organization.ciLogGroupArn}")
                .build()
            organization.ciLogGroupArn = ciLogGroupArn.toString()
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

private fun buildEcrTag(key: String, value: String) = EcrTag.builder().key(key).value(value).build()

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
