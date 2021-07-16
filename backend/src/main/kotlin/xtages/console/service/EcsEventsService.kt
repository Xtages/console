package xtages.console.service

import com.amazonaws.services.sns.message.SnsMessageManager
import com.amazonaws.services.sns.message.SnsNotification
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy
import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.stereotype.Service
import xtages.console.config.ConsoleProperties
import xtages.console.exception.ensure
import xtages.console.query.enums.BuildStatus
import xtages.console.query.enums.BuildType
import xtages.console.query.tables.daos.BuildDao
import xtages.console.query.tables.pojos.Build
import xtages.console.time.toUtcLocalDateTime
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger { }

@Service
class EscEventsService(
    private val objectMapper: ObjectMapper,
    private val buildDao: BuildDao,
) {

    private val snsMessageManager = SnsMessageManager()

    @SqsListener("scalein-staging-updates-queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    fun scaleInStagingEvent(eventStr: String) {
        val notification = ensure.ofType<SnsNotification>(
            value = snsMessageManager.parseMessage(eventStr.byteInputStream()),
            valueDesc = "received message"
        )
        logger.info { "Processing SNS Notification for Scale-in in Staging with id [${notification.messageId}]" }
        logger.info { notification.message }

        val event = objectMapper.readValue(notification.message, CloudTrailEvent::class.java)
        val buildIds = listOf(event.detail.requestParameters.service.substringAfterLast("-", "").toLong())
        insertBuildWith(
            buildIds = buildIds,
            buildStatus = BuildStatus.UNDEPLOYED,
        )
    }

    @SqsListener("deployment-updates-queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    fun deployCompletedEvent(eventStr: String) {
        val notification = ensure.ofType<SnsNotification>(
            value = snsMessageManager.parseMessage(eventStr.byteInputStream()),
            valueDesc = "received message"
        )
        logger.info { "Processing SNS Notification for deployment updates with id [${notification.messageId}]" }
        logger.info { notification.message }

        val event = objectMapper.readValue(notification.message, EcsEvent::class.java)
        val buildIds = event.resources.map { it.substringAfterLast("-", "").toLong() }
        insertBuildWith(
            buildIds = buildIds,
            buildStatus = BuildStatus.DEPLOYED,
        )
    }

    /**
     * Adds a [Build] to the DB with the [BuildStatus.DEPLOYED] if and only if
     * the previous build was a [BuildStatus.SUCCEEDED]
     */
    private fun insertBuildWith(buildIds: List<Long>, buildStatus: BuildStatus) {
        val builds = buildDao.fetchById(*buildIds.toLongArray())
        builds.forEach {
            val build = Build(
                projectId = it.projectId!!,
                type = BuildType.CD,
                environment = it.environment,
                status = buildStatus,
                startTime = Instant.now().toUtcLocalDateTime(),
                endTime = Instant.now().toUtcLocalDateTime(),
                commitHash = it.commitHash
            )
            when (buildStatus) {
                BuildStatus.DEPLOYED -> {
                    if (it.status == BuildStatus.SUCCEEDED) buildDao.insert(build)
                }
                BuildStatus.UNDEPLOYED -> buildDao.insert(build)
                else -> {
                }
            }
        }
    }

//    /**
//     * Adds the undeployed status if the time of the event is between the duration that the deploys in staging last
//     * plus an error
//     */
//    private fun addUndeployedStatus(build: Build) {
//        val deployedBuild = buildDao.findLatestBuildWithStatusFor(build.projectId!!, BuildStatus.DEPLOYED)
//        val nowTime = Instant.now()
//        if (deployedBuild != null) {
//            val timeInStagingRunning = Duration.between(deployedBuild.startTime?.toUtcInstant(), nowTime).toMinutes()
//            if (timeInStagingRunning <= upperBoundDeployDuration && lowerBoundDeployDuration < timeInStagingRunning) {
//                buildDao.insert(build)
//            }
//        }
//    }
}

private data class CloudTrailEvent(
    val account: String,
    val region: String,
    val time: Instant,
    val id: String?,
    val detail: CloudTrailDetail,
)

private data class CloudTrailDetail(
    val eventName: String,
    val requestParameters: RequestParameters,
)

private data class RequestParameters(
    val cluster: String,
    val desiredCount: Int,
    val service: String,
)

private data class EcsEvent(
    val account: String,
    val region: String,
    val time: Instant,
    val id: String?,
    val resources: List<String>,
)
