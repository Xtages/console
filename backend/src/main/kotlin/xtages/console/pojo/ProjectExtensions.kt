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
        return "${type.name}_${version}_template".toLowerCase()
    }

/**
 * Returns the CodeBuild CI project name.
 */
val Project.codeBuildCiProjectName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "${organization}__${name}__ci"
    }

/**
 * Returns the CodeBuild CD project name.
 */
val Project.codeBuildCdProjectName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "${organization}__${name}__cd"
    }

/**
 * Returns the name of the image to use in the CodeBuild CI project.
 */
val Project.codeBuildCiImageName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type}_${version}_ci".toLowerCase()
    }

/**
 * Returns the name of the image to use in the CodeBuild CD project.
 */
val Project.codeBuildCdImageName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type}_${version}_cd".toLowerCase()
    }


/**
 * Returns the name of the buildspec file to use in the CodeBuild CD project.
 */
val Project.codeBuildCiBuildSpecName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type}_${version}_ci".toLowerCase()
    }

/**
 * Returns the name of the buildspec file to use in the CodeBuild CD project.
 */
val Project.codeBuildCdBuildSpecName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type}_${version}_cd".toLowerCase()
    }

/**
 * Returns the name of the CloudWatch Logs group to use in the CodeBuild CI project.
 */
val Project.codeBuildCiLogsGroupName: String
    get() {
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "${organization}_ci_logs"
    }

/**
 * Returns the name of the CloudWatch Logs group to use in the CodeBuild CD project.
 */
val Project.codeBuildCdLogsGroupName: String
    get() {
        val organization = ensure.notNull(value = organization, valueDesc = "project.organization")
        return "${organization}_cd_logs"
    }

/**
 * Returns the name of the CloudWatch Logs stream to use in the CodeBuild CI project.
 */
val Project.codeBuildCiLogsStreamName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        return "${name}_ci_logs"
    }

/**
 * Returns the name of the CloudWatch Logs stream to use in the CodeBuild CD project.
 */
val Project.codeBuildCdLogsStreamName: String
    get() {
        val name = ensure.notNull(value = name, valueDesc = "project.name")
        return "${name}_cd_logs"
    }
