package xtages.console.pojo

import xtages.console.exception.ensure
import xtages.console.query.tables.pojos.Project

/**
 * Returns the template repository name based on the [Project.type] and [Project.version] of the [Project].
 */
val Project.templateRepoName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type.name.toLowerCase()}_${version}_template"
    }

val Project.codeBuildCiProjectName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "${organization}__${name}__ci"
    }

val Project.codeBuildCdProjectName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "${organization}__${name}__cd"
    }

val Project.codeBuildCiImageName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type}_${version}_ci"
    }

val Project.codeBuildCdImageName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type}_${version}_cd"
    }


val Project.codeBuildCiBuildSpecName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type}_${version}_ci"
    }

val Project.codeBuildCdBuildSpecName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type}_${version}_cd"
    }

val Project.codeBuildCiLogsGroupName: String
    get() {
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "${organization}_ci_logs"
    }

val Project.codeBuildCdLogsGroupName: String
    get() {
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "${organization}_cd_logs"
    }

val Project.codeBuildCiLogsStreamName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        return "${name}_ci_logs"
    }

val Project.codeBuildCdLogsStreamName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        return "${name}_cd_logs"
    }
