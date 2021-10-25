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
import xtages.console.dao.fetchLatestDeploymentStatus
import xtages.console.dao.insertIfNotExist
import xtages.console.exception.ExceptionCode.PROJECT_NOT_FOUND
import xtages.console.exception.UnknownProjectDeploymentStatus
import xtages.console.exception.ensure
import xtages.console.pojo.buildId
import xtages.console.query.enums.DeployStatus
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.daos.ProjectDeploymentDao
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.pojos.ProjectDeployment
import xtages.console.service.aws.EcsService
import java.time.Instant
import software.amazon.awssdk.services.ecs.model.Service as AwsEcsService


private val logger = KotlinLogging.logger { }

/***
 * This service receives notifications from ECS related to scale-in, steady-state and deploy/re-deploy events
 * for customers clusters.
 *
 * The way we decided the [DeployStatus] is by making a call to ECS to see the status of the service and based on the `desired`
 * and `running` tasks we decide the status.
 */

@Service
class EcsEventsService(
    private val objectMapper: ObjectMapper,
    private val projectDeploymentDao: ProjectDeploymentDao,
    private val ecsService: EcsService,
    private val projectDao: ProjectDao,
) {
    private val snsMessageManager = SnsMessageManager()

    /**
     * Listen to ECS events that resulted in steady state and adds a [ProjectDeployment] record with the status
     * based on the ECS service status.
     * https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs_cwe_events.html
     */
    @SqsListener("ecs-steady-state-queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    fun ecsSteadyStateEvent(eventStr: String) {
        logger.info { "Steady State msg received" }
        val notification = checkNotification(eventStr)
        val event = objectMapper.readValue(notification.message, EcsEvent::class.java)

        val service = ecsService.describeService(event.serviceName(), event.clusterName())
        val deployStatus = decideDeployStatus(service)
        val (buildId, project) = getBuildIdAndProject(event, service)

        // search for the last project deployment in case there is no change in the status
        val latestProjectDeployment = projectDeploymentDao.fetchLatestDeploymentStatus(event.serviceName(), event.environment())
        if (latestProjectDeployment != null) {
            projectDeploymentDao.insertIfNotExist(
                ProjectDeployment(
                    projectId = project.id,
                    status = deployStatus,
                    buildId = buildId
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
        val service = ecsService.describeService(event.serviceName(), event.clusterName())
        val (buildId, project) = getBuildIdAndProject(event, service)

        projectDeploymentDao.insert(
            ProjectDeployment(
                projectId = project.id,
                status = DeployStatus.DRAINING,
                buildId = buildId
            )
        )

    }

    /**
     * Listen to deploy events in ECS to mark the Project's  [DeployStatus] with status [DeployStatus.PROVISIONING]
     */
    @SqsListener("deployment-updates-queue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    fun deployCompletedEvent(eventStr: String) {
        logger.info { "Deploy updates msg received" }
        val notification = checkNotification(eventStr)
        val event = objectMapper.readValue(notification.message, CloudTrailDeployEvent::class.java)
        val service = ecsService.describeService(event.serviceName(), event.clusterName())
        val (buildId, project) = getBuildIdAndProject(event, service)

        projectDeploymentDao.insert(
            ProjectDeployment(
                projectId = project.id,
                status = DeployStatus.PROVISIONING,
                buildId = buildId
            )
        )
    }

    private fun getBuildIdAndProject(event: GenericEvent, service: AwsEcsService?): Pair<Long?, Project> {
        val project = ensure.foundOne(
            operation = { projectDao.fetchByHash(event.serviceName()).singleOrNull() },
            code = PROJECT_NOT_FOUND,
            message = event.serviceName(),
        )
        return Pair(service?.buildId(), project)
    }

    /**
     * It decides the deploy status based on the desired and running count + the service status.
     * Basically if:
     * running count == desired count == 1 status == Active ==> DEPLOYED
     * running count == desired count == 0 ==> DRAINED
     * running count == 0 && desired count == 1 ==> PROVISIONING
     * running count == 1 && desired count == 0 ==> DRAINED
     */
    private fun decideDeployStatus(service: AwsEcsService?) =
        when {
            service?.runningCount() == service?.desiredCount() && service?.desiredCount() == 1
                    && service.status() == EcsServiceStatus.ACTIVE.name -> DeployStatus.DEPLOYED
            service?.runningCount() == service?.desiredCount() && service?.desiredCount() == 0 -> DeployStatus.DRAINED
            service?.runningCount() == 1  && service.desiredCount() == 0 -> DeployStatus.DRAINING
            service?.runningCount() == 0 && service.desiredCount() == 1 -> DeployStatus.PROVISIONING
            else -> throw UnknownProjectDeploymentStatus("Unknown status ${service?.status()} for service: $service")
        }


    private fun checkNotification(eventStr: String): SnsNotification {
        val notification = ensure.ofType<SnsNotification>(
            value = snsMessageManager.parseMessage(eventStr.byteInputStream()),
            valueDesc = "received message"
        )
        logger.info { "Processing SNS Notification for ECS events with id [${notification.messageId}]" }
        logger.debug { notification.message }
        return notification
    }

}

private interface GenericEvent {
    fun environment(): Environment
    fun serviceName(): String
    fun clusterName(): String
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

    override fun clusterName(): String {
        return detail.requestParameters.cluster.substringAfterLast("/")
    }
}

private enum class EcsServiceStatus {
    ACTIVE,
    DRAINING,
    INACTIVE,
}

private enum class EcsOperation {
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
        if (detail.eventName.toUpperCase() == EcsOperation.UPDATESERVICE.name)
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

    override fun clusterName(): String {
        return resources.singleOrNull()?.substringAfter("/")!!.substringBeforeLast("/")
    }
}
