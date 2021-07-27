package xtages.console.service

import com.amazonaws.services.sns.message.SnsMessageManager
import com.amazonaws.services.sns.message.SnsNotification
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy
import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.stereotype.Service
import xtages.console.controller.model.Environment
import xtages.console.controller.model.Environment.PRODUCTION
import xtages.console.controller.model.Environment.STAGING
import xtages.console.dao.fetchLatestByProjectAndEnvironment
import xtages.console.dao.fetchLatestDeploymentStatus
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.enums.DeployStatus
import xtages.console.query.tables.daos.BuildDao
import xtages.console.query.tables.daos.ProjectDeploymentDao
import xtages.console.query.tables.pojos.Build
import xtages.console.query.tables.pojos.ProjectDeployment
import java.time.Instant


private val logger = KotlinLogging.logger { }

@Service
class EcsEventsService(
    private val objectMapper: ObjectMapper,
    private val projectDeploymentDao: ProjectDeploymentDao,
    private val buildDao: BuildDao,
) {
    private val snsMessageManager = SnsMessageManager()

    /**
     * Listen to ECS events that resulted in steady state. Depending on the previous [DeployStatus] in the [Project]
     * the [DeployStatus] will be updated:
     * If the previous [DeployStatus] was [DeployStatus.PROVISIONING] then it will transition to [DeployStatus.DEPLOYED]
     * If the previous [DeployStatus] was [DeployStatus.DRAINING] then it will transition to [DeployStatus.DRAINED]
     * https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs_cwe_events.html
     */
    @SqsListener("ecs-steady-state-queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    fun ecsSteadyStateEvent(eventStr: String) {
        logger.info { "Steady State msg received" }
        val notification = checkNotification(eventStr)
        val event = objectMapper.readValue(notification.message, EcsEvent::class.java)

        val latestProjectDeployment = projectDeploymentDao.fetchLatestDeploymentStatus(
            projectHash = event.serviceName(),
            environment = event.environment(),
        )

        if (latestProjectDeployment.status != DeployStatus.DRAINED && latestProjectDeployment.status != DeployStatus.DEPLOYED) {
            val status =
                if (latestProjectDeployment.status == DeployStatus.PROVISIONING) DeployStatus.DEPLOYED else DeployStatus.DRAINED

            projectDeploymentDao.insert(
                ProjectDeployment(
                    projectId = latestProjectDeployment.projectId,
                    buildId = latestProjectDeployment.buildId,
                    status = status
                )
            )
        }

    }

    /**
     * Listen to events related to scale-in for staging. When an instance goes down after a period of time a new [ProjectDeployment]
     * is added to note that the project is in [DeployStatus.DRAINING] state
     */
    @SqsListener("scalein-staging-updates-queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    fun scaleInStagingEvent(eventStr: String) {
        logger.info { "Scale-in msg received" }
        val notification = checkNotification(eventStr)
        val event = objectMapper.readValue(notification.message, CloudTrailAutoScalingEvent::class.java)

        addProjectDeployment(
            serviceName = event.serviceName(),
            environment = event.environment(),
            deployStatus = DeployStatus.DRAINING
        )

    }

    /**
     * Listen to deploy events in ECS to mark the [Project]'s  [DeployStatus] with status [DeployStatus.PROVISIONING]
     */
    @SqsListener("deployment-updates-queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    fun deployCompletedEvent(eventStr: String) {
        logger.info { "Deploy updates msg received" }
        val notification = checkNotification(eventStr)
        val event = objectMapper.readValue(notification.message, CloudTrailDeployEvent::class.java)

        addProjectDeployment(
            serviceName = event.serviceName(),
            environment = event.environment(),
            deployStatus = DeployStatus.PROVISIONING
        )

    }

    private fun addProjectDeployment(serviceName: String, environment: Environment, deployStatus: DeployStatus) {
        val latestBuild = lookupLatestBuild(
            serviceName = serviceName,
            environment = environment,
        )

        projectDeploymentDao.insert(
            ProjectDeployment(
                projectId = latestBuild.projectId,
                buildId = latestBuild.id,
                status = deployStatus,
            )
        )
    }


    private fun checkNotification(eventStr: String): SnsNotification {
        val notification = ensure.ofType<SnsNotification>(
            value = snsMessageManager.parseMessage(eventStr.byteInputStream()),
            valueDesc = "received message"
        )
        logger.info { "Processing SNS Notification for ECS events with id [${notification.messageId}]" }
        logger.info { notification.message }
        return notification
    }

    private fun lookupLatestBuild(serviceName: String, environment: Environment): Build {
        return ensure.foundOne(
            operation = {
                buildDao.fetchLatestByProjectAndEnvironment(
                    projectHash = serviceName,
                    env = environment.name.toLowerCase(),
                )
            },
            code = ExceptionCode.BUILD_NOT_FOUND
        )
    }

}

private interface GenericEvent {
    fun environment(): Environment
    fun serviceName(): String
}

private abstract class CloudTrailEvent(
    open val detail: CloudTrailDetail,
) : GenericEvent {
    override fun environment(): Environment {
        return if (detail.requestParameters.cluster.contains(
                other = STAGING.name,
                ignoreCase = true
            )
        )
            STAGING
        else
            PRODUCTION
    }
}

private enum class ECS_OPERATION {
    UPDATESERVICE,
    CREATESERVICE,
}

private data class CloudTrailAutoScalingEvent(
    val account: String,
    val region: String,
    val time: Instant,
    val id: String?,
    override val detail: CloudTrailDetail,
) : CloudTrailEvent(
    detail = detail,
) {

    override fun serviceName(): String {
        return detail.requestParameters.service!!
    }
}

private data class CloudTrailDeployEvent(
    val account: String,
    val region: String,
    val time: Instant,
    val id: String?,
    override val detail: CloudTrailDetail
) : CloudTrailEvent(
    detail = detail,
) {

    override fun environment(): Environment {
        return if (detail.requestParameters.cluster.contains(
                other = STAGING.name,
                ignoreCase = true
            )
        )
            STAGING
        else
            PRODUCTION
    }

    override fun serviceName(): String {
        if (detail.eventName.toUpperCase() == ECS_OPERATION.UPDATESERVICE.name)
            return detail.requestParameters.service!!.substringAfterLast("/")
        else
            return detail.requestParameters.serviceName!!.substringAfterLast("/")
    }
}

private data class CloudTrailDetail(
    val eventName: String,
    val requestParameters: RequestParameters,
)

private data class RequestParameters(
    val cluster: String,
    val desiredCount: Int,
    val service: String?,
    val serviceName: String?,
)

private data class EcsEvent(
    val account: String,
    val region: String,
    val time: Instant,
    val id: String?,
    val resources: List<String>,
) : GenericEvent {
    override fun environment(): Environment {
        return if (resources.none {
                it.contains(
                    other = STAGING.name,
                    ignoreCase = true
                )
            })
            PRODUCTION
        else
            STAGING
    }

    override fun serviceName(): String {
        return resources.singleOrNull()?.substringAfterLast("/")!!
    }
}
