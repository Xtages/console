package xtages.console.dao

import xtages.console.controller.model.Environment
import xtages.console.query.tables.daos.ProjectDeploymentDao
import xtages.console.query.tables.pojos.Build
import xtages.console.query.tables.pojos.ProjectDeployment
import xtages.console.query.tables.references.BUILD
import xtages.console.query.tables.references.PROJECT
import xtages.console.query.tables.references.PROJECT_DEPLOYMENT

/***
 * Returns the lastest [ProjectDeployment] based on the [projectHash]
 * and the [environment]
 */
fun ProjectDeploymentDao.fetchLatestDeploymentStatus(
    projectHash: String,
    environment: Environment
): ProjectDeployment {
    return ctx()
        .select(PROJECT_DEPLOYMENT.asterisk())
        .from(PROJECT_DEPLOYMENT)
        .join(PROJECT).on(PROJECT_DEPLOYMENT.PROJECT_ID.eq(PROJECT.ID))
        .join(BUILD).on(PROJECT_DEPLOYMENT.BUILD_ID.eq(BUILD.ID))
        .where(
            PROJECT.HASH.eq(projectHash)
                .and(BUILD.ENVIRONMENT.eq(environment.name.toLowerCase()))
        )
        .orderBy(PROJECT_DEPLOYMENT.START_TIME.desc())
        .limit(1)
        .fetchOneInto(ProjectDeployment::class.java)!!
}

/***
 * Returns the latest [ProjectDeployment] based on the [builds] list
 */
fun ProjectDeploymentDao.fetchByLatestBuild(builds: List<Build>): List<ProjectDeployment> {
    return ctx()
        .select(
            PROJECT_DEPLOYMENT.BUILD_ID, PROJECT_DEPLOYMENT.START_TIME,
            PROJECT_DEPLOYMENT.STATUS, PROJECT_DEPLOYMENT.ID
        )
        .from(PROJECT_DEPLOYMENT)
        .where(
            PROJECT_DEPLOYMENT.BUILD_ID.`in`(builds)
        )
        .groupBy(
            PROJECT_DEPLOYMENT.BUILD_ID, PROJECT_DEPLOYMENT.START_TIME,
            PROJECT_DEPLOYMENT.STATUS, PROJECT_DEPLOYMENT.ID
        )
        .orderBy(PROJECT_DEPLOYMENT.START_TIME.desc())
        .fetchInto(ProjectDeployment::class.java)

}
