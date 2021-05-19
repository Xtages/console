package xtages.console.dao

import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.select
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.BuildEventDao
import xtages.console.query.tables.pojos.BuildEvent
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.references.BUILD_EVENT

/**
 * Fetches a [BuildEvent] by [buildArn] and [name] and [status].
 */
fun BuildEventDao.fetchOneByBuildArnAndNameAndStatus(buildArn: String, name: String, status: String): BuildEvent {
    return ensure.foundOne(
        operation = {
            ctx().select(BUILD_EVENT.asterisk())
                .from(BUILD_EVENT)
                .where(
                    BUILD_EVENT.BUILD_ARN.eq(buildArn)
                        .and(BUILD_EVENT.NAME.eq(name))
                        .and(BUILD_EVENT.STATUS.eq(status))
                ).fetchOneInto(BuildEvent::class.java)
        },
        code = ExceptionCode.BUILD_EVENT_NOT_FOUND,
        message = "BuildEvent with arn [$buildArn] and name [$name] and [$status] not found"
    )
}

/**
 * Fetches the lastest groups of [BuildEvents] for [projects].
 *
 * @return A [Map<Project, List<BuildEvent>>] where every [Project] key corresponds to the list of [BuildEvent]s we have
 * received from CodeBuild.
 */
fun BuildEventDao.fetchLatestBuildEventsOfProjects(projects: List<Project>): Map<Project, List<BuildEvent>> {
    val projectIdToProject = projects.associateBy { it.id!! }
    val latestBuild = name("latest_build").`as`(
        select(BUILD_EVENT.PROJECT_ID, BUILD_EVENT.BUILD_ARN, BUILD_EVENT.START_TIME)
            .distinctOn(BUILD_EVENT.PROJECT_ID)
            .from(BUILD_EVENT)
            .where(
                BUILD_EVENT.PROJECT_ID.`in`(projectIdToProject.keys)
                    .and(
                        BUILD_EVENT.STATUS.eq("STARTED")
                            .and(BUILD_EVENT.NAME.eq("SENT_TO_BUILD"))
                    )
            )
            .orderBy(BUILD_EVENT.PROJECT_ID, BUILD_EVENT.START_TIME.desc(), BUILD_EVENT.BUILD_ARN)
    )
    return ctx()
        .with(latestBuild)
        .select(BUILD_EVENT.asterisk())
        .from(BUILD_EVENT)
        .join(latestBuild)
        .on(
            BUILD_EVENT.BUILD_ARN.eq(latestBuild.field(BUILD_EVENT.BUILD_ARN))
        ).fetchInto(BuildEvent::class.java)
        .groupBy { it.projectId!! }
        .mapKeys { projectIdToProject[it.key]!! }
}
