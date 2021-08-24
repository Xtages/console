package xtages.console.service.aws

import mu.KotlinLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.*
import xtages.console.config.ConsoleProperties
import xtages.console.pojo.dbIdentifier
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutionException

private val logger = KotlinLogging.logger { }

@Service
class CloudWatchService(
    private val cloudWatchAsyncClient: CloudWatchAsyncClient,
    private val consoleProperties: ConsoleProperties
) {

    /**
     * @return The amount of free storage, in bytes, for the DB allocated to the [organization].
     */
    fun getBytesDbStorageFree(organization: Organization): Long {
        val request = GetMetricStatisticsRequest.builder()
            .metricName("FreeStorageSpace")
            .namespace("AWS/RDS")
            .dimensions(
                buildDimension(
                    name = "DBInstanceIdentifier",
                    value = organization.dbIdentifier
                )
            )
            .period(Duration.of(5, ChronoUnit.MINUTES).toSeconds().toInt())
            .statistics(Statistic.AVERAGE)
            .startTime(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5).toInstant().truncatedTo(ChronoUnit.MILLIS))
            .endTime(OffsetDateTime.now(ZoneOffset.UTC).toInstant().truncatedTo(ChronoUnit.MILLIS))
            .build()
        return try {
            val datapoints = cloudWatchAsyncClient
                .getMetricStatistics(request)
                .get()
                .datapoints()
            if (datapoints.isEmpty()) {
                return -1L
            }
            datapoints
                .map { point -> point.average() }
                .average()
                .toLong()
        } catch (e: ExecutionException) {
            if (e.cause is CloudWatchException) {
                logger.error(e) {}
                return 0L
            }
            throw e
        }
    }

    /**
     * @return The amount of bytes sent (egress) by the [projects] of [organization] from [since] until now.
     */
    fun getBytesSent(organization: Organization, vararg projects: Project, since: LocalDateTime): Long {
        // Edge case when the account is created, there are no projects.
        if (projects.isEmpty()) return 0L;
        val request = GetMetricDataRequest.builder()
            .metricDataQueries(
                listOf("staging", "production").map { env ->
                    projects.map { project ->
                        buildMetricDataQuery(
                            env = env,
                            project = project,
                            organization = organization
                        )
                    }
                }.flatten()
            )
            .startTime(since.atZone(ZoneOffset.UTC).toInstant().truncatedTo(ChronoUnit.MILLIS))
            .endTime(OffsetDateTime.now(ZoneOffset.UTC).toInstant().truncatedTo(ChronoUnit.MILLIS))
            .build()

        return try {
            cloudWatchAsyncClient
                .getMetricData(request)
                .get()
                .metricDataResults()
                .flatMap { result -> result.values() }
                .sumOf { datapoint -> datapoint }
                .toLong()
        } catch (e: ExecutionException) {
            if (e.cause is CloudWatchException) {
                logger.error { e }
                return 0L
            }
            throw e
        }
    }

    private fun buildMetricDataQuery(env: String, project: Project, organization: Organization): MetricDataQuery {
        return MetricDataQuery.builder()
            .id("${env}_${project.hash!!}")
            .metricStat(
                MetricStat.builder()
                    .metric(
                        Metric.builder()
                            .metricName(consoleProperties.aws.cloudWatch.egressBytesMetricName)
                            .namespace(project.hash!!)
                            .dimensions(
                                getDimensions(
                                    env = env,
                                    organizationHash = organization.hash!!,
                                    projectHash = project.hash!!
                                )
                            )
                            .build()
                    )
                    .period(Duration.of(1, ChronoUnit.HOURS).toSeconds().toInt())
                    .stat(Statistic.SUM.toString())
                    .build()
            )
            .build()
    }

    private fun getDimensions(env: String, organizationHash: String, projectHash: String): List<Dimension> {
        return listOf(
            buildDimension(name = "environment", value = env),
            buildDimension(name = "organization", value = organizationHash),
            buildDimension(name = "application", value = projectHash),
        )
    }

    private fun buildDimension(name: String, value: String) = Dimension.builder().name(name).value(value).build()
}
