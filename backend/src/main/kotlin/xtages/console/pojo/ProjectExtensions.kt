package xtages.console.pojo

import xtages.console.controller.model.CodeBuildType
import xtages.console.exception.ensure
import xtages.console.query.tables.pojos.Project

/**
 * Returns the CodeBuild CI project name.
 */
val Project.codeBuildCiProjectName: String
    get() {
        val hash = ensure.notNull(value = hash, valueDesc = "project.hash")
        return "${hash}_ci"
    }

/**
 * Returns the CodeBuild CD project name.
 */
val Project.codeBuildCdProjectName: String
    get() {
        val hash = ensure.notNull(value = hash, valueDesc = "project.hash")
        return "${hash}_cd"
    }


/**
 * Returns the CodeBuild CI project description.
 */
val Project.codeBuildCiProjectDescription: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "CI for $organization/$name"
    }

/**
 * Returns the CodeBuild CD project description.
 */
val Project.codeBuildCdProjectDescription: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "CD for $organization/$name"
    }

/**
 * Returns the name of the log stream associated to the CodeBuild project given the
 * [CodeBuildType]
 */
fun Project.codeBuildLogsStreamNameFor(codeBuildType: CodeBuildType): String {
    val hash = ensure.notNull(value = hash, valueDesc = "project.hash")
    return "${hash}_${codeBuildType.name}_logs".toLowerCase()
}

/**
 * Returns the name of the notifications rule to use in the CodeBuild CI project.
 */
val Project.codeBuildCiNotificationRuleName: String
    get() = "${codeBuildCiProjectName}_build_events_notif_rule"

/**
 * Returns the name of the notifications rule to use in the CodeBuild CD project.
 */
val Project.codeBuildCdNotificationRuleName: String
    get() = "${codeBuildCdProjectName}_build_events_notif_rule"

/**
 * Returns the CloudWatch LogGroup name for this projec.
 */
val Project.ecsLogGroupName: String
    get() = "/ecs/${hash}"
