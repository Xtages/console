package xtages.console.dao

import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.XtagesUser
import xtages.console.query.tables.references.XTAGES_USER

/**
 * Given an [Organization] returns it's owner.
 * Throws an exception if an owner is not found.
 */
fun XtagesUserDao.fetchOrganizationsOwner(organization: Organization) = fetchOrganizationsOwner(organization.name!!)

/**
 * Given an [Organization.name] returns it's owner.
 * Throws an exception if an owner is not found.
 */
fun XtagesUserDao.fetchOrganizationsOwner(organizationName: String): XtagesUser {
    return ensure.foundOne(
        operation = {
            ctx().select(XTAGES_USER.asterisk()).from(XTAGES_USER).where(
                XTAGES_USER.ORGANIZATION_NAME.eq(organizationName).and(
                    XTAGES_USER.IS_OWNER.eq(true)
                )
            ).fetchOneInto(XtagesUser::class.java)
        },
        code = ExceptionCode.USER_NOT_FOUND,
        message = "Could not find [$organizationName] owner"
    )
}
