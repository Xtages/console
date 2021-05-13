package xtages.console.dao

import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.BuildEventDao
import xtages.console.query.tables.pojos.BuildEvent
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
