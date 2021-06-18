package xtages.console.service

import org.springframework.stereotype.Service
import xtages.console.dao.fetchActiveCreditsByOrganizationName
import xtages.console.dao.fetchByOrganizationInDateRange
import xtages.console.dao.fetchLatestByOrganizationName
import xtages.console.exception.UsageOverLimitException
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.enums.ResourceType
import xtages.console.query.tables.daos.BuildDao
import xtages.console.query.tables.daos.CreditDao
import xtages.console.query.tables.daos.PlanDao
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Plan
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

private const val UNLIMITED = -1L

@Service
class UsageService(
    private val creditDao: CreditDao,
    private val planDao: PlanDao,
    private val projectDao: ProjectDao,
    private val buildDao: BuildDao
) {
    /**
     * Checks if the usage for [resourceType] is over the limit configured in the [Plan] [organization] is currently
     * subscribed to.
     *
     * @return An [UsageUnderLimit] instance if the usage is under the limit then, otherwise throws an
     * [UsageOverLimitException]. For an non-throwing version use [getUsageDetails].
     * @throws [UsageOverLimitException] if the usage of the resource if over the limit.
     */
    fun checkUsageIsBelowLimit(organization: Organization, resourceType: ResourceType): UsageUnderLimit {
        return when (val usageResult = getUsageDetails(organization, resourceType)) {
            is UsageUnderLimit -> usageResult
            is UsageOverLimitBecauseOfSubscriptionStatus -> throw UsageOverLimitException(usageResult)
            is UsageOverLimitNoPlan -> throw UsageOverLimitException(usageResult)
            is UsageOverLimitWithDetails -> throw UsageOverLimitException(usageResult)
        }
    }

    /**
     * @return An [UsageResult] with the details of the usage for [resourceType] and the limit set by the [Plan]
     * [organization] is currently subscribed to.
     */
    fun getUsageDetails(organization: Organization, resourceType: ResourceType): UsageResult {
        return when (val subscriptionStatus = organization.subscriptionStatus!!) {
            OrganizationSubscriptionStatus.UNCONFIRMED,
            OrganizationSubscriptionStatus.SUSPENDED,
            OrganizationSubscriptionStatus.CANCELLED -> {
                UsageOverLimitBecauseOfSubscriptionStatus(resourceType = resourceType, status = subscriptionStatus)
            }
            else -> {
                // If there's no current plan for the organization then they are considered over the limit for
                // everything.
                val (plan, billingCycleAnchorDay) = planDao
                    .fetchLatestByOrganizationName(organizationName = organization.name!!)
                    ?: return UsageOverLimitNoPlan(resourceType = resourceType)
                val currentBillingMonth = CurrentBillingMonth(anchorDay = billingCycleAnchorDay)
                val limit = findLimitForResourceType(
                    organization = organization,
                    plan = plan,
                    resourceType = resourceType,
                )
                return when (limit) {
                    // If the limit is -1, then it means the resource is unlimited
                    UNLIMITED -> UsageUnderLimitGrandfathered(resourceType = resourceType)
                    else -> {
                        val usage = findUsageForResource(
                            organization = organization,
                            resourceType = resourceType,
                            dateRange = currentBillingMonth,
                        )
                        return if (usage >= limit) {
                            UsageOverLimitWithDetails(resourceType = resourceType, limit = limit, usage = usage)
                        } else {
                            UsageUnderLimitWithDetails(resourceType = resourceType, limit = limit, usage = usage)
                        }
                    }
                }
            }
        }
    }

    private fun findUsageForResource(
        organization: Organization,
        resourceType: ResourceType,
        dateRange: ClosedRange<LocalDateTime>
    ): Long {
        return when (resourceType) {
            ResourceType.PROJECT -> projectDao.fetchByOrganization(organization.name!!).size.toLong()
            ResourceType.MONTHLY_BUILD_MINUTES -> calculateBuildMinutes(
                organization = organization,
                billingCycle = dateRange
            )
            ResourceType.MONTHLY_DATA_TRANSFER_GBS -> 0L
        }
    }

    private fun calculateBuildMinutes(organization: Organization, billingCycle: ClosedRange<LocalDateTime>): Long {
        val builds =
            buildDao.fetchByOrganizationInDateRange(organizationName = organization.name!!, dateRange = billingCycle)
        val buildSecs = builds.sumOf { build ->
            val startTime = build.startTime!!
            val endTime = build.endTime
            when {
                endTime == null -> 0
                startTime in billingCycle && endTime in billingCycle -> {
                    Duration.between(startTime, endTime).toSeconds()
                }
                startTime !in billingCycle -> {
                    Duration.between(billingCycle.start, endTime).toSeconds()
                }
                endTime !in billingCycle -> {
                    Duration.between(startTime, billingCycle.endInclusive).toSeconds()
                }
                else -> 0L
            }
        }
        // Round up to the nearest minute
        return if (buildSecs % 60.0 == 0.0) {
            buildSecs / 60
        } else {
            (buildSecs / 60) + 1
        }
    }

    fun findLimitForResourceType(
        organization: Organization,
        plan: Plan,
        resourceType: ResourceType,
    ): Long {
        // when `limit` is `-1` it means that there's actually no limit, this might happen for orgs with grandfathered
        // plans (in other words, when a new plan is created that limits a new type of resource).
        val limit = extractLimitFromPlan(plan = plan, resourceType = resourceType)
        if (limit == UNLIMITED) {
            return limit
        }
        val credits = creditDao.fetchActiveCreditsByOrganizationName(organizationName = organization.name!!)
            .filter { credit -> credit.resource == resourceType }
        return limit + credits.sumOf { credit -> credit.amount!! }
    }

    private fun extractLimitFromPlan(plan: Plan, resourceType: ResourceType): Long {
        return when (resourceType) {
            ResourceType.PROJECT -> plan.limitProjects!!
            ResourceType.MONTHLY_BUILD_MINUTES -> plan.limitMonthlyBuildMinutes!!
            ResourceType.MONTHLY_DATA_TRANSFER_GBS -> plan.limitMonthlyDataTransferGbs!!
        }
    }
}

