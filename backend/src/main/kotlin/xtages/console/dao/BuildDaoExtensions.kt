package xtages.console.dao

import xtages.console.query.enums.BuildStatus
import xtages.console.query.tables.daos.BuildDao
import xtages.console.query.tables.pojos.Build
import xtages.console.query.tables.pojos.BuildStatsPerMonth
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.references.BUILD
import xtages.console.query.tables.references.BUILD_STATS_PER_MONTH
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Fetches the latest [Build]s for [projects].
 */
fun BuildDao.fetchLatestByProject(projects: List<Project>): List<Build> {
    val projectIds = projects.mapNotNull { project -> project.id }
    return ctx().select(BUILD.asterisk()).distinctOn(BUILD.PROJECT_ID)
        .from(BUILD)
        .where(BUILD.PROJECT_ID.`in`(projectIds))
        .orderBy(
            BUILD.PROJECT_ID, BUILD.START_TIME.desc(),
        ).fetchInto(Build::class.java)
}

/**
 * Returns the percentage of successful [Build]s in the last month.
 */
fun BuildDao.findPercentageOfSuccessfulBuildsInMonth(organizationName: String): Double {
    val stats = ctx().select(BUILD_STATS_PER_MONTH.asterisk()).from(BUILD_STATS_PER_MONTH)
        .where(
            BUILD_STATS_PER_MONTH.ORGANIZATION.eq(organizationName)
                .and(
                    BUILD_STATS_PER_MONTH.DATE.eq(
                        LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1)
                    )
                )
        ).fetchInto(BuildStatsPerMonth::class.java)
    val succeeded = stats.singleOrNull { stat -> stat.status == BuildStatus.SUCCEEDED }?.buildCount ?: 0L
    val failed = stats.singleOrNull { stat -> stat.status == BuildStatus.FAILED }?.buildCount ?: 0L
    return when {
        succeeded == 0L || failed == 0L -> 0.0
        failed == 0L && succeeded != 0L -> 1.0
        else -> (succeeded + failed).toDouble() / failed
    }
}
