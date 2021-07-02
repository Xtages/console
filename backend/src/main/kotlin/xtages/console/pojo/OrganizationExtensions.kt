package xtages.console.pojo

import xtages.console.exception.ensure
import xtages.console.query.tables.pojos.Organization
import xtages.console.controller.model.CodeBuildType
import xtages.console.controller.model.MD5

/**
 * Returns the name of the CloudWatch Logs group to use in the CodeBuild project.
 */
fun Organization.codeBuildLogsGroupNameFor(codeBuildType: CodeBuildType): String {
    val hash = ensure.notNull(value = hash, valueDesc = "organization.hash")
    return "${hash}_${codeBuildType.name}_logs".toLowerCase()
}

fun Organization.dbUsername(): String {
    return "u${hash?.substring(0,15)}"
}

fun Organization.dbIdentifier(): String {
    return "db-$hash"
}
