package xtages.console.dao

import org.jooq.SelectOnConditionStep
import org.springframework.cache.annotation.Cacheable
import xtages.console.config.CognitoUserId
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.enums.BuildStatus
import xtages.console.query.enums.BuildType
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.references.*

/**
 * Finds an [Organization] by its owner's [CognitoUserId].
 */
@Cacheable
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

fun OrganizationDao.fetchAllProjectsDeployedIn(organization: Organization): List<Project> {
    return ctx().select(PROJECT.asterisk())
        .from(PROJECT)
        .join(BUILD).on(BUILD.PROJECT_ID.eq(PROJECT.ID))
        .where(
            PROJECT.ORGANIZATION.eq(organization.name)
                .and(
                    BUILD.TYPE.eq(BuildType.CD)
                        .and(BUILD.STATUS.eq(BuildStatus.SUCCEEDED))
                )
        )
        .groupBy(PROJECT.ID)
        .fetchInto(Project::class.java)
}
