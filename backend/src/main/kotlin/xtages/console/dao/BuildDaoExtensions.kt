package xtages.console.dao

import xtages.console.query.tables.daos.BuildDao
import xtages.console.query.tables.pojos.Build
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.references.BUILD

/**
 * Fetches the latest [Build]s for [projects].
 */
fun BuildDao.fetchLatestByProject(projects: List<Project>): List<Build> {
    val projectIds = projects.mapNotNull { project -> project.id }
    return ctx().select(BUILD.asterisk()).distinctOn(BUILD.PROJECT_ID)
        .from(BUILD)
        .where(BUILD.PROJECT_ID.`in`(projectIds))
        .orderBy(
            BUILD.PROJECT_ID, BUILD.START_TIME.desc(),
        ).fetchInto(Build::class.java)
}
