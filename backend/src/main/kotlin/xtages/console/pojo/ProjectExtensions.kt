package xtages.console.pojo

import xtages.console.controller.model.CodeBuildType
import xtages.console.exception.ensure
import xtages.console.query.tables.pojos.Project

/**
 * Returns the template repository name based on the [Project.type] and [Project.version] of the [Project].
 */
val Project.templateRepoName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type.name}_${version}_template".toLowerCase()
    }

/**
 * Returns the CodeBuild CI project name.
 */
val Project.codeBuildCiProjectName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "${organization}_${name}_ci".toLowerCase()
    }

/**
 * Returns the CodeBuild CD project name.
 */
val Project.codeBuildCdProjectName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "${organization}_${name}_cd".toLowerCase()
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
 * Returns the name of the image to use in the CodeBuild CI project.
 */
val Project.codeBuildCiImageName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type}_ci:$version".toLowerCase()
    }

/**
 * Returns the name of the image to use in the CodeBuild CD project.
 */
val Project.codeBuildCdImageName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type}_cd:$version".toLowerCase()
    }


/**
 * Returns the name of the buildspec file to use in the CodeBuild CD project.
 */
val Project.codeBuildCiBuildSpecName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "ci/$type/$version-buildspec.yml".toLowerCase()
    }

/**
 * Returns the name of the buildspec file to use in the CodeBuild CD project.
 */
val Project.codeBuildCdBuildSpecName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "cd/$type/$version-buildspec.yml".toLowerCase()
    }

fun Project.codeBuildLogsStreamNameFor(codeBuildType: CodeBuildType): String {
    val name = ensure.notNull(value = name, valueDesc = "project.name")
    return "${name}_${codeBuildType.name}_logs".toLowerCase()
}

/**
 * Returns the name of the notifications rule to use in the CodeBuild CI project.
 */
val Project.codeBuildCiNotificationRuleName: String
    get() {
        return "${codeBuildCiProjectName}_build_events_notif_rule"
    }

/**
 * Returns the name of the notifications rule to use in the CodeBuild CD project.
 */
val Project.codeBuildCdNotificationRuleName: String
    get() {
        return "${codeBuildCdProjectName}_build_events_notif_rule"
    }
