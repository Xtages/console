package xtages.console.service.aws

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.ecr.EcrAsyncClient
import software.amazon.awssdk.services.ecr.model.CreateRepositoryRequest
import software.amazon.awssdk.services.ecr.model.ImageTagMutability
import software.amazon.awssdk.services.ecr.model.Tag
import xtages.console.exception.ensure
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.pojos.Organization

private val xtagesEcrTag: Tag = buildEcrTag(key = "XTAGES_CONSOLE_CREATED", value = "true")

/**
 * A [Service] to handle the logic to communicate to the AWS ECR service.
 */
@Service
class EcrService(private val ecrAsyncClient: EcrAsyncClient, private val organizationDao: OrganizationDao) {

    /**
     * Creates an `ECR` repository for the [organization] if one hasn't already been created. The repository's name is
     * the [organization.name] in lower case.
     */
    fun maybeCreateEcrRepositoryForOrganization(organization: Organization) {
        if (organization.ecrRepositoryArn == null) {
            val name = ensure.notNull(value = organization.name, valueDesc = "organization.name")
            val createRepositoryResponse = ecrAsyncClient.createRepository(
                CreateRepositoryRequest.builder()
                    .repositoryName(name.toLowerCase())
                    .imageTagMutability(ImageTagMutability.IMMUTABLE)
                    .tags(xtagesEcrTag, buildEcrTag("organization", name))
                    .build()
            ).get()
            organization.ecrRepositoryArn = createRepositoryResponse.repository().repositoryArn()
            organizationDao.merge(organization)
        }
    }
}

private fun buildEcrTag(key: String, value: String) = Tag.builder().key(key).value(value).build()
