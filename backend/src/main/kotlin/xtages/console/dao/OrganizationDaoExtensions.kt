package xtages.console.dao

import xtages.console.config.CognitoUserId
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.references.ORGANIZATION
import xtages.console.query.tables.references.XTAGES_USER

/**
 * Finds an [Organization] by its owner's [CognitoUserId].
 */
fun OrganizationDao.fetchOneByCognitoUserId(cognitoUserId: CognitoUserId): Organization {
    return ensure.foundOne(
        operation = {
            ctx().select(ORGANIZATION.asterisk())
                .from(ORGANIZATION)
                .join(XTAGES_USER).on(ORGANIZATION.NAME.eq(XTAGES_USER.ORGANIZATION_NAME))
                .where(XTAGES_USER.COGNITO_USER_ID.eq(cognitoUserId.id))
                .fetchOneInto(Organization::class.java)
        },
        code = ExceptionCode.ORG_NOT_FOUND,
        message = "Organization not found by cognitoUserId [$cognitoUserId]"
    )
}
