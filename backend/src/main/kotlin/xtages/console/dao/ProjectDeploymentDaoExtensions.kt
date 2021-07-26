package xtages.console.dao

import xtages.console.controller.model.Environment
import xtages.console.query.tables.daos.ProjectDeploymentDao
import xtages.console.query.tables.pojos.ProjectDeployment
import xtages.console.query.tables.references.BUILD
import xtages.console.query.tables.references.PROJECT
import xtages.console.query.tables.references.PROJECT_DEPLOYMENT

fun ProjectDeploymentDao.fetchLatestDeploymentStatus(
    projectName: String,
    environment: Environment
): ProjectDeployment {
    return ctx()
        .select(PROJECT_DEPLOYMENT.asterisk())
        .from(PROJECT_DEPLOYMENT)
        .join(PROJECT).on(PROJECT_DEPLOYMENT.PROJECT_ID.eq(PROJECT.ID))
        .join(BUILD).on(PROJECT_DEPLOYMENT.BUILD_ID.eq(BUILD.ID))
        .where(
            PROJECT.HASH.eq(projectName)
                .and(BUILD.ID.eq(
                    ctx()
                    .select(BUILD.ID)
                    .from(BUILD)
                    .where(
                        BUILD.PROJECT_ID.eq(PROJECT.ID)
                            .and(BUILD.ENVIRONMENT.eq(environment.name.toLowerCase()))
                    )
                    .orderBy(BUILD.START_TIME.desc())
                    .limit(1)
                ))
        )
        .orderBy(PROJECT_DEPLOYMENT.START_TIME.desc())
        .limit(1)
        .fetchOneInto(ProjectDeployment::class.java)!!
}

