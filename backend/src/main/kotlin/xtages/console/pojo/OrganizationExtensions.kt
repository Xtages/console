package xtages.console.pojo

import xtages.console.exception.ensure
import xtages.console.query.tables.pojos.Organization

/**
 * Returns the name of the CloudWatch Logs group to use in the CodeBuild CI project.
 */
val Organization.codeBuildCiLogsGroupName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "organization.name")
        return "${name}_ci_logs".toLowerCase()
    }

/**
 * Returns the name of the CloudWatch Logs group to use in the CodeBuild CD project.
 */
val Organization.codeBuildCdLogsGroupName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "organization.name")
        return "${name}_cd_logs".toLowerCase()
    }
