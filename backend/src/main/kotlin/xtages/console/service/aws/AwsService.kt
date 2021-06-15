package xtages.console.service.aws

import org.springframework.stereotype.Service
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project
import xtages.console.query.tables.pojos.Recipe

@Service
class AwsService(
    private val ecrService: EcrService,
    private val cloudWatchLogsService: CloudWatchLogsService,
    private val codeBuildService: CodeBuildService,
) {
    /**
     * Creates a [Project]-related infrastructure in AWS. Specifically creates:
     *   * an ECR repository for the [organization] if one hasn't been created already
     *   * Cloud Watch Log groups for the [organization] if they haven't been created already
     *   * a CodeBuild project for CI
     *   * a CodeBuild project for CD
     */
    fun registerProject(project: Project, recipe: Recipe, organization: Organization) {
        ecrService.createEcrRepositoryForProject(organization = organization, project = project)
        registerOrganization(organization)
        codeBuildService.createCodeBuildCiProject(organization = organization, project = project, recipe = recipe)
        codeBuildService.createCodeBuildCdProject(organization = organization, project = project, recipe = recipe)
    }

    private fun registerOrganization(organization: Organization) {
        cloudWatchLogsService.maybeCreateLogGroupForOrganization(organization = organization)
    }

    fun unregisterProject(project: Project, organization: Organization) {
        TODO("(czuniga): Use the list below to implement this")
        // unregisterOrganization(organization = organization)
        // Remove CI CodeBuild Project
        //   * Remove Notification Rule (use project.codebuildCiNotificationRuleArn)
        //   * Remove Project  (use project.codebuildCiProjectArn)
        // Remove CD CodeBuild Project
        //   * Remove Notification Rule (use project.codebuildCdNotificationRuleArn)
        //   * Remove Project  (use project.codebuildCdProjectArn)
    }

    private fun unregisterOrganization(organization: Organization) {
        TODO("(czuniga): Use the list below to implement this")
        // Remove ECR repository - mark them to delete them after a period of time
        // Remove CI log group - move the logs to S3 in a cheap storage
        // Remove CD log group - same as above
    }
}
