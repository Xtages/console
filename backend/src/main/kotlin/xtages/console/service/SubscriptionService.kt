package xtages.console.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xtages.console.concurrent.waitForAll
import xtages.console.dao.fetchLatestByOrganization
import xtages.console.dao.insertIfNotExists
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.enums.ResourceStatus
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.OrganizationToPlanDao
import xtages.console.query.tables.daos.PlanDao
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.OrganizationToPlan
import xtages.console.query.tables.pojos.Plan
import xtages.console.service.aws.CodeBuildService
import xtages.console.service.aws.RdsService
import java.time.LocalDateTime
import java.time.ZoneOffset

private val logger = KotlinLogging.logger { }

@Service
class SubscriptionService(
    private val organizationDao: OrganizationDao,
    private val planDao: PlanDao,
    private val organizationToPlanDao: OrganizationToPlanDao,
    private val codeBuildService: CodeBuildService,
    private val projectDao: ProjectDao,
    private val rdsService: RdsService,
) {

    /**
     * Updates the subscription status for [organizationName]. [stripeCustomerId] may be specified if
     * [stripeCustomerId] was previously `null` which can happen for free plans.
     */
    @Transactional
    fun updateSubscriptionStatus(
        organizationName: String,
        stripeCustomerId: String? = null,
        subscriptionStatus: OrganizationSubscriptionStatus,
        newPlanId: String? = null,
    ): Organization {
        val organization = findOrganizationByName(organizationName)
        organization.subscriptionStatus = subscriptionStatus
        if (organization.stripeCustomerId == null) {
            organization.stripeCustomerId = stripeCustomerId
        }
        organizationDao.update(organization)
        logger.info { "Organization ${organization.name} has transitioned to [$subscriptionStatus] state" }

        if (newPlanId != null) {
            val latestPlan =
                planDao.fetchLatestByOrganization(organization = organization)
            if (latestPlan == null) {
                val plan = planDao.fetchByProductId(newPlanId).single()
                organizationToPlanDao.insertIfNotExists(
                    OrganizationToPlan(
                        organizationName = organization.name,
                        planId = plan.id,
                        startTime = LocalDateTime.now(ZoneOffset.UTC)
                    )
                )
                logger.debug { "Updated [Organization ${organization.name}] to plan [$newPlanId]" }
            } else {
                val plan = planDao.fetchByProductId(newPlanId).single()
                val organizationToPlan = ensure.notNull(
                    value = organizationToPlanDao.fetchLatestByOrganization(organization),
                    valueDesc = "organizationToPlan"
                )
                if (organizationToPlan.planId != plan.id) {
                    logger.debug { "Upgrading [Organization ${organization.name}] from plan [${plan.productId}] to plan [$newPlanId]" }
                    organizationToPlan.endTime = LocalDateTime.now(ZoneOffset.UTC)
                    organizationToPlanDao.update(organizationToPlan)
                    organizationToPlanDao.insert(
                        OrganizationToPlan(
                            organizationName = organizationName,
                            planId = plan.id,
                            startTime = LocalDateTime.now(ZoneOffset.UTC)
                        )
                    )
                    upgrade(organization = organization, fromPlan = latestPlan.plan, toPlan = plan)
                } else {
                    logger.warn { "Upgrade to plan [$newPlanId] has already been handled." }
                }
            }
        }
        return organization
    }

    private fun upgrade(organization: Organization, fromPlan: Plan, toPlan: Plan) {
        // TODO(czuniga): Make upgrading more flexible in regards to which plans can be upgraded. For the time being
        // we only support upgrading from Free to a Paid plan, nothing else.
        ensure.isTrue(
            value = !fromPlan.paid!!,
            code = ExceptionCode.INVALID_PLAN_UPGRADE,
            message = "Only the free plan can be upgraded"
        )
        ensure.isTrue(
            value = toPlan.paid!!,
            code = ExceptionCode.INVALID_PLAN_UPGRADE,
            message = "Cannot only upgrade to a paying plan"
        )
        if (fromPlan.concurrentBuildLimit != toPlan.concurrentBuildLimit) {
            val projects = projectDao.fetchByOrganization(organization.name!!)
            if (projects.isNotEmpty()) {
                projects
                    .flatMap { project ->
                        codeBuildService.updateCodeBuildProjects(
                            organization = organization,
                            project = project,
                            plan = toPlan,
                        )
                    }
                    .waitForAll()
            }
        }
        if (fromPlan.dbInstance != toPlan.dbInstance || fromPlan.dbStorageGbs != toPlan.dbStorageGbs) {
            val resource = rdsService.refreshPostgreSqlInstanceStatus(organization)
            if (resource != null) {
                if (resource.resourceStatus == ResourceStatus.REQUESTED
                    || resource.resourceStatus == ResourceStatus.PROVISIONED
                ) {
                    rdsService.updatePostgreSqlInstanceSpecs(organization = organization, plan = toPlan)
                }
            }
        }
    }

    private fun findOrganizationByName(organizationName: String): Organization {
        return ensure.foundOne(
            operation = { organizationDao.fetchOneByName(organizationName) },
            code = ExceptionCode.ORG_NOT_FOUND,
            lazyMessage = { "Organization [$organizationName] not found" }
        )
    }
}
