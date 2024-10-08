package xtages.console.dao

import org.springframework.cache.annotation.Cacheable
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

/**
 * Returns the latest [Plan] that an [Organization] has
 */
@Cacheable
fun OrganizationToPlanDao.fetchLatestPlan(organization: Organization): Plan? {
    return ctx().select(PLAN.asterisk())
        .from(PLAN)
        .join(ORGANIZATION_TO_PLAN).on(PLAN.ID.eq(ORGANIZATION_TO_PLAN.PLAN_ID))
        .where(ORGANIZATION_TO_PLAN.ORGANIZATION_NAME.eq(organization.name))
        .orderBy(ORGANIZATION_TO_PLAN.START_TIME.desc())
        .limit(1)
        .fetchOneInto(Plan::class.java)
}

/**
 * Returns the latest [OrganizationToPlan] for [organization], or `null` if there isn't one.
 */
fun OrganizationToPlanDao.fetchLatestByOrganization(organization: Organization): OrganizationToPlan? {
    return ctx().select()
        .from(ORGANIZATION_TO_PLAN)
        .where(ORGANIZATION_TO_PLAN.ORGANIZATION_NAME.eq(organization.name))
        .orderBy(ORGANIZATION_TO_PLAN.START_TIME.desc())
        .limit(1)
        .fetchOneInto(OrganizationToPlan::class.java)
}
