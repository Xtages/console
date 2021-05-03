package xtages.console.dao

import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.references.PROJECT

/**
 * Finds a [Project] given a name and an [Organization].
 */
fun ProjectDao.findByNameAndOrganization(projectName: String, orgName: String): Project? {
    return ctx().select(PROJECT.asterisk())
        .from(PROJECT)
        .where(PROJECT.NAME.eq(projectName).and(PROJECT.ORGANIZATION.eq(orgName)))
        .fetchOneInto(Project::class.java)
}
