package xtages.console.pojo

import xtages.console.exception.ensure
import xtages.console.query.tables.pojos.Organization
import xtages.console.controller.model.CodeBuildType

/**
 * Returns the name of the CloudWatch Logs group to use in the CodeBuild project.
 */
fun Organization.codeBuildLogsGroupNameFor(codeBuildType: CodeBuildType): String
    {
        val name = ensure.notNull(value = name, valueDesc = "organization.name")
        return "${name}_${codeBuildType.name}_logs".toLowerCase()
    }