/** A sealed class hierarchy to represent different usage states */
sealed class UsageResult(val resourceType: ResourceType)

/** Represents usage of [resourceType] being *over* the limit. */
sealed class UsageOverLimit(resourceType: ResourceType) : UsageResult(resourceType = resourceType)

/** Represents usage of [resourceType] being *under* the limit. */
sealed class UsageUnderLimit(resourceType: ResourceType) : UsageResult(resourceType = resourceType)

/** Represents [usage] of [resourceType] being *over* the [limit]. */
class UsageOverLimitWithDetails(resourceType: ResourceType, val limit: Long, val usage: Long) :
    UsageOverLimit(resourceType = resourceType)

/**
 * Represents [resourceType] being considered *over* the limit because the [OrganizationSubscriptionStatus] of the
 *[Organization] is not in good standing.
 */
class UsageOverLimitBecauseOfSubscriptionStatus(
    resourceType: ResourceType,
    val status: OrganizationSubscriptionStatus
) : UsageOverLimit(resourceType = resourceType)

/**
 * Represents [resourceType] being considered *over* the limit because the [Organization] doesn't have a current [Plan].
 */
class UsageOverLimitNoPlan(resourceType: ResourceType) : UsageOverLimit(resourceType = resourceType)

/** Represents [usage] of [resourceType] being *under* the [limit] and includes details about [usage] and [limit]. */
class UsageUnderLimitWithDetails(resourceType: ResourceType, val limit: Long, val usage: Long) :
    UsageUnderLimit(resourceType = resourceType)

/**
 * Represents [resourceType] being considered *under* the limit because the [Organization] has been grandfathered into a
 * [Plan] that doesn't have a limit for [resourceType] configured.
 */
class UsageUnderLimitGrandfathered(resourceType: ResourceType) : UsageUnderLimit(resourceType = resourceType)


/**
 * A [ClosedRange<LocalDateTime>] representing the current billing month for a subscription.
 *
 * The billing month for a subscription starts at [anchorDay] every month.
 */
data class CurrentBillingMonth(val anchorDay: Int) : ClosedRange<LocalDateTime> {
    override val start: LocalDateTime by lazy {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        if (anchorDay <= now.dayOfMonth) {
            now.withDayOfMonth(anchorDay).toLocalDate().atStartOfDay()
        } else {
            // If anchorDay is later than today it means that the current wall-clock day is contained in the previous
            // billing month and as such we subtract one month from today's date.
            val lastMonth = now.minusMonths(1).monthValue
            now.withMonth(lastMonth).withDayOfMonth(anchorDay).toLocalDate().atStartOfDay()
        }
    }
    override val endInclusive: LocalDateTime by lazy {
        start.plusMonths(1).toLocalDate().atTime(LocalTime.MAX)
    }
}
