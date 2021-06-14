package xtages.console.service.aws

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
import io.awspring.cloud.core.naming.AmazonResourceName
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy
import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.codebuild.CodeBuildAsyncClient
import software.amazon.awssdk.services.codebuild.model.*
import xtages.console.config.ConsoleProperties
import xtages.console.controller.api.model.LogEvent
import xtages.console.controller.model.CodeBuildType
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ExceptionCode.INVALID_ENVIRONMENT
import xtages.console.exception.ensure
import xtages.console.pojo.*
import xtages.console.query.enums.BuildStatus
import xtages.console.query.enums.BuildType
import xtages.console.query.tables.daos.BuildDao
import xtages.console.query.tables.daos.BuildEventDao
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.pojos.*
import xtages.console.query.tables.pojos.Build
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.references.BUILD
import xtages.console.service.AuthenticationService
import xtages.console.time.toUtcLocalDateTime
import java.time.*
import java.time.format.DateTimeFormatter
import software.amazon.awssdk.services.codebuild.model.Tag as CodebuildTag

private val xtagesCodeBuildTag = buildCodeBuildProjectTag(key = "XTAGES_CONSOLE_CREATED", value = "true")

private val envVars = listOf(
    buildEnvironmentVariable("XTAGES_COMMIT"),
    buildEnvironmentVariable("XTAGES_REPO"),
    buildEnvironmentVariable("XTAGES_GITHUB_TOKEN"),
)

private val eventTypeIds = listOf(
    "codebuild-project-build-state-failed",
    "codebuild-project-build-state-succeeded",
    "codebuild-project-build-state-in-progress",
    "codebuild-project-build-state-stopped",
    "codebuild-project-build-phase-failure",
    "codebuild-project-build-phase-success",
)

private val logger = KotlinLogging.logger { }

/**
 * A [Service] to handle the logic to communicate to the AWS CodeBuild service. Also handles events sent to
 * `"build-updates-queue"`.
 */
