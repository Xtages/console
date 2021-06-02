package xtages.console.pojo

import xtages.console.exception.ensure
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.pojos.Recipe

/**
 * Returns the template repository name based on the [Recipe.projectType] and [Recipe.version] of the [Recipe]
 * associated to the [Project]
 */
val Recipe.templateRepoName: String
    get() {
        val projectType = ensure.notNull(value = projectType, valueDesc = "recipe.projectType")
        val version = ensure.notNull(value = version, valueDesc = "recipe.version")
        return "${projectType.name}_${version}_template".toLowerCase()
    }

/**
 * Returns the name of the image to use in the CodeBuild CI project.
 */
val Recipe.codeBuildCiImageName: String
    get() {
        val type = ensure.notNull(value = projectType, valueDesc = "recipe.projectType")
        val version = ensure.notNull(value = version, valueDesc = "recipe.version")
        return "${type}_ci:$version".toLowerCase()
    }

/**
 * Returns the name of the image to use in the CodeBuild CD project.
 */
val Recipe.codeBuildCdImageName: String
    get() {
        val type = ensure.notNull(value = projectType, valueDesc = "recipe.projectType")
        val version = ensure.notNull(value = version, valueDesc = "recipe.version")
        return "${type}_cd:$version".toLowerCase()
    }


/**
 * Returns the name of the buildspec file to use in the CodeBuild CD project.
 */
val Recipe.codeBuildCiBuildSpecName: String
    get() {
        val type = ensure.notNull(value = projectType, valueDesc = "recipe.projectType")
        val version = ensure.notNull(value = version, valueDesc = "recipe.version")
        return "ci/$type/$version-buildspec.yml".toLowerCase()
    }

/**
 * Returns the name of the buildspec file to use in the CodeBuild CD project.
 */
val Recipe.codeBuildCdBuildSpecName: String
    get() {
        val type = ensure.notNull(value = projectType, valueDesc = "recipe.projectType")
        val version = ensure.notNull(value = version, valueDesc = "recipe.version")
        return "cd/$type/$version-buildspec.yml".toLowerCase()
    }
