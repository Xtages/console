package xtages.console.service.aws

import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.rds.RdsAsyncClient
import software.amazon.awssdk.services.rds.model.*
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.*
import software.amazon.awssdk.services.ssm.model.AddTagsToResourceRequest
import xtages.console.config.ConsoleProperties
import xtages.console.dao.fetchLatestByOrganizationName
import xtages.console.pojo.dbIdentifier
import xtages.console.pojo.dbName
import xtages.console.pojo.dbUsername
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.PlanDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Plan
import java.util.concurrent.ExecutionException
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

    fun provisionDb(organization: Organization, plan: Plan) {
        when (plan.paid!!) {
            true -> provisionServerless(organization)
            false -> provisionDbInstance(organization)
        }
    }
    /**
     * Async operation that provision an Aurora Serverless cluster asynchronously
     * By default the cluster will have a min of 2 ACUs and a max of 4 ACUs
     * That's similar to 2 vCPU and 2 GB of RAM and 4 vCPU and 4 GB of RAM
     * Also, by default the auto pause will be enable
     */
    private fun provisionServerless(organization: Organization) {
        val plan = planDao.fetchLatestByOrganizationName(organization.name!!)?.plan!!
        val password = createAndStorePassInSsm(organization)
        val cluster = CreateDbClusterRequest.builder()
            .dbClusterIdentifier(organization.dbIdentifier)
            .databaseName(organization.dbName)
            .masterUserPassword(password)
            .engine(consoleProperties.aws.rds.postgres.serverless.engine)
            .engineVersion(consoleProperties.aws.rds.postgres.serverless.engineVersion)
            .storageEncrypted(consoleProperties.aws.rds.storageEncrypted)
            .kmsKeyId(consoleProperties.aws.rds.kmsKeyId)
            .masterUsername(organization.dbUsername)
            .vpcSecurityGroupIds(consoleProperties.aws.rds.dbSecurityGroup)
            .backupRetentionPeriod(consoleProperties.aws.rds.backupRetentionPeriod)
            .engineMode(consoleProperties.aws.rds.postgres.serverless.engineMode)
            .dbSubnetGroupName(consoleProperties.aws.rds.dbSubnetGroupName)
            .scalingConfiguration(
                ScalingConfiguration.builder()
                    .minCapacity(consoleProperties.aws.rds.postgres.serverless.scaling.minCapacity)
                    .maxCapacity(consoleProperties.aws.rds.postgres.serverless.scaling.maxCapacity)
                    .autoPause(consoleProperties.aws.rds.postgres.serverless.scaling.autoPauseEnable)
                    .secondsUntilAutoPause(consoleProperties.aws.rds.postgres.serverless.scaling.secondsUntilAutoPause)
                    .build()
            )
            .tags(
                buildRdsTag("organization", organization.name!!),
                buildRdsTag("organization-hash", organization.hash!!)
            )
            .build()
        try {
            val result = rdsAsyncClient.createDBCluster(cluster).get()
            organization.rdsArn = result.dbCluster().dbClusterArn()
            organizationDao.merge(organization)
        } catch (e: RdsException) {
            logger.error {
                "There was an error while provisioning the DB for organization: ${organization.name}. Plan id used: ${plan.id}"
            }
            logger.error(e) {}
            throw e;
        }
    }

    /**
     * Provisions a classic RDS instance with Postgres
     */
    private fun provisionDbInstance(organization: Organization) {
        val plan = planDao.fetchLatestByOrganizationName(organization.name!!)?.plan!!
        val password = createAndStorePassInSsm(organization)
        val instanceRequest = CreateDbInstanceRequest.builder()
            .dbInstanceIdentifier(organization.dbIdentifier)
            .masterUserPassword(password)
            .allocatedStorage(plan?.dbStorageGbs!!.toInt())
            .dbName(organization.dbName)
            .engine(consoleProperties.aws.rds.postgres.instance.engine)
            .dbInstanceClass(plan.dbInstance)
            .engineVersion(consoleProperties.aws.rds.postgres.instance.engineVersion)
            .storageType(consoleProperties.aws.rds.storageType)
            .masterUsername(organization.dbUsername)
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
                .dbInstanceIdentifier(organization.dbIdentifier)
                .build()
        ).get()

        return rdsResponse.dbInstances().firstOrNull()?.endpoint()?.address()
    }

    fun dbInstanceExists(organization: Organization, paid: Boolean): Boolean {
        if (paid) {
            // serverless
            try {
                rdsAsyncClient.describeDBClusters(
                    DescribeDbClustersRequest
                        .builder()
                        .dbClusterIdentifier(organization.dbIdentifier)
                        .build()
                ).get()
            } catch (e: ExecutionException) {
                if (e.cause is DbClusterNotFoundException) {
                    return false
                } else {
                    throw e
                }
            }
        } else {
            // db instance
            try {
                rdsAsyncClient.describeDBInstances(
                    DescribeDbInstancesRequest
                        .builder()
                        .dbInstanceIdentifier(organization.dbIdentifier)
                        .build()
                ).get()
            } catch (e: ExecutionException) {
                if (e.cause is DbInstanceNotFoundException) {
                    return false
                } else {
                    throw e
                }
            }
        }
        return true
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
            .overwrite(true)
            .build()
        ssmAsyncClient.putParameter(putParameterRequest).get()

        // Amazon doesn't allow setting `overwrite = true` in the `PutParameter` request above and adding tags at the
        // same time. So we have to create the parameter first and then add the tags using `AddTagsToResource`.
        val addTagsToResourceRequest = AddTagsToResourceRequest.builder()
            .resourceId(organization.ssmDbPassPath)
            .resourceType(ResourceTypeForTagging.PARAMETER)
            .tags(
                buildSsmTag("organization", organization.name!!),
                buildSsmTag("organization-hash", organization.hash!!)
            )
            .build()
        ssmAsyncClient.addTagsToResource(addTagsToResourceRequest)
        organizationDao.merge(organization)
        return password
    }
}

private fun buildSsmTag(key: String, value: String) = Tag.builder().key(key).value(value).build()
private fun buildRdsTag(key: String, value: String) = TagRds.builder().key(key).value(value).build()
