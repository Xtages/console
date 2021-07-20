package xtages.console.service

import com.amazonaws.services.sns.message.SnsMessageManager
import com.amazonaws.services.sns.message.SnsNotification
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy
import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.stereotype.Service
import xtages.console.dao.fetchMostRecentStatusByProject
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.enums.BuildStatus
import xtages.console.query.enums.BuildType
import xtages.console.query.tables.daos.BuildDao
import xtages.console.query.tables.pojos.Build
import xtages.console.service.Environment.PRODUCTION
import xtages.console.service.Environment.STAGING
import xtages.console.time.toUtcLocalDateTime
import java.time.Instant

private val logger = KotlinLogging.logger { }

@Service
class EscEventsService(
    private val objectMapper: ObjectMapper,
    private val buildDao: BuildDao,
) {

    private val snsMessageManager = SnsMessageManager()

    /**
     * Listen to events related to scale-in for staging. When an instance goes down after a period of time a new [Build]
     * is added to note that the project is in [BuildStatus.UNDEPLOYED]
     */
    @SqsListener("scalein-staging-updates-queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    fun scaleInStagingEvent(eventStr: String) {
        val notification = ensure.ofType<SnsNotification>(
            value = snsMessageManager.parseMessage(eventStr.byteInputStream()),
            valueDesc = "received message"
        )
        logger.info { "Processing SNS Notification for Scale-in in Staging with id [${notification.messageId}]" }
        logger.info { notification.message }

        val event = EventFactory.createEvent(
            eventStr = notification.message,
            objectMapper = objectMapper,
        )
        val previousBuild = lookupPreviousBuild(
            serviceName = event.serviceName(),
            environment = event.environment(),
            buildStatus = BuildStatus.DEPLOYED,
        )
        val build = buildBuildObject(previousBuild, BuildStatus.UNDEPLOYED)
        buildDao.insert(build)
    }

    private fun lookupPreviousBuild(serviceName: String, environment: Environment, buildStatus: BuildStatus): Build {
        return ensure.foundOne(
            operation = {
                buildDao.fetchMostRecentStatusByProject(
                    projectHash = serviceName,
                    env = environment.name.toLowerCase(),
                    status = buildStatus,
                )
            },
            code = ExceptionCode.BUILD_NOT_FOUND
        )
    }

    /**
     * Listen to deploy events in ECS to mark the project with a new [Build] with status [BuildStatus.DEPLOYED]
     */
    @SqsListener("deployment-updates-queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    fun deployCompletedEvent(eventStr: String) {
        val notification = ensure.ofType<SnsNotification>(
            value = snsMessageManager.parseMessage(eventStr.byteInputStream()),
            valueDesc = "received message"
        )
        logger.info { "Processing SNS Notification for deployment updates with id [${notification.messageId}]" }
        logger.info { notification.message }

        val event = EventFactory.createEvent(
            eventStr = notification.message,
            objectMapper = objectMapper,
            deployment_update = true,
        )

        val previousBuild = lookupPreviousBuild(
            serviceName = event.serviceName(),
            environment = event.environment(),
            buildStatus = BuildStatus.SUCCEEDED
        )

        val build = buildBuildObject(previousBuild, BuildStatus.DEPLOYED)
        buildDao.insert(build)
    }

    private fun buildBuildObject(build: Build, buildStatus: BuildStatus): Build {
        return  Build(
            projectId = build.projectId!!,
            type = BuildType.CD,
            environment = build.environment,
            status = buildStatus,
            startTime = Instant.now().toUtcLocalDateTime(),
            endTime = Instant.now().toUtcLocalDateTime(),
            commitHash = build.commitHash,
            githubUserUsername = build.githubUserUsername,
            userId = build.userId,
            tag = build.tag,
        )
    }

}

enum class Environment {
    STAGING,
    PRODUCTION
}

private class EventFactory{

    companion object {
        fun createEvent(
            eventStr: String,
            objectMapper: ObjectMapper,
            deployment_update: Boolean = false
        ): GenericEvent {
            return when {
                eventStr.contains("ECS Deployment State Change") -> objectMapper.readValue(eventStr, EcsEvent::class.java)
                deployment_update -> objectMapper.readValue(eventStr, CloudTrailDeployEvent::class.java)
                else -> objectMapper.readValue(eventStr, CloudTrailAutoScalingEvent::class.java)
            }
        }
    }
}

private abstract class GenericEvent {
    abstract fun environment(): Environment
    abstract fun serviceName(): String
}

private open class CloudTrailAutoScalingEvent(
    open val account: String,
    open val region: String,
    open val time: Instant,
    open val id: String?,
    open val detail: CloudTrailDetail,
) : GenericEvent() {
    override fun environment(): Environment {
        return if (detail.requestParameters.cluster.contains(STAGING.name,true))
            STAGING
        else
            PRODUCTION
    }

    override fun serviceName(): String {
        return detail.requestParameters.service
    }
}

private data class CloudTrailDeployEvent(
    override val account: String,
    override val region: String,
    override val time: Instant,
    override val id: String?,
    override val detail: CloudTrailDetail
) : CloudTrailAutoScalingEvent(
    account, region, time, id, detail
) {
    override fun serviceName(): String {
        return detail.requestParameters.service.substringAfterLast("/")
    }
}

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
) : GenericEvent() {
    override fun environment(): Environment {
        return if (resources.none { it.contains(STAGING.name, true) })
            PRODUCTION
        else
            STAGING
    }

    override fun serviceName(): String {
        return resources.singleOrNull()?.substringAfterLast("/")!!
    }
}
