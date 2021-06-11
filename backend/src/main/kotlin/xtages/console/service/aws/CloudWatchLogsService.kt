package xtages.console.service.aws

import com.amazonaws.arn.Arn
import io.awspring.cloud.autoconfigure.context.properties.AwsRegionProperties
import mu.KotlinLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent
import xtages.console.config.ConsoleProperties
import xtages.console.controller.api.model.LogEvent
import xtages.console.controller.model.CodeBuildType
import xtages.console.exception.ensure
import xtages.console.pojo.codeBuildLogsGroupNameFor
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.pojos.Organization

private val logger = KotlinLogging.logger { }

/**
 * A [Service] to handle the logic to communicate to the AWS ClodWatch Logs service.
 */
@Service
class CloudWatchLogsService(
    private val cloudWatchLogsAsyncClient: CloudWatchLogsAsyncClient,
    private val organizationDao: OrganizationDao,
    private val awsRegionProperties: AwsRegionProperties,
    private val consoleProperties: ConsoleProperties,
) {


    /**
     * Creates a log group for the [organization] if necessary.
     */
    fun maybeCreateLogGroupForOrganization(organization: Organization) {
        val tags = buildLogGroupTags(organization)
        if (organization.cdLogGroupArn == null || organization.ciLogGroupArn == null) {
            cloudWatchLogsAsyncClient.createLogGroup(
                CreateLogGroupRequest.builder()
                    .logGroupName(organization.codeBuildLogsGroupNameFor(CodeBuildType.CI))
                    .tags(tags)
                    .build()
            ).get()
            cloudWatchLogsAsyncClient.createLogGroup(
                CreateLogGroupRequest.builder()
                    .logGroupName(organization.codeBuildLogsGroupNameFor(CodeBuildType.CD))
                    .tags(tags)
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

    /**
     * Gets the logs from a [logStreamName] in [logGroupName].
     */
    fun getLogs(logGroupName: String, logStreamName: String): List<LogEvent> {
        logger.info { "logGroupName: $logGroupName logStreamName: $logStreamName" }
        val logEventRequest = GetLogEventsRequest.builder()
            .logGroupName(logGroupName)
            .logStreamName(logStreamName)
            .startFromHead(true)
            .build()
        val logEvents = cloudWatchLogsAsyncClient.getLogEvents(logEventRequest).get().events()
        return logEvents.map { it.toLogEvent() }
    }

    private fun OutputLogEvent.toLogEvent() = LogEvent(
        message = message(),
        timestamp = timestamp(),
    )

    private fun buildLogGroupTags(organization: Organization): Map<String,String> {
        val orgHash = ensure.notNull(value = organization.hash, valueDesc = "organization.hash")
        val orgName = ensure.notNull(value = organization.name, valueDesc = "organization.name")
        return mapOf(
            "organization" to orgHash,
            "organization-name" to orgName
        )
    }

}
