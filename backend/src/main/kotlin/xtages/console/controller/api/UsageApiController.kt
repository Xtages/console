package xtages.console.controller.api

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.controller.api.model.ResourceType
import xtages.console.controller.api.model.UsageDetail
import xtages.console.controller.model.usageDetailPojoToUsageDetail
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.dao.maybeFetchOneByCognitoUserId
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.service.AuthenticationService
import xtages.console.service.UsageService
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream
import kotlin.streams.toList
import xtages.console.query.enums.ResourceType as ResourceTypePojo
import xtages.console.service.UsageDetail as UsageDetailPojo

/**
 * Controller to handle queries for usage of resources.
 */
@Controller
class UsageApiController(
    val organizationDao: OrganizationDao,
    val usageService: UsageService,
    val authenticationService: AuthenticationService,
) : UsageApiControllerBase {

    override fun getAllUsageDetails(): ResponseEntity<List<UsageDetail>> {
        val organization = organizationDao.maybeFetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        organization ?: run {
            return ResponseEntity.ok(emptyList())
        }
        val usageFutures = ResourceType.values().map { resourceType ->
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

    private fun getUsageDetail(organization: Organization, resourceType: ResourceType): UsageDetailPojo {
        return usageService.getUsageDetail(
            organization = organization,
            resourceType = ResourceTypePojo.valueOf(resourceType.value)
        )
    }
}
