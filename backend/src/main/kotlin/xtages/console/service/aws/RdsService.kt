package xtages.console.service.aws

import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.rds.RdsAsyncClient
import software.amazon.awssdk.services.rds.model.*
import software.amazon.awssdk.services.ssm.model.*
import xtages.console.config.ConsoleProperties
import xtages.console.dao.canAllocatedResource
import xtages.console.dao.fetchByOrganizationAndResourceType
import xtages.console.dao.insertIfNotExists
import xtages.console.pojo.dbIdentifier
import xtages.console.pojo.dbName
import xtages.console.pojo.dbUsername
import xtages.console.query.enums.ResourceStatus
import xtages.console.query.enums.ResourceType.POSTGRESQL
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.PlanDao
import xtages.console.query.tables.daos.ResourceDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.Plan
import xtages.console.query.tables.pojos.Resource
import xtages.console.service.OrganizationService
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
    private val consoleProperties: ConsoleProperties,
    private val planDao: PlanDao,
    private val organizationDao: OrganizationDao,
    private val organizationService: OrganizationService,
    private val resourceDao: ResourceDao,
) {

    /**
     * Provisions a PostgreSQL DB for [organization]. If [plan] is free then we first check if we are over the limit of
     * db allocations and if we are then we add a [ResourceStatus.WAIT_LISTED] [Resource], instead of actually making
     * the call to AWS to provision the DB.
     */
    fun provisionPostgreSql(organization: Organization, plan: Plan): Resource {
        return refreshPostgreSqlInstanceStatus(organization = organization)
            ?: return if (resourceDao.canAllocatedResource(POSTGRESQL)) {
                provisionPostgreSqlDbInstance(organization = organization, plan = plan)
            } else {
                val waitListedResource = Resource(
                    organizationName = organization.name,
                    resourceType = POSTGRESQL,
                    resourceStatus = ResourceStatus.WAIT_LISTED,
                )
                resourceDao.insertIfNotExists(waitListedResource)
                waitListedResource
            }
    }

    /**
     * Async operation that provision an Aurora Serverless cluster asynchronously
     * By default the cluster will have a min of 2 ACUs and a max of 4 ACUs
     * That's similar to 2 vCPU and 2 GB of RAM and 4 vCPU and 4 GB of RAM
     * Also, by default the auto pause will be enabled
     * Note: Even though this is not being used currently, will leave it in case we want to explore this
     * solution based on customer usage
     */
    private fun provisionPostgreSqlServerlessCluster(organization: Organization, plan: Plan): Resource {
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
            val resource = Resource(
                organizationName = organization.name,
                resourceType = POSTGRESQL,
                resourceStatus = ResourceStatus.REQUESTED,
                resourceArn = result.dbCluster().dbClusterArn()
            )
            resourceDao.insert(resource)
            return resource
        } catch (e: RdsException) {
            logger.error {
                "There was an error while provisioning the DB for organization: ${organization.name}. Plan id used: ${plan.id}"
            }
            logger.error(e) {}
            throw e
        }
    }

    /**
     * Provisions a classic RDS instance with Postgres
     */
    private fun provisionPostgreSqlDbInstance(organization: Organization, plan: Plan): Resource {
        val password = createAndStorePassInSsm(organization)
        val instanceRequest = CreateDbInstanceRequest.builder()
            .dbInstanceIdentifier(organization.dbIdentifier)
            .masterUserPassword(password)
            .allocatedStorage(plan.dbStorageGbs!!.toInt())
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
            val resource = Resource(
                organizationName = organization.name,
                resourceType = POSTGRESQL,
                resourceStatus = ResourceStatus.REQUESTED,
                resourceArn = result.dbInstance().dbInstanceArn()
            )
            resourceDao.insert(resource)
            return resource
        } catch (e: RdsException) {
            logger.error {
                "There was an error while provisioning the DB for organization: ${organization.name}. Plan id used: ${plan.id}"
            }
            logger.error(e) {}
            throw e
        }
    }

    /**
     * Checks if a PostgreSQL DB instance exists for the [organization] and has been provisioned. Records in our
     * DB the endpoint for the instance.
     */
    fun refreshPostgreSqlInstanceStatus(organization: Organization): Resource? {
        val dbResource = resourceDao.fetchByOrganizationAndResourceType(
            organization = organization,
            resourceType = POSTGRESQL
        )
        if (dbResource != null && dbResource.resourceStatus == ResourceStatus.REQUESTED) {
            val endpoint = getEndpoint(organization = organization)
            if (endpoint != null) {
                dbResource.resourceEndpoint = endpoint
                dbResource.resourceStatus = ResourceStatus.PROVISIONED
                resourceDao.merge(dbResource)
            }
        }
        return dbResource
    }

    private fun getEndpoint(organization: Organization): String? {
        return try {
            // db instance
            val response = rdsAsyncClient.describeDBInstances(
                DescribeDbInstancesRequest
                    .builder()
                    .dbInstanceIdentifier(organization.dbIdentifier)
                    .build()
            ).get()
            response.dbInstances().firstOrNull()?.endpoint()?.address()
        } catch (e: ExecutionException) {
            if (e.cause is DbInstanceNotFoundException) {
                return null
            } else {
                throw e
            }
        }
    }

    /**
     * Updates the DB instance specs for [organization] based on [plan].
     */
    fun updatePostgreSqlInstanceSpecs(organization: Organization, plan: Plan) {
        rdsAsyncClient
            .modifyDBInstance(
                ModifyDbInstanceRequest
                    .builder()
                    .dbInstanceIdentifier(organization.dbIdentifier)
                    .dbInstanceClass(plan.dbInstance)
                    .allocatedStorage(plan.dbStorageGbs!!.toInt())
                    .applyImmediately(true)
                    .build()
            )
            .also { logger.debug { "Updated DB Instance [${organization.dbIdentifier}] to instanceClass [${plan.dbInstance}] and allocatedStorage [${plan.dbStorageGbs!!.toInt()}]" } }
            .get()
    }

    /**
     * Creates and store in SSM the password used for the RDS instance
     * The path for SSM is build as follow:
     * [ConsoleProperties.aws.rds.ssmPrefix] + [organization.hash] + /rds/password
     */
    private fun createAndStorePassInSsm(organization: Organization): String {
        val password = RandomStringUtils.randomAscii(25).replace(charsNotAllowedInPasswordRegex, "")
        val parameter =
            organizationService.storeSsmParameter(organization = organization, name = "/rds/password", value = password)
        organization.ssmDbPassPath = parameter
        organizationDao.merge(organization)
        return password
    }
}

private fun buildRdsTag(key: String, value: String) = TagRds.builder().key(key).value(value).build()
