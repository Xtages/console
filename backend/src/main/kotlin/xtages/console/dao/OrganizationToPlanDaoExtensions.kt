package xtages.console.dao

import xtages.console.controller.model.PlanType
import xtages.console.query.tables.daos.OrganizationToPlanDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.OrganizationToPlan
import xtages.console.query.tables.pojos.Plan
import xtages.console.query.tables.references.ORGANIZATION_TO_PLAN
import xtages.console.query.tables.references.PLAN

/**
 * Inserts a row in "organization_to_plan" table, if there's a conflict then it's a no-op.
 */
fun OrganizationToPlanDao.insertIfNotExists(organizationToPlan: OrganizationToPlan) {
    ctx()
        .insertInto(ORGANIZATION_TO_PLAN)
        .columns(
            ORGANIZATION_TO_PLAN.ORGANIZATION_NAME,
            ORGANIZATION_TO_PLAN.PLAN_ID,
            ORGANIZATION_TO_PLAN.START_TIME
        )
        .values(
            organizationToPlan.organizationName,
            organizationToPlan.planId,
            organizationToPlan.startTime
        )
        .onConflict()
        .doNothing()
        .execute()
}

fun OrganizationToPlanDao.getTypeOfPlan(organization: Organization): PlanType {
    val plan = ctx().select(PLAN.NAME)
        .from(PLAN)
        .join(ORGANIZATION_TO_PLAN).on(PLAN.ID.eq(ORGANIZATION_TO_PLAN.PLAN_ID))
        .where(ORGANIZATION_TO_PLAN.ORGANIZATION_NAME.eq(organization.name))
        .fetchOneInto(Plan::class.java)
    return if (plan?.name == "Free") PlanType.FREE else PlanType.PAID
}