@Service
class CodeBuildService(
    private val cloudWatchLogsService: CloudWatchLogsService,
    private val codeBuildAsyncClient: CodeBuildAsyncClient,
    private val codestarNotificationsService: CodestarNotificationsService,
    private val projectDao: ProjectDao,
    private val buildDao: BuildDao,
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

            val build = ensure.foundOne(
                operation = { buildDao.fetchOneByBuildArn(event.detail.buildId) },
                code = ExceptionCode.BUILD_NOT_FOUND,
                message = "Could not find build with ARN [${event.detail.buildId}]"
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
                    build,
                    phases,
                )
            } else {
                buildEventDao.insert(
                    BuildEvent(
                        notificationId = notification.messageId,
                        name = event.detail.currentPhase,
                        status = event.detail.buildStatus,
                        startTime = event.time.toUtcLocalDateTime(),
                        endTime = event.time.toUtcLocalDateTime(),
                        buildId = build.id,
                    )
                )
            }
        } else if (event.detailType == CodeBuildEventDetailType.CODE_BUILD_PHASE_CHANGE) {
            logger.debug { "Dropping CodeBuildEvent of detailType [${event.detailType}] for build [${event.detail.buildId}]" }
        }
    }

    private fun handleCompletedBuildStatusChange(
        notificationId: String,
        build: Build,
        phases: List<CodeBuildPhase>
    ) {
        // Sort the phases based on their end time, the last phase with `phase-type` == "COMPLETED" has a `null`
        // `end-time` order it last.
        val sortedPhases = phases.sortedWith { phaseA, phaseB ->
            when {
                phaseA.endTime == null -> 1
                phaseB.endTime == null -> -1
                else -> phaseA.endTime.compareTo(phaseB.endTime)
            }
        }
        val lastPhase = sortedPhases.last()
        ensure.isEqual(
            actual = lastPhase.phaseType,
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
                startTime = phase.startTime.toUtcLocalDateTime(),
                endTime = (phase.endTime ?: phase.startTime).toUtcLocalDateTime(),
                message = phase.message,
                buildId = build.id,
            )
        }
        buildEventDao.insert(buildEvents)

        val updatedBuild =
            build.copy(
                status = determineBuildStatus(buildEvents),
                endTime = (lastPhase.endTime ?: lastPhase.startTime).toUtcLocalDateTime()
            )
        buildDao.update(updatedBuild)
    }

    private fun determineBuildStatus(events: List<BuildEvent>) =
        when {
            events.any { event -> event.status == "FAILED" } -> BuildStatus.FAILED
            events.none { event -> event.name == "COMPLETED" } -> BuildStatus.IN_PROGRESS
            events.any { event -> event.name == "COMPLETED" && event.status == "SUCCEEDED" } -> BuildStatus.SUCCEEDED
            else -> BuildStatus.UNKNOWN
        }

    /**
     * Starts a CodeBuild project for a specific [Project]
     * codeBuildStarterRequest provides information about the [CodeBuildType] to run and
     * al the necessary information to make the build run
     */
    fun startCodeBuildProject(
        gitHubAppToken: String,
        user: XtagesUser? = null,
        githubUser: GithubUser? = null,
        project: Project,
        recipe: Recipe,
        organization: Organization,
        commitHash: String,
        codeBuildType: CodeBuildType,
        environment: String,
        fromGitHubApp: Boolean = false,
        gitHubProjectTag: String? = null,
        previousGitHubProjectTag: String? = null,
    ): Pair<StartBuildResponse, Build> {
        ensure.isTrue(
            environment in setOf("dev", "staging", "production"),
            INVALID_ENVIRONMENT,
            "[$environment] is not valid"
        )
        val build = Build(
            environment = environment,
            type = BuildType.valueOf(codeBuildType.name),
            status = BuildStatus.UNKNOWN,
            userId = user?.id,
            githubUserUsername = githubUser?.username,
            projectId = project.id,
            commitHash = commitHash,
            startTime = LocalDateTime.now(ZoneOffset.UTC),
            tag = previousGitHubProjectTag ?: gitHubProjectTag
        )
        val buildRecord = buildDao.ctx().newRecord(BUILD, build)
        buildRecord.store()
        logger.debug { "Build created with id: ${buildRecord.id}" }

        logger.info { "running CodeBuild: $codeBuildType for project : ${project.name} commit: $commitHash organization: ${organization.name}" }
        val cbProjectName = if (codeBuildType == CodeBuildType.CI)
            project.codeBuildCiProjectName
        else
            project.codeBuildCdProjectName

        val codeBuildClient = if (fromGitHubApp) codeBuildAsyncClient else userSessionCodeBuildClient()

        val scriptPath = getScriptPath(recipe, previousGitHubProjectTag, environment)

        val startBuildResponse = codeBuildClient.startBuild(
            StartBuildRequest.builder()
                .projectName(cbProjectName)
                .environmentVariablesOverride(
                    listOf(
                        buildEnvironmentVariable("XTAGES_SCRIPT", scriptPath),
                        buildEnvironmentVariable("XTAGES_COMMIT", commitHash),
                        buildEnvironmentVariable("XTAGES_REPO", project.ghRepoFullName),
                        buildEnvironmentVariable("XTAGES_PROJECT", project.hash),
                        buildEnvironmentVariable("XTAGES_GITHUB_TOKEN", gitHubAppToken),
                        buildEnvironmentVariable("XTAGES_GH_PROJECT_TAG", gitHubProjectTag),
                        buildEnvironmentVariable("XTAGES_APP_ENV", environment.toLowerCase()),
                        buildEnvironmentVariable("XTAGES_PROJECT_TYPE", recipe.projectType?.name!!.toLowerCase()),
                        buildEnvironmentVariable("XTAGES_ORG", organization.name),
                        buildEnvironmentVariable("XTAGES_ORG_HASH", organization.hash),
                        buildEnvironmentVariable("XTAGES_RECIPE_REPO", recipe.repository),
                        buildEnvironmentVariable("XTAGES_GH_RECIPE_TAG", recipe.tag),
                        buildEnvironmentVariable("XTAGES_NODE_VER", recipe.version),
                        buildEnvironmentVariable("XTAGES_PREVIOUS_GH_PROJECT_TAG", previousGitHubProjectTag)
                    )
                )
                .build()
        ).get()

        val buildArn = startBuildResponse.build().arn()
        logger.debug { "StartBuildResponse Object: $buildArn" }
        buildRecord.buildArn = buildArn
        buildRecord.status = BuildStatus.IN_PROGRESS
        buildRecord.update()

        logger.info { "started CodeBuild project: $cbProjectName" }
        return Pair(startBuildResponse, buildRecord.into(Build::class.java))
    }

    private fun getScriptPath(
        recipe: Recipe,
        previousGitHubProjectTag: String?,
        environment: String
    ): String {
        val scriptPath = when {
            previousGitHubProjectTag != null -> recipe.rollbackScriptPath
            environment == "staging" -> recipe.deployScriptPath
            environment == "production" -> recipe.promoteScriptPath
            environment == "dev" -> recipe.buildScriptPath
            else -> null
        }
        return ensure.notNull(value = scriptPath, valueDesc = "scriptPath")
    }

    /**
     * This function retrieve the logs from CloudWatch.
     * The log group name is build using the name of the [Organization] and the type of run from [CodeBuildType]
     * The log stream name is build using the [Project], [Build.buildArn] and [CodeBuildType]
     * This method is currently not paginated and relying in the 10k (1MB) events that returns
     * TODO(mdellamerlina): Fast-follow add pagination for this method
     */
    fun getLogsFor(
        codeBuildType: CodeBuildType,
        build: Build,
        project: Project,
        organization: Organization,
    ): List<LogEvent> {
        val logGroupName = organization.codeBuildLogsGroupNameFor(codeBuildType)
        val logStreamName =
            "${project.codeBuildLogsStreamNameFor(codeBuildType)}/${AmazonResourceName.fromString(build.buildArn).resourceName}"
        return cloudWatchLogsService.getLogs(logGroupName = logGroupName, logStreamName = logStreamName)
    }

    private fun userSessionCodeBuildClient(): CodeBuildAsyncClient {
        return CodeBuildAsyncClient.builder()
            .credentialsProvider(StaticCredentialsProvider.create(authenticationService.userAwsSessionCredentials))
            .build()
    }

    /**
     * Creates a new `CodeBuild` [BuildType.CI] project for [organization] and [project].
     */
    fun createCodeBuildCiProject(organization: Organization, project: Project, recipe: Recipe) {
        val response = codeBuildAsyncClient.createProject(
            buildCreateProjectRequest(
                organization = organization,
                project = project,
                recipe = recipe,
                codeBuildType = CodeBuildType.CI,
                serviceRoleName = "xtages-codebuild-ci-role",
            )
        ).get()
        val arn = response.project().arn()
        project.codebuildCiProjectArn = arn
        project.codebuildCiNotificationRuleArn = createCodeStarNotification(
            organization = organization,
            notificationName =  project.codeBuildCiNotificationRuleName,
            arn = arn
        )
        projectDao.merge(project)
    }

    /**
     * Creates a new `CodeBuild` [BuildType.CD] project for [organization] and [project].
     */
    fun createCodeBuildCdProject(organization: Organization, project: Project, recipe: Recipe) {
        val response = codeBuildAsyncClient.createProject(
            buildCreateProjectRequest(
                organization = organization,
                project = project,
                recipe = recipe,
                codeBuildType = CodeBuildType.CD,
                privilegedMode = true,
                serviceRoleName = "xtages-codebuild-cd-role",
                concurrentBuildLimit = 2,
            )
        ).get()

        val arn = response.project().arn()
        project.codebuildCdProjectArn = arn
        project.codebuildCdNotificationRuleArn = createCodeStarNotification(
            organization = organization,
            notificationName = project.codeBuildCdNotificationRuleName,
            arn = arn
        )
        projectDao.merge(project)
    }

    private fun buildCreateProjectRequest(
        organization: Organization,
        project: Project,
        recipe: Recipe,
        codeBuildType: CodeBuildType,
        privilegedMode: Boolean = false,
        serviceRoleName: String,
        concurrentBuildLimit: Int? = null,
    ): CreateProjectRequest {
        fun <T> buildTypeVar(ciVar: T, cdVar: T) = if (codeBuildType == CodeBuildType.CI) ciVar else cdVar
        val imageName = buildTypeVar(recipe.codeBuildCiImageName, recipe.codeBuildCdImageName)
        val projectName = buildTypeVar(project.codeBuildCiProjectName, project.codeBuildCdProjectName)
        val projectDesc = buildTypeVar(project.codeBuildCiProjectDescription, project.codeBuildCdProjectDescription)
        val logsGroupName = organization.codeBuildLogsGroupNameFor(codeBuildType)
        val logsStreamName = project.codeBuildLogsStreamNameFor(codeBuildType)
        val buildSpecLocation = buildTypeVar(recipe.codeBuildCiBuildSpecName, recipe.codeBuildCdBuildSpecName)
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
            .tags(
                xtagesCodeBuildTag,
                buildCodeBuildProjectTag(key = "organization", value = project.organization!!),
                buildCodeBuildProjectTag(key = "organization-hash", value = organization.hash!!)
            )
            .badgeEnabled(false)
        if (concurrentBuildLimit != null) {
            builder.concurrentBuildLimit(concurrentBuildLimit)
        }
        return builder.build()
    }

    private fun createCodeStarNotification(organization: Organization, notificationName: String, arn: String): String {
        return codestarNotificationsService.createNotificationRule(
            notificationRuleName = notificationName,
            projectArn = arn,
            organization = organization,
            eventTypeIds = eventTypeIds
        )
    }
}

private fun buildEnvironmentVariable(name: String, value: String? = null) = EnvironmentVariable.builder()
    .type(EnvironmentVariableType.PLAINTEXT)
    .name(name)
    .value(value ?: "FILL_ME")
    .build()

private fun buildCodeBuildProjectTag(key: String, value: String) =
    CodebuildTag.builder().key(key).value(value).build()


private enum class CodeBuildEventDetailType(
    private
    val internalName: String
) {
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

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm:ss a")
        .withZone(ZoneId.of("UTC"))

    override fun deserialize(parser: JsonParser, context: DeserializationContext): Instant {
        return Instant.from(dateTimeFormatter.parse(parser.text.trim()))
    }
}

/**
 * [StdDeserializer] for [CodeBuildEventDetailType].
 */
private object CodeBuildEventDetailTypeDeserializer :
    StdDeserializer<CodeBuildEventDetailType>(CodeBuildEventDetailType::class.java) {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): CodeBuildEventDetailType {
        return CodeBuildEventDetailType.fromInternalName(parser.text.trim())
    }
}
