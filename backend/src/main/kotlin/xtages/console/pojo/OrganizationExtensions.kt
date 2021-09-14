package xtages.console.pojo

import xtages.console.controller.model.CodeBuildType
import xtages.console.exception.ensure
import xtages.console.query.tables.pojos.Organization

/**
 * Returns the name of the CloudWatch Logs group to use in the CodeBuild project.
 */
fun Organization.codeBuildLogsGroupNameFor(codeBuildType: CodeBuildType): String {
    val hash = ensure.notNull(value = hash, valueDesc = "organization.hash")
    return "${hash}_${codeBuildType.name}_logs".toLowerCase()
}

/**
 * Name of the database (inside the RDS PostgreSQL instance) created for `this` [Organization].
 */
val Organization.dbName: String
    // The DB name must be at most 63 chars long and may only contain underscores and alphanumeric characters.
    get() = "db_${name!!.replace("-", "_").replace(Regex("[^A-Za-z0-9_]"), "").take(63)}"

/**
 * Name of the username for the RDS PostgreSQL instance created for `this` [Organization].
 */
val Organization.dbUsername: String
    get() = "u${hash?.substring(0, 15)}"

/**
 * Identifier of the RDS PostgreSQL instance created for `this` [Organization].
 */
val Organization.dbIdentifier: String
    get() = "db-$hash"
