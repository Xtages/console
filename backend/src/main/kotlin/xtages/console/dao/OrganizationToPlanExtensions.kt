package xtages.console.dao

import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.OrganizationToPlanDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Plan
import xtages.console.query.tables.references.ORGANIZATION_TO_PLAN
import xtages.console.query.tables.references.PLAN

fun OrganizationToPlanDao.findPlanBy(organization: Organization): Plan {
    return ensure.foundOne(
        operation = {
            ctx().select(PLAN.asterisk())
                .from(PLAN)
                .join(ORGANIZATION_TO_PLAN).on(PLAN.ID.eq(ORGANIZATION_TO_PLAN.PLAN_ID))
                .where(ORGANIZATION_TO_PLAN.ORGANIZATION_NAME.eq(organization.name))
                .orderBy(ORGANIZATION_TO_PLAN.START_TIME.desc())
                .limit(1)
                .fetchOneInto(Plan::class.java)
        },
        code = ExceptionCode.PLAN_NOT_FOUND,
        message = "Plan usage not found for Organization ${organization.name}"
    )
}
