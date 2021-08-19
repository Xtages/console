package xtages.console.dao

import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.rank
import xtages.console.controller.model.Environment
import xtages.console.query.enums.BuildType
import xtages.console.query.tables.daos.ProjectDeploymentDao
import xtages.console.query.tables.pojos.Build
import xtages.console.query.tables.pojos.ProjectDeployment
import xtages.console.query.tables.references.BUILD
import xtages.console.query.tables.references.ORGANIZATION
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

/**
 * Finds the latest [ProjectDeployment]s for [organizationName]. That is, for each project of [organizationName], for
 * each `environment`.
 */
fun ProjectDeploymentDao.fetchLatestDeploymentsByOrg(organizationName: String): List<ProjectDeployment> {
    val subQuery = ctx()
        .select(
            PROJECT_DEPLOYMENT.asterisk(),
            rank()
                .over()
                .partitionBy(PROJECT.ID, BUILD.ENVIRONMENT)
                .orderBy(PROJECT_DEPLOYMENT.STATUS_CHANGE_TIME.desc())
                .`as`("pos")
        ).from(ORGANIZATION)
        .join(PROJECT).on(ORGANIZATION.NAME.eq(PROJECT.ORGANIZATION))
        .join(PROJECT_DEPLOYMENT).on(PROJECT.ID.eq(PROJECT_DEPLOYMENT.PROJECT_ID))
        .join(BUILD).on(PROJECT_DEPLOYMENT.BUILD_ID.eq(BUILD.ID))
        .where(ORGANIZATION.NAME.eq(organizationName).and(BUILD.TYPE.eq(BuildType.CD)))
        .groupBy(PROJECT_DEPLOYMENT.ID, PROJECT.ID, BUILD.ENVIRONMENT)
        .asTable("subquery")
    return ctx()
        .select(subQuery.asterisk())
        .from(subQuery)
        .where(subQuery.field("pos", Int::class.java)!!.lessThan(2))
        .fetchInto(ProjectDeployment::class.java)
}
