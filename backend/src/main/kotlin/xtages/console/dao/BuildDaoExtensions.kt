package xtages.console.dao

import org.jooq.impl.DSL.*
import xtages.console.query.enums.BuildStatus
import xtages.console.query.enums.BuildType
import xtages.console.query.tables.daos.BuildDao
import xtages.console.query.tables.pojos.Build
import xtages.console.query.tables.pojos.BuildStatsPerMonth
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.references.BUILD
import xtages.console.query.tables.references.BUILD_STATS_PER_MONTH
import xtages.console.query.tables.references.PROJECT
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Fetches the latest [Build]s for [projects].
 */
fun BuildDao.fetchLatestByProject(projects: List<Project>): List<Build> {
    val projectIds = projects.mapNotNull { project -> project.id }
    return ctx()
        .select(BUILD.asterisk())
        .distinctOn(BUILD.PROJECT_ID)
        .from(BUILD)
        .where(BUILD.PROJECT_ID.`in`(projectIds))
        .orderBy(
            BUILD.PROJECT_ID, BUILD.START_TIME.desc(),
        ).fetchInto(Build::class.java)
}

/**
 * Returns the percentage of successful [Build]s in the last month.
 */
fun BuildDao.findPercentageOfSuccessfulBuildsInMonth(organizationName: String, projectName: String): Double {
    val stats = ctx()
        .select(BUILD_STATS_PER_MONTH.asterisk())
        .from(BUILD_STATS_PER_MONTH)
        .where(
            BUILD_STATS_PER_MONTH.ORGANIZATION.eq(organizationName)
                .and(
                    BUILD_STATS_PER_MONTH.PROJECT.eq(projectName).and(
                        BUILD_STATS_PER_MONTH.DATE.greaterOrEqual(
                            LocalDate.now().withDayOfMonth(1)
                        )
                    )
                )
        ).fetchInto(BuildStatsPerMonth::class.java)
    val succeeded = stats.singleOrNull { stat -> stat.status == BuildStatus.SUCCEEDED }?.buildCount ?: 0L
    val failed = stats.singleOrNull { stat -> stat.status == BuildStatus.FAILED }?.buildCount ?: 0L
    return when {
        succeeded + failed == 0L -> 0.0
        else -> succeeded.toDouble() / (succeeded + failed).toDouble()
    }
}

/**
 * Finds the last [Build] deployed to `staging` for [organizationName] and [projectName].
 *
 * @return `null` if there hasn't been a deploy yet.
 */
fun BuildDao.findLatestDeploy(organizationName: String, projectName: String): Build? {
    return findCdBuildsByOrganizationAndProjectAndEnvironment(
        organizationName = organizationName,
        projectName = projectName,
        environment = "staging",
        limit = 1,
    ).singleOrNull()
}

/**
 * Finds the last 2 [Build]s that were promoted to `production` for [organizationName] and [projectName].
 */
fun BuildDao.findLastTwoPreviousPromotions(organizationName: String, projectName: String): List<Build> {
    return findCdBuildsByOrganizationAndProjectAndEnvironment(
        organizationName = organizationName,
        projectName = projectName,
        environment = "production",
        limit = 2
    )
}

private fun BuildDao.findCdBuildsByOrganizationAndProjectAndEnvironment(
    organizationName: String,
    projectName: String,
    environment: String,
    limit: Int,
): List<Build> {
    return ctx()
        .select(BUILD.asterisk())
        .from(BUILD)
        .join(PROJECT).on(BUILD.PROJECT_ID.eq(PROJECT.ID))
        .where(
            PROJECT.NAME.eq(projectName).and(
                PROJECT.ORGANIZATION.eq(organizationName)
                    .and(
                        BUILD.TYPE.eq(BuildType.CD)
                            .and(
                                BUILD.STATUS.eq(BuildStatus.SUCCEEDED)
                                    .and(BUILD.ENVIRONMENT.eq(environment))
                            )
                    )
            )
        )
        .orderBy(BUILD.END_TIME.desc())
        .limit(limit)
        .fetchInto(Build::class.java)
}

/** @return All the [Build]s that where either started or ended within [dateRange]. */
fun BuildDao.fetchByOrganizationInDateRange(
    organizationName: String,
    dateRange: ClosedRange<LocalDateTime>
): List<Build> {
    return ctx()
        .select(BUILD.asterisk())
        .from(BUILD)
        .join(PROJECT).on(BUILD.PROJECT_ID.eq(PROJECT.ID))
        .where(
            PROJECT.ORGANIZATION.eq(organizationName).and(
                BUILD.START_TIME.between(dateRange.start, dateRange.endInclusive)
                    .or(BUILD.END_TIME.between(dateRange.start, dateRange.endInclusive))
            )
        )
        .fetchInto(Build::class.java)
}

/**
 * Finds the previous `CI` [Build] to [build].
 */
fun BuildDao.findPreviousCIBuild(build: Build): Build? {
    return ctx()
        .select(BUILD.asterisk())
        .from(BUILD)
        .join(PROJECT).on(BUILD.PROJECT_ID.eq(build.projectId))
        .where(BUILD.END_TIME.lessThan(build.endTime).and(BUILD.TYPE.eq(BuildType.CI)))
        .orderBy(BUILD.END_TIME.desc())
        .limit(1)
        .fetchOneInto(Build::class.java)
}
