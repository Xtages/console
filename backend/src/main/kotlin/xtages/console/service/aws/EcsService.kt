package xtages.console.service.aws

import mu.KotlinLogging
import org.springframework.stereotype.Service
import xtages.console.controller.api.model.LogEvent
import xtages.console.controller.api.model.Logs
import xtages.console.pojo.ecsLogGroupName
import xtages.console.query.tables.pojos.Project

private const val LOGS_PAGE_SIZE = 100
private val logger = KotlinLogging.logger { }

@Service
class EcsService(private val cloudWatchLogsService: CloudWatchLogsService) {

    /**
     * Returns the [LogEvent]s for a task that has run in ECS.
     */
    fun getLogsFor(
        env: String,
        buildId: Long,
        project: Project,
        startTimeInMillis: Long?,
        endTimeInMillis: Long?,
        token: String?
    ): Logs {
        val logGroupName = project.ecsLogGroupName(env)
        val logStreamPrefix = "$env-$buildId"
        val latestLogStream = cloudWatchLogsService
            .getLogStreamsByLogStreamPrefix(
                logGroupName = logGroupName,
                logStreamPrefix = logStreamPrefix,
            )
            .maxByOrNull { logStream -> logStream.lastEventTimestamp() }
        if (latestLogStream != null) {
            return cloudWatchLogsService.getLogs(
                logGroupName = logGroupName,
                logStreamName = latestLogStream.logStreamName(),
                startTime = startTimeInMillis,
                endTime = endTimeInMillis ?: latestLogStream.lastEventTimestamp(),
                paginationToken = token,
                limit = LOGS_PAGE_SIZE,
            )
        } else {
            logger.warn { "Could not find a LogStream with prefix [$logStreamPrefix] for LogGroup [$logGroupName]" }
        }

        return Logs(events = emptyList())
    }
}
