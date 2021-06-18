package xtages.console.dao

import xtages.console.query.tables.daos.PlanDao
import xtages.console.query.tables.pojos.Plan
import xtages.console.query.tables.references.ORGANIZATION_TO_PLAN
import xtages.console.query.tables.references.PLAN
import java.time.LocalDateTime

/**
 * Finds the latest active [Plan] for [organizationName].
 */
fun PlanDao.fetchLatestByOrganizationName(organizationName: String): PlanWithBillingCycleAnchorDay? {
    val result = ctx()
        .select(PLAN.asterisk(), ORGANIZATION_TO_PLAN.START_TIME)
        .from(PLAN)
        .join(ORGANIZATION_TO_PLAN).on(PLAN.ID.eq(ORGANIZATION_TO_PLAN.PLAN_ID))
        .where(
            ORGANIZATION_TO_PLAN.ORGANIZATION_NAME.eq(organizationName).and(
                ORGANIZATION_TO_PLAN.END_TIME.isNull.or(
                    ORGANIZATION_TO_PLAN.END_TIME.lessOrEqual(LocalDateTime.now())
                )
            )
        )
        .orderBy(ORGANIZATION_TO_PLAN.START_TIME.desc())
        .limit(1)
        .fetch()
    if (result.isEmpty()) {
        return null
    }
    val record = result.first()
    val plan = record.into(Plan::class.java)
    val startTime = record[ORGANIZATION_TO_PLAN.START_TIME]!!
    return PlanWithBillingCycleAnchorDay(plan = plan, billingCycleAnchorDay = startTime.dayOfMonth)
}

data class PlanWithBillingCycleAnchorDay(val plan: Plan, val billingCycleAnchorDay: Int)
