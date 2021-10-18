package xtages.console.dao

import org.springframework.cache.annotation.Cacheable
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.references.PROJECT

/**
 * Finds a [Project] given a name and an [Organization].
 */
@Cacheable
fun ProjectDao.fetchOneByNameAndOrganization(organization: Organization, projectName: String): Project {
    return ensure.foundOne(
        operation = {
           maybeFetchOneByNameAndOrganization(organization = organization, projectName = projectName)
        },
        code = ExceptionCode.PROJECT_NOT_FOUND,
        message = "Could not find project [$projectName] in organization [${organization.name}]"
    )
}


fun ProjectDao.maybeFetchOneByNameAndOrganization(organization: Organization, projectName: String): Project? {
    return ctx().select(PROJECT.asterisk())
        .from(PROJECT)
        .where(PROJECT.NAME.eq(projectName).and(PROJECT.ORGANIZATION.eq(organization.name!!)))
        .fetchOneInto(Project::class.java)
}
