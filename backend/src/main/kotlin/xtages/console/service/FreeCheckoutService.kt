package xtages.console.service

import org.springframework.stereotype.Service
import xtages.console.controller.model.PlanType
import xtages.console.dao.insertIfNotExists
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.OrganizationToPlanDao
import xtages.console.query.tables.daos.PlanDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.OrganizationToPlan
import java.time.LocalDateTime

private val FREE_PLAN_NAME = PlanType.FREE.name.toLowerCase().capitalize()

@Service
class FreeCheckoutService(
    private val organizationDao: OrganizationDao,
    private val planDao: PlanDao,
    private val organizationToPlanDao: OrganizationToPlanDao,
) {

    fun provision(organization: Organization) {
        organization.subscriptionStatus = OrganizationSubscriptionStatus.ACTIVE
        organizationDao.update(organization)

        val plan = planDao.fetchByName(FREE_PLAN_NAME).single()

        organizationToPlanDao.insertIfNotExists(
            OrganizationToPlan(
                organizationName = organization.name,
                planId = plan.id,
                startTime = LocalDateTime.now()
            )
        )
    }
}
