package xtages.console.dao

import xtages.console.query.enums.ResourceType
import xtages.console.query.tables.daos.ResourceDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Resource
import xtages.console.query.tables.references.RESOURCE

fun ResourceDao.fetchByOrganizationNameAndResourceType(
    organization: Organization,
    resourceType: ResourceType
): Resource? {
    return ctx()
        .select(RESOURCE.asterisk())
        .where(
            RESOURCE.ORGANIZATION_NAME.eq(organization.name)
                .and(RESOURCE.RESOURCE_TYPE.eq(resourceType))
        )
        .fetchOneInto(Resource::class.java)
}
