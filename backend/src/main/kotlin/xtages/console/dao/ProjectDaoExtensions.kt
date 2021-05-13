package xtages.console.dao

import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.references.PROJECT

/**
 * Finds a [Project] given a name and an [Organization].
 */
fun ProjectDao.fetchOneByNameAndOrganization(projectName: String, orgName: String): Project {
    return ensure.foundOne(
        operation = {
            ctx().select(PROJECT.asterisk())
                .from(PROJECT)
                .where(PROJECT.NAME.eq(projectName).and(PROJECT.ORGANIZATION.eq(orgName)))
                .fetchOneInto(Project::class.java)
        },
        code = ExceptionCode.PROJECT_NOT_FOUND,
        message = "Could not find project in organization"
    )
}
