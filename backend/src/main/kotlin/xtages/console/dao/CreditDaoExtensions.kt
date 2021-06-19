package xtages.console.dao

import xtages.console.query.tables.daos.CreditDao
import xtages.console.query.tables.pojos.Credit
import xtages.console.query.tables.references.CREDIT
import xtages.console.query.tables.references.ORGANIZATION
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Finds the active [Credit]s for [organizationName].
 */
fun CreditDao.fetchActiveCreditsByOrganizationName(organizationName: String): List<Credit> {
    val now = LocalDateTime.now(ZoneOffset.UTC)
    return ctx()
        .select(CREDIT.asterisk())
        .from(CREDIT)
        .join(ORGANIZATION).on(CREDIT.ORGANIZATION_NAME.eq(ORGANIZATION.NAME))
        .where(
            ORGANIZATION.NAME.eq(organizationName).and(
                CREDIT.CREATED_TIME.lessOrEqual(now).and(
                    CREDIT.EXPIRY_TIME.isNull.or(
                        CREDIT.EXPIRY_TIME.greaterOrEqual(now)
                    )
                )
            )
        )
        .fetchInto(Credit::class.java)
}
