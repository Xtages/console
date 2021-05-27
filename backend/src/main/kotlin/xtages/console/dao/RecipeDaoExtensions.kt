package xtages.console.dao;

import org.springframework.cache.annotation.Cacheable
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.enums.ProjectType
import xtages.console.query.tables.daos.RecipeDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Recipe
import xtages.console.query.tables.references.ORGANIZATION
import xtages.console.query.tables.references.RECIPE
import xtages.console.query.tables.references.XTAGES_USER

/**
 * Fetch the most recent [Recipe] given a [ProjectType] and a version
 */
@Cacheable
fun RecipeDao.fetchBy(projectType: ProjectType, version: String): Recipe {
    return ensure.foundOne(
        operation = {
            ctx().select(RECIPE.asterisk())
                .from(RECIPE)
                .where(RECIPE.VERSION.eq(version).and(RECIPE.PROJECT_TYPE.eq(projectType)))
                .orderBy(RECIPE.ID.desc())
                .fetchOneInto(Recipe::class.java)
        },
        code = ExceptionCode.RECIPE_NOT_FOUND,
        message = "Recipe not found for project type ${projectType} and version ${version}"
    )
}
