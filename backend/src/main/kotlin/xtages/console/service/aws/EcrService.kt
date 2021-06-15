package xtages.console.service.aws

import mu.KotlinLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.ecr.EcrAsyncClient
import software.amazon.awssdk.services.ecr.model.*
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.ProjectDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Project

private val logger = KotlinLogging.logger { }
private val xtagesEcrTag: Tag = buildEcrTag(key = "XTAGES_CONSOLE_CREATED", value = "true")

/**
 * A [Service] to handle the logic to communicate to the AWS ECR service.
 */
@Service
class EcrService(
    private val ecrAsyncClient: EcrAsyncClient,
    private val projectDao: ProjectDao,
    ) {

    /**
     * Creates an `ECR` repository for the [project] if one hasn't already been created. The repository's name is
     * the [Project.hash].
     */
    //TODO(@mdellamerlina) add perms to the repo so ECR can pull it
    fun createEcrRepositoryForProject(organization: Organization, project: Project) {
        if (project.ecrRepositoryArn == null) {
            logger.debug { "Creating ECR repository for project: ${project.name}" }
            val orgName = ensure.notNull(value = organization.name, valueDesc = "organization.name")
            val orgHash = ensure.notNull(value = organization.hash, valueDesc = "organization.hash")
            val projectHash = ensure.notNull(value = project.hash, valueDesc = "project.hash")
            val createRepositoryResponse = ecrAsyncClient.createRepository(
                CreateRepositoryRequest.builder()
                    .repositoryName(projectHash)
                    .imageTagMutability(ImageTagMutability.IMMUTABLE)
                    .encryptionConfiguration(
                        EncryptionConfiguration.builder().encryptionType(EncryptionType.KMS).build()
                    )
                    .imageScanningConfiguration(ImageScanningConfiguration.builder()
                        .scanOnPush(true).build()
                    )
                    .tags(xtagesEcrTag,
                        buildEcrTag("organization", orgName),
                        buildEcrTag("organization-hash", orgHash)
                    )
                    .build()
            ).get()
            project.ecrRepositoryArn = createRepositoryResponse.repository().repositoryArn()
            projectDao.merge(project)
        }
    }
}

private fun buildEcrTag(key: String, value: String) = Tag.builder().key(key).value(value).build()
