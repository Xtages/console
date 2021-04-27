package xtages.console.service

import org.springframework.stereotype.Service
import xtages.console.query.tables.pojos.Project

@Service
class AwsService {
    fun registerProject(project: Project) {
        createEcrForProject(project = project)
        createCodeBuildProject(project = project)
    }

    private fun createCodeBuildProject(project: Project) {
        TODO("(czuniga): Not yet implemented")
    }

    private fun createEcrForProject(project: Project) {
        TODO("(czuniga): Not yet implemented")
    }
}
