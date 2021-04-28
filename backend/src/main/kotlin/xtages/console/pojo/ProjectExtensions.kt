package xtages.console.pojo

import xtages.console.exception.ensure
import xtages.console.query.tables.pojos.Project

val Project.templateRepoName: String
    get() {
        val type = ensure.notNull(value = type, valueDesc = "project.type")
        val version = ensure.notNull(value = version, valueDesc = "project.version")
        return "${type.name.toLowerCase()}_${version}_template"
    }
