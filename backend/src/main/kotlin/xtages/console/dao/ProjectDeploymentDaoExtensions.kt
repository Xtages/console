package xtages.console.dao

import org.jooq.impl.DSL.rank
import xtages.console.controller.model.Environment
import xtages.console.query.enums.BuildType
import xtages.console.query.tables.daos.ProjectDeploymentDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.pojos.ProjectDeployment
import xtages.console.query.tables.references.BUILD
import xtages.console.query.tables.references.ORGANIZATION
import xtages.console.query.tables.references.PROJECT
import xtages.console.query.tables.references.PROJECT_DEPLOYMENT

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

/**
 * @return The latest two [ProjectDeployment]s for [project] per by `environment`.
 */
fun ProjectDeploymentDao.fetchLatestDeploymentsByProject(
    project: Project
): List<ProjectDeployment> {
    // This sub query, groups project_deployment rows by build.id and build.environment and ranks each group sorted by
    // status_change_time.
    val subQueryGroupedByBuildAndEnv = ctx()
        .select(
            PROJECT_DEPLOYMENT.asterisk(),
            BUILD.ENVIRONMENT,
            rank()
                .over()
                .partitionBy(BUILD.ID, BUILD.ENVIRONMENT)
                .orderBy(PROJECT_DEPLOYMENT.STATUS_CHANGE_TIME.desc())
                .`as`("pos")
        )
        .from(PROJECT_DEPLOYMENT)
        .join(PROJECT).on(PROJECT_DEPLOYMENT.PROJECT_ID.eq(PROJECT.ID))
        .join(BUILD).on(PROJECT_DEPLOYMENT.BUILD_ID.eq(BUILD.ID))
        .where(PROJECT.ID.eq(project.id!!))
        .orderBy(PROJECT_DEPLOYMENT.STATUS_CHANGE_TIME.desc())
        .asTable()
    // This sub query, takes the previous sub query and for each group of (build.id, build.environment) selects the
    // first row and subsequently groups those rows by (build.environment), meaning that by the end we will have the
    // last 2 project_deployments for each environment.
    val subTableGroupedByEnv = ctx()
        .select(
            subQueryGroupedByBuildAndEnv.asterisk(),
            rank()
                .over()
                .partitionBy(subQueryGroupedByBuildAndEnv.field(BUILD.ENVIRONMENT.unqualifiedName))
                .orderBy(subQueryGroupedByBuildAndEnv.field(PROJECT_DEPLOYMENT.STATUS_CHANGE_TIME.unqualifiedName)!!.desc())
                .`as`("pos2")
        )
        .from(subQueryGroupedByBuildAndEnv)
        .where(subQueryGroupedByBuildAndEnv.field("pos", Int::class.java)!!.eq(1))
        .orderBy(subQueryGroupedByBuildAndEnv.field(PROJECT_DEPLOYMENT.STATUS_CHANGE_TIME.unqualifiedName)!!.desc())
        .asTable()
    return ctx()
        .select(subTableGroupedByEnv.asterisk())
        .from(subTableGroupedByEnv)
        .where(subTableGroupedByEnv.field("pos2", Int::class.java)!!.lessOrEqual(2))
        .orderBy(
            subTableGroupedByEnv.field(BUILD.ENVIRONMENT.unqualifiedName),
            subTableGroupedByEnv.field(PROJECT_DEPLOYMENT.STATUS_CHANGE_TIME.unqualifiedName)!!.desc())
        .fetchInto(ProjectDeployment::class.java)
}

/**
 * Finds the latest [ProjectDeployment]s for [organization]. That is, for each project of [organization], for
 * each `environment`.
 */
fun ProjectDeploymentDao.fetchLatestDeploymentsByOrg(organization: Organization): List<ProjectDeployment> {
    val subQuery = ctx()
        // Each row in this query will the `project_deployment` details plus their "pos" (position) in their respective
        // partition. We will then use the "pos" to get the first row for each partition, in the `WHERE` of the outer
        // `SELECT`.
        .select(
            PROJECT_DEPLOYMENT.asterisk(),
            // This following statement is going to partition the rows by project and environment and order each
            // partition by `project_deployment.status_change_time`, and alias the racking of each row withing their
            // partition to a column called 'pos'.
            rank()
                .over()
                .partitionBy(PROJECT.ID, BUILD.ENVIRONMENT)
                .orderBy(PROJECT_DEPLOYMENT.STATUS_CHANGE_TIME.desc())
                .`as`("pos")
        ).from(ORGANIZATION)
        .join(PROJECT).on(ORGANIZATION.NAME.eq(PROJECT.ORGANIZATION))
        .join(PROJECT_DEPLOYMENT).on(PROJECT.ID.eq(PROJECT_DEPLOYMENT.PROJECT_ID))
        .join(BUILD).on(PROJECT_DEPLOYMENT.BUILD_ID.eq(BUILD.ID))
        .where(ORGANIZATION.NAME.eq(organization.name!!).and(BUILD.TYPE.eq(BuildType.CD)))
        .groupBy(PROJECT_DEPLOYMENT.ID, PROJECT.ID, BUILD.ENVIRONMENT)
        .asTable("subquery")
    return ctx()
        .select(subQuery.asterisk())
        .from(subQuery)
        .where(subQuery.field("pos", Int::class.java)!!.lessThan(2))
        .fetchInto(ProjectDeployment::class.java)
}

/**
 * Insert a [ProjectDeployment] if there is no conflict. A conflict could happen if there is a record with the same
 * build_id and [DeployStatus]
 */
fun ProjectDeploymentDao.insertIfNotExist(projectDeployment: ProjectDeployment) {
    ctx().insertInto(PROJECT_DEPLOYMENT)
        .columns(
            PROJECT_DEPLOYMENT.BUILD_ID,
            PROJECT_DEPLOYMENT.STATUS,
            PROJECT_DEPLOYMENT.PROJECT_ID,
        )
        .values(
            projectDeployment.buildId,
            projectDeployment.status,
            projectDeployment.projectId
        )
        .onConflict()
        .doNothing()
        .execute()
}
