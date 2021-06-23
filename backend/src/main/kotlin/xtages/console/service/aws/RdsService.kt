package xtages.console.service.aws

import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.rds.RdsAsyncClient
import software.amazon.awssdk.services.rds.model.CreateDbInstanceRequest
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest
import software.amazon.awssdk.services.rds.model.RdsException
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.ParameterType
import software.amazon.awssdk.services.ssm.model.PutParameterRequest
import software.amazon.awssdk.services.ssm.model.Tag
import xtages.console.config.ConsoleProperties
import xtages.console.dao.findPlanBy
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.OrganizationToPlanDao
import xtages.console.query.tables.pojos.Organization
import software.amazon.awssdk.services.rds.model.Tag as TagRds

private val logger = KotlinLogging.logger { }

/**
 * A service to handle the logic to communicate with AWS RDS
 */
@Service
class RdsService (
    private val rdsAsyncClient: RdsAsyncClient,
    private val ssmAsyncClient: SsmAsyncClient,
    private val consoleProperties: ConsoleProperties,
    private val organizationToPlanDao: OrganizationToPlanDao,
    private val organizationDao: OrganizationDao,
    private val charsNotAllowInPassword: String = "/|\"|@|\\s",
){
    /**
     * Async operation to not slow down the Organization setup
     */
    fun provision(organization: Organization) {
        val plan = organizationToPlanDao.findPlanBy(organization)
        val password = createAndStorePassInSsm(organization)

        val instanceRequest = CreateDbInstanceRequest.builder()
            .dbInstanceIdentifier(organization.hash)
            .masterUserPassword(password)
            .allocatedStorage(plan.dbStorageGbs!!.toInt())
            .dbName(organization.name)
            .engine(consoleProperties.aws.rds.engine)
            .dbInstanceClass(plan.dbInstance)
            .engineVersion(consoleProperties.aws.rds.engineVersion)
            .storageType(consoleProperties.aws.rds.storageType)
            .masterUsername(organization.hash)
            .vpcSecurityGroupIds(consoleProperties.aws.rds.dbSecurityGroup)
            .backupRetentionPeriod(consoleProperties.aws.rds.backupRetentionPeriod)
            .storageEncrypted(consoleProperties.aws.rds.storageEncrypted)
            .kmsKeyId(consoleProperties.aws.rds.kmsKeyId)
            .enablePerformanceInsights(consoleProperties.aws.rds.enablePerformanceInsights)
            .performanceInsightsRetentionPeriod(consoleProperties.aws.rds.performanceInsightsRetentionPeriod)
            .publiclyAccessible(consoleProperties.aws.rds.publiclyAccessible)
            .dbSubnetGroupName(consoleProperties.aws.rds.dbSubnetGroupName)
            .tags(
                buildRdsTag("organization", organization.name!!),
                buildRdsTag("organization-hash", organization.hash!!)
            )
            .build()
        try {
            rdsAsyncClient.createDBInstance(instanceRequest).get()
        } catch (e: RdsException) {
            logger.error { "There was an error while provisioning the DB for " +
                    "organization: ${organization.name}. Plan id used: ${plan.id}" }
            logger.error { e.localizedMessage }
        }
    }

    fun getEndpoint(organization: Organization): String? {
        val rdsResponse = rdsAsyncClient.describeDBInstances(
            DescribeDbInstancesRequest
                .builder()
                    .dbInstanceIdentifier(organization.hash)
                    .build()
        ).get()
        if (rdsResponse.hasDbInstances() && rdsResponse.dbInstances().first().endpoint() != null) {
            return rdsResponse.dbInstances().first().endpoint().address()
        }

        return null
    }

    /**
     * Creates and store in SSM the password used for the RDS instance
     * The path for SSM is build as follow:
     * [ConsoleProperties.aws.rds.ssmPrefix] + [organization.hash] + /rds/password
     */
    private fun createAndStorePassInSsm(organization: Organization): String {
        val regexRemoveChars = Regex(charsNotAllowInPassword)
        val password = RandomStringUtils.randomAscii(25).replace(regexRemoveChars,"")
        organization.ssmDbPassPath =  "${consoleProperties.aws.rds.ssmPrefix}${organization.hash}/rds/password"
        val putParameterRequest = PutParameterRequest.builder()
            .name(organization.ssmDbPassPath)
            .type(ParameterType.SECURE_STRING)
            .keyId("alias/aws/ssm")
            .value(password)
            .tags(
                buildSsmTag("organization", organization.name!!),
                buildSsmTag("organization-hash", organization.hash!!)
            )
            .build()

        ssmAsyncClient.putParameter(putParameterRequest).get()
        organizationDao.merge(organization)
        return password
    }
}

private fun buildSsmTag(key: String, value: String) = Tag.builder().key(key).value(value).build()
private fun buildRdsTag(key: String, value: String) = TagRds.builder().key(key).value(value).build()
