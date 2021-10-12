package xtages.console.controller.api

import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import xtages.console.controller.api.model.Resource
import xtages.console.dao.fetchLatestPlan
import xtages.console.dao.maybeFetchOneByCognitoUserId
import xtages.console.exception.UserNeedsToHavePlanException
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.OrganizationToPlanDao
import xtages.console.service.AuthenticationService
import xtages.console.service.aws.RdsService

class ResourcesApiController(
    private val rdsService: RdsService,
    private val organizationDao: OrganizationDao,
    private val authenticationService: AuthenticationService,
    private val organizationToPlanDao: OrganizationToPlanDao,
): ResourcesApiControllerBase {

    override fun provisionResource( @PathVariable("resource") resource: Resource
    ): ResponseEntity<Unit> {
        val organization = organizationDao.maybeFetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        organization ?: run {
            val plan = organizationToPlanDao.fetchLatestPlan(organization!!)
                ?: throw UserNeedsToHavePlanException(
                    "User needs to have a plan associated to provision a DB"
                )
            when (resource) {
                Resource.DB -> {rdsService.provisionDb(organization, plan)}
            }
            return ResponseEntity.ok().build()
        }
     return ResponseEntity(FORBIDDEN)
    }
}
