package xtages.console.controller.api

import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import xtages.console.controller.api.model.ResourceType
import xtages.console.controller.api.model.UsageDetail
import xtages.console.controller.model.resourceTypeToResourceTypePojo
import xtages.console.controller.model.usageDetailPojoToUsageDetail
import xtages.console.dao.fetchLatestPlan
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.dao.maybeFetchOneByCognitoUserId
import xtages.console.exception.UserNeedsToHavePlanException
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.OrganizationToPlanDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.service.AuthenticationService
import xtages.console.service.UsageService
import xtages.console.service.aws.RdsService
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream
import kotlin.streams.toList

val IMPLEMENTED_RESOURCES =
    listOf(ResourceType.PROJECT, ResourceType.BUILD_MINUTES, ResourceType.DATA_TRANSFER, ResourceType.POSTGRESQL)

/**
 * Controller to handle queries about resources, including their usage/quota.
 */
@Controller
class ResourceApiController(
    private val rdsService: RdsService,
    private val organizationDao: OrganizationDao,
    private val authenticationService: AuthenticationService,
    private val usageService: UsageService,
    private val organizationToPlanDao: OrganizationToPlanDao,
) : ResourceApiControllerBase {

    override fun provisionResource(@PathVariable(value = "resource") resource: ResourceType): ResponseEntity<Unit> {
        val organization = organizationDao.maybeFetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        organization ?: run {
            val plan = organizationToPlanDao.fetchLatestPlan(organization!!)
                ?: throw UserNeedsToHavePlanException(
                    "User needs to have a plan associated to provision a DB"
                )
            return when (resource) {
                ResourceType.POSTGRESQL -> {
                    rdsService.provisionPostgreSql(organization, plan)
                    return ResponseEntity.ok().build()
                }
                else -> ResponseEntity(BAD_REQUEST)
            }
        }
        return ResponseEntity(FORBIDDEN)
    }

    override fun getAllUsageDetails(): ResponseEntity<List<UsageDetail>> {
        val organization = organizationDao.maybeFetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        organization ?: run {
            return ResponseEntity.ok(emptyList())
        }
        val usageFutures = IMPLEMENTED_RESOURCES.map { resourceType ->
            CompletableFuture.supplyAsync {
                getUsageDetail(organization, resourceType = resourceType)
            }
        }
        val usages = Stream.of(*usageFutures.toTypedArray())
            .map { future -> future.join() }
            .toList()
            .mapNotNull(usageDetailPojoToUsageDetail::convert)
        return ResponseEntity.ok(usages)
    }

    override fun getUsageDetail(resourceType: ResourceType): ResponseEntity<UsageDetail> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val usageDetail = getUsageDetail(organization, resourceType)
        return ResponseEntity.ok(usageDetailPojoToUsageDetail.convert(usageDetail))
    }

    private fun getUsageDetail(
        organization: Organization,
        resourceType: ResourceType
    ): xtages.console.service.UsageDetail {
        return usageService.getUsageDetail(
            organization = organization,
            resourceType = resourceTypeToResourceTypePojo.convert(resourceType)!!
        )
    }
}
