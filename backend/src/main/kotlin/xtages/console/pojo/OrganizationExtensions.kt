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

val Organization.dbUsername: String
    get() = "u${hash?.substring(0, 15)}"

val Organization.dbIdentifier: String
    get() = "db-$hash"
