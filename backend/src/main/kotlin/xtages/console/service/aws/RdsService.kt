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
import xtages.console.dao.fetchLatestByOrganizationName
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.PlanDao
import xtages.console.query.tables.pojos.Organization
import software.amazon.awssdk.services.rds.model.Tag as TagRds

private val logger = KotlinLogging.logger { }
private val charsNotAllowedInPasswordRegex: Regex = Regex("/|\"|@|\\s")

/**
 * A service to handle the logic to communicate with AWS RDS
 */
@Service
class RdsService(
    private val rdsAsyncClient: RdsAsyncClient,
    private val ssmAsyncClient: SsmAsyncClient,
    private val consoleProperties: ConsoleProperties,
    private val planDao: PlanDao,
    private val organizationDao: OrganizationDao,
) {
    /**
     * Async operation to not slow down the Organization setup
     */
    fun provision(organization: Organization) {
        val plan = planDao.fetchLatestByOrganizationName(organization.name!!)?.plan
        val password = createAndStorePassInSsm(organization)

        val instanceRequest = CreateDbInstanceRequest.builder()
            .dbInstanceIdentifier("db-${organization.hash}")
            .masterUserPassword(password)
            .allocatedStorage(plan?.dbStorageGbs!!.toInt())
            .dbName(organization.name)
            .engine(consoleProperties.aws.rds.engine)
            .dbInstanceClass(plan.dbInstance)
            .engineVersion(consoleProperties.aws.rds.engineVersion)
            .storageType(consoleProperties.aws.rds.storageType)
            .masterUsername("u${organization.hash?.substring(IntRange(0,15))}")
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
            val result = rdsAsyncClient.createDBInstance(instanceRequest).get()
            organization.rdsArn = result.dbInstance().dbInstanceArn()
            organizationDao.merge(organization)
        } catch (e: RdsException) {
            logger.error {
                "There was an error while provisioning the DB for organization: ${organization.name}. Plan id used: ${plan.id}"
            }
            logger.error(e) {}
            throw e;
        }
    }

    fun getEndpoint(organization: Organization): String? {
        val rdsResponse = rdsAsyncClient.describeDBInstances(
            DescribeDbInstancesRequest
                .builder()
                .dbInstanceIdentifier(organization.hash)
                .build()
        ).get()

        return rdsResponse.dbInstances().firstOrNull()?.endpoint()?.address()
    }

    /**
     * Creates and store in SSM the password used for the RDS instance
     * The path for SSM is build as follow:
     * [ConsoleProperties.aws.rds.ssmPrefix] + [organization.hash] + /rds/password
     */
    private fun createAndStorePassInSsm(organization: Organization): String {
        val password = RandomStringUtils.randomAscii(25).replace(charsNotAllowedInPasswordRegex, "")
        organization.ssmDbPassPath = "${consoleProperties.aws.rds.ssmPrefix}${organization.hash}/rds/password"
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
