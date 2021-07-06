package xtages.console.service.aws

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.*
import xtages.console.config.ConsoleProperties
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
class CloudWatchService(
    private val cloudWatchAsyncClient: CloudWatchAsyncClient,
    private val consoleProperties: ConsoleProperties
) {

    /**
     * @return The amount of bytes sent (egress) by the [projects] of [organization] from [since] until now.
     */
    fun getBytesSent(organization: Organization, vararg projects: Project, since: LocalDateTime): Long {
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
            .startTime(since.atZone(ZoneOffset.UTC).toInstant())
            .endTime(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
            .build()
        // Edge case when the account is created, there are no projects.
        if (request.metricDataQueries().size == 0) return 0L;

        return cloudWatchAsyncClient
            .getMetricData(request)
            .get()
            .metricDataResults()
            .flatMap { result -> result.values() }
            .sumOf { datapoint -> datapoint }
            .toLong()
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
