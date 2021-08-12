package xtages.console.dao

import xtages.console.query.tables.daos.GithubUserDao
import xtages.console.query.tables.pojos.Build
import xtages.console.query.tables.pojos.GithubUser

/**
 * Finds all the [GithubUser]s from a list of [Build]s.
 */
fun GithubUserDao.findFromBuilds(vararg builds: Build): Map<String, GithubUser> {
    val githubUserNames = builds.mapNotNull { build -> build.githubUserUsername }
    return when {
        githubUserNames.isNotEmpty() -> fetchByEmail(*githubUserNames.toSet().toTypedArray())
            .associateBy { githubUser -> githubUser.username!! }
        else -> emptyMap()
    }
}

/**
 * Finds all the [GithubUser]s from a list of [Build]s.
 */
fun GithubUserDao.findFromBuilds(builds: List<Build>): Map<String, GithubUser> = findFromBuilds(*builds.toTypedArray())
