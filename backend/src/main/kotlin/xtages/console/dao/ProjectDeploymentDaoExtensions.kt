package xtages.console.dao

import org.jooq.impl.DSL.max
import xtages.console.controller.model.Environment
import xtages.console.query.tables.daos.ProjectDeploymentDao
import xtages.console.query.tables.pojos.Build
import xtages.console.query.tables.pojos.ProjectDeployment
import xtages.console.query.tables.references.BUILD
import xtages.console.query.tables.references.PROJECT
import xtages.console.query.tables.references.PROJECT_DEPLOYMENT
import java.time.LocalDateTime

/***
 * Returns the lastest [ProjectDeployment] based on the [projectHash]
 * and the [environment]
 */
fun ProjectDeploymentDao.fetchLatestDeploymentStatus(
    projectHash: String,
    environment: Environment
): ProjectDeployment? {
    return ctx()
        .select(PROJECT_DEPLOYMENT.asterisk())
        .from(PROJECT_DEPLOYMENT)
        .join(PROJECT).on(PROJECT_DEPLOYMENT.PROJECT_ID.eq(PROJECT.ID))
        .join(BUILD).on(PROJECT_DEPLOYMENT.BUILD_ID.eq(BUILD.ID))
        .where(
            PROJECT.HASH.eq(projectHash)
                .and(BUILD.ENVIRONMENT.eq(environment.name.toLowerCase()))
        )
        .orderBy(PROJECT_DEPLOYMENT.STATUS_CHANGE_TIME.desc())
        .limit(1)
        .fetchOneInto(ProjectDeployment::class.java)
}

/***
 * Returns the latest [ProjectDeployment] based on the [builds] list
 */
fun ProjectDeploymentDao.fetchLatestByBuilds(builds: List<Build>): List<ProjectDeployment> {
    val latestDeployments = ctx()
        .select(
            PROJECT_DEPLOYMENT.BUILD_ID,
            max(PROJECT_DEPLOYMENT.STATUS_CHANGE_TIME).`as`("latest_time")
        )
        .from(PROJECT_DEPLOYMENT)
        .where(PROJECT_DEPLOYMENT.BUILD_ID.`in`(builds.map { it.id }))
        .groupBy(PROJECT_DEPLOYMENT.BUILD_ID).asTable("latest_build")
    return ctx()
        .select(
            PROJECT_DEPLOYMENT.asterisk()
        )
        .from(
            latestDeployments
        )
        .join(PROJECT_DEPLOYMENT)
        .on(PROJECT_DEPLOYMENT.STATUS_CHANGE_TIME.eq(latestDeployments.field("latest_time", LocalDateTime::class.java)))
        .fetchInto(ProjectDeployment::class.java)

}
