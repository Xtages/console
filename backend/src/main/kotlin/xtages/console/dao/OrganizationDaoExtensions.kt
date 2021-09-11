package xtages.console.dao

import org.springframework.cache.annotation.Cacheable
import xtages.console.config.CognitoUserId
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.references.ORGANIZATION
import xtages.console.query.tables.references.XTAGES_USER

/**
 * Finds an [Organization] by its owner's [CognitoUserId] or throws.
 */
@Cacheable
fun OrganizationDao.fetchOneByCognitoUserId(cognitoUserId: CognitoUserId): Organization {
    return ensure.foundOne(
        operation = { maybeFetchOneByCognitoUserId(cognitoUserId = cognitoUserId) },
        code = ExceptionCode.ORG_NOT_FOUND,
        message = "Organization not found by cognitoUserId [$cognitoUserId]"
    )
}

/**
 * Finds an [Organization] by its owner's [CognitoUserId] or returns `null` if not found.
 */
@Cacheable
fun OrganizationDao.maybeFetchOneByCognitoUserId(cognitoUserId: CognitoUserId): Organization? {
    return ctx().select(ORGANIZATION.asterisk())
        .from(ORGANIZATION)
        .join(XTAGES_USER).on(ORGANIZATION.NAME.eq(XTAGES_USER.ORGANIZATION_NAME))
        .where(XTAGES_USER.COGNITO_USER_ID.eq(cognitoUserId.id))
        .fetchOneInto(Organization::class.java)
}
