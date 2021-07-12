package xtages.console.service.aws

import com.amazonaws.arn.Arn
import io.awspring.cloud.autoconfigure.context.properties.AwsRegionProperties
import mu.KotlinLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient
import software.amazon.awssdk.services.cloudwatchlogs.model.*
import xtages.console.config.ConsoleProperties
import xtages.console.controller.api.model.LogEvent
import xtages.console.controller.api.model.Logs
import xtages.console.controller.model.CodeBuildType
import xtages.console.exception.ensure
import xtages.console.pojo.codeBuildLogsGroupNameFor
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.pojos.Organization

private val logger = KotlinLogging.logger { }

private const val MAX_API_CALLS = 10

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

        val ciLogGroupName = organization.codeBuildLogsGroupNameFor(CodeBuildType.CI)
        val cdLogGroupName = organization.codeBuildLogsGroupNameFor(CodeBuildType.CD)
        if (organization.cdLogGroupArn == null || organization.ciLogGroupArn == null) {
            buildLogGroup(ciLogGroupName, organization)
            buildLogGroup(cdLogGroupName, organization)

            // For some reason the `createLogGroup` call doesn't return anything in it's response, so we have to create the
            // ARNs for the LogGroups by hand.
            val cdLogGroupArn = Arn.builder()
                .withPartition("aws")
                .withService("logs")
                .withRegion(awsRegionProperties.static)
                .withAccountId(consoleProperties.aws.accountId)
                .withResource("log-group:${cdLogGroupName}")
                .build()
            organization.cdLogGroupArn = cdLogGroupArn.toString()
            val ciLogGroupArn = cdLogGroupArn.toBuilder()
                .withResource("log-group:${ciLogGroupName}")
                .build()
            organization.ciLogGroupArn = ciLogGroupArn.toString()
            organizationDao.merge(organization)
        }
    }

    /**
     * Finds all the [LogStream]s from [logGroupName] that match [logStreamPrefix].
     */
    fun getLogStreamsByLogStreamPrefix(logGroupName: String, logStreamPrefix: String): List<LogStream> {
        fun describeLogStreams(
            logGroupName: String,
            logStreamPrefix: String,
            nextToken: String? = null
        ): DescribeLogStreamsResponse {
            val builder =
                DescribeLogStreamsRequest.builder()
                    .logGroupName(logGroupName)
                    .logStreamNamePrefix(logStreamPrefix)
            if (nextToken != null) {
                builder.nextToken(nextToken)
            }
            return cloudWatchLogsAsyncClient.describeLogStreams(builder.build()).get()
        }

        val allLogStreams = mutableListOf<LogStream>()
        var nextToken: String? = null
        do {
            val response = describeLogStreams(
                logGroupName = logGroupName,
                logStreamPrefix = logStreamPrefix,
                nextToken = nextToken,
            )
            nextToken = response.nextToken()
            allLogStreams.addAll(response.logStreams())
        } while (nextToken != null)
        return allLogStreams
    }

    /**
     * Gets the logs from a [logStreamName] in [logGroupName].
     */
    fun getLogs(
        logGroupName: String,
        logStreamName: String,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int? =null,
        paginationToken: String? = null
    ): Logs {
        logger.info { "logGroupName: $logGroupName logStreamName: $logStreamName" }
        var nextToken: String? = paginationToken
        val events = mutableListOf<OutputLogEvent>()
        var apiCalls = 0
        do {
            val response = cloudWatchLogsAsyncClient.getLogEvents(
                GetLogEventsRequest.builder()
                    .startFromHead(true)
                    .startTime(startTime)
                    .endTime(endTime)
                    .logGroupName(logGroupName)
                    .logStreamName(logStreamName)
                    .nextToken(nextToken)
                    .limit(limit)
                    .build()
            ).get()
            apiCalls++
            if (response.hasEvents()) {
                events.addAll(response.events())
            }
            nextToken = if (nextToken != response.nextForwardToken()) {
                response.nextForwardToken()
            } else {
                null
            }
        } while (nextToken != null && response.eventsIsEmpty() && apiCalls <= MAX_API_CALLS)
        return Logs(
            events = events.toList().map { e -> e.toLogEvent() },
            nextToken = nextToken,
        )
    }

    private fun GetLogEventsResponse.eventsIsEmpty() = !hasEvents() || events().isEmpty()

    private fun OutputLogEvent.toLogEvent() = LogEvent(
        message = message(),
        timestamp = timestamp(),
    )

    private fun buildLogGroupTags(organization: Organization): Map<String, String> {
        val orgHash = ensure.notNull(value = organization.hash, valueDesc = "organization.hash")
        val orgName = ensure.notNull(value = organization.name, valueDesc = "organization.name")
        return mapOf(
            "organization" to orgName,
            "organization-hash" to orgHash
        )
    }

    private fun buildLogGroup(name: String, organization: Organization) {
        val tags = buildLogGroupTags(organization)
        cloudWatchLogsAsyncClient.createLogGroup(
            CreateLogGroupRequest.builder()
                .logGroupName(name)
                .tags(tags)
                .build()
        ).get()
    }

}
