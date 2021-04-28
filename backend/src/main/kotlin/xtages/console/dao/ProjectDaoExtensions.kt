package xtages.console.dao

import xtages.console.config.CognitoUserId
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.references.PROJECT

/**
 * Finds a [Project] given a name and an [Organization].
 */
fun ProjectDao.findByNameAndOrganization(project_name: String, org_name: String): Project? {
    return ctx().select(PROJECT.asterisk())
        .from(PROJECT)
        .where(PROJECT.NAME.eq(project_name).and(PROJECT.ORGANIZATION.eq(org_name)))
        .fetchOneInto(Project::class.java)
}
