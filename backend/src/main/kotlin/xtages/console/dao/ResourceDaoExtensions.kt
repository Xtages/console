package xtages.console.dao

import org.jooq.impl.DSL.*
import xtages.console.query.enums.ResourceType
import xtages.console.query.tables.daos.ResourceDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Resource
import xtages.console.query.tables.references.RESOURCE
import xtages.console.query.tables.references.RESOURCE_ALLOCATION
import xtages.console.query.tables.references.RESOURCE_LIMIT

/**
 * Returns a [Resource] of [resourceType] for [organization] or `null`.
 */
fun ResourceDao.fetchByOrganizationNameAndResourceType(
    organization: Organization,
    resourceType: ResourceType
): Resource? {
    return ctx()
        .select(RESOURCE.asterisk())
        .from(RESOURCE)
        .where(
            RESOURCE.ORGANIZATION_NAME.eq(organization.name)
                .and(RESOURCE.RESOURCE_TYPE.eq(resourceType))
        )
        .fetchOneInto(Resource::class.java)
}

fun ResourceDao.canAllocatedResource(resourceType: ResourceType): Boolean {
    val limitAndCountSubQuery = ctx()
        .select(
            RESOURCE_LIMIT.LIMIT,
            coalesce(RESOURCE_ALLOCATION.COUNT, `val`(0)).`as`(RESOURCE_ALLOCATION.COUNT.name)
        )
        .from(
            RESOURCE_LIMIT.join(RESOURCE_ALLOCATION)
                .on(RESOURCE_LIMIT.RESOURCE_TYPE.eq(RESOURCE_ALLOCATION.RESOURCE_TYPE))
        )
        .where(RESOURCE_LIMIT.RESOURCE_TYPE.eq(resourceType))
    val countField = limitAndCountSubQuery.field(RESOURCE_ALLOCATION.COUNT.name, Int::class.java)!!
    val limitField = limitAndCountSubQuery.field(RESOURCE_LIMIT.LIMIT.name, Int::class.java)!!
    val underLimitField = field(countField.lessThan(limitField))
    val isUnderLimit = ctx()
        .select(
            underLimitField
        ).from(
            limitAndCountSubQuery
        ).fetch()
    return if (isUnderLimit.isEmpty()) true else isUnderLimit.single().getValue(underLimitField)
}

fun ResourceDao.insertIfNotExists(resource: Resource) {
    ctx()
        .insertInto(RESOURCE)
        .columns(
            RESOURCE.ORGANIZATION_NAME,
            RESOURCE.RESOURCE_TYPE,
            RESOURCE.RESOURCE_STATUS,
            RESOURCE.RESOURCE_ARN,
            RESOURCE.RESOURCE_ENDPOINT
        )
        .values(
            resource.organizationName,
            resource.resourceType,
            resource.resourceStatus,
            resource.resourceArn,
            resource.resourceEndpoint,
        )
        .onConflict(RESOURCE.ORGANIZATION_NAME, RESOURCE.RESOURCE_TYPE)
        .doNothing()
        .execute()
}
