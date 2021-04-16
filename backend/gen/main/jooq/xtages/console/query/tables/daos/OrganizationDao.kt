/*
 * This file is generated by jOOQ.
 */
package xtages.console.query.tables.daos


import kotlin.collections.List

import org.jooq.Configuration
import org.jooq.impl.DAOImpl
import org.springframework.stereotype.Repository

import xtages.console.query.enums.GithubAppInstallationStatus
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.Organization
import xtages.console.query.tables.records.OrganizationRecord


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
@Repository
open class OrganizationDao(configuration: Configuration?) : DAOImpl<OrganizationRecord, xtages.console.query.tables.pojos.Organization, String>(Organization.ORGANIZATION, xtages.console.query.tables.pojos.Organization::class.java, configuration) {

    /**
     * Create a new OrganizationDao without any configuration
     */
    constructor(): this(null)

    override fun getId(o: xtages.console.query.tables.pojos.Organization): String? = o.name

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND upperInclusive</code>
     */
    fun fetchRangeOfName(lowerInclusive: String?, upperInclusive: String?): List<xtages.console.query.tables.pojos.Organization> = fetchRange(Organization.ORGANIZATION.NAME, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    fun fetchByName(vararg values: String): List<xtages.console.query.tables.pojos.Organization> = fetch(Organization.ORGANIZATION.NAME, *values)

    /**
     * Fetch a unique record that has <code>name = value</code>
     */
    fun fetchOneByName(value: String): xtages.console.query.tables.pojos.Organization? = fetchOne(Organization.ORGANIZATION.NAME, value)

    /**
     * Fetch records that have <code>stripe_customer_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    fun fetchRangeOfStripeCustomerId(lowerInclusive: String?, upperInclusive: String?): List<xtages.console.query.tables.pojos.Organization> = fetchRange(Organization.ORGANIZATION.STRIPE_CUSTOMER_ID, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>stripe_customer_id IN (values)</code>
     */
    fun fetchByStripeCustomerId(vararg values: String): List<xtages.console.query.tables.pojos.Organization> = fetch(Organization.ORGANIZATION.STRIPE_CUSTOMER_ID, *values)

    /**
     * Fetch records that have <code>subscription_status BETWEEN lowerInclusive AND upperInclusive</code>
     */
    fun fetchRangeOfSubscriptionStatus(lowerInclusive: OrganizationSubscriptionStatus?, upperInclusive: OrganizationSubscriptionStatus?): List<xtages.console.query.tables.pojos.Organization> = fetchRange(Organization.ORGANIZATION.SUBSCRIPTION_STATUS, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>subscription_status IN (values)</code>
     */
    fun fetchBySubscriptionStatus(vararg values: OrganizationSubscriptionStatus): List<xtages.console.query.tables.pojos.Organization> = fetch(Organization.ORGANIZATION.SUBSCRIPTION_STATUS, *values)

    /**
     * Fetch records that have <code>owner_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    fun fetchRangeOfOwnerId(lowerInclusive: Int?, upperInclusive: Int?): List<xtages.console.query.tables.pojos.Organization> = fetchRange(Organization.ORGANIZATION.OWNER_ID, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>owner_id IN (values)</code>
     */
    fun fetchByOwnerId(vararg values: Int): List<xtages.console.query.tables.pojos.Organization> = fetch(Organization.ORGANIZATION.OWNER_ID, *values.toTypedArray())

    /**
     * Fetch records that have <code>github_app_installation_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    fun fetchRangeOfGithubAppInstallationId(lowerInclusive: Long?, upperInclusive: Long?): List<xtages.console.query.tables.pojos.Organization> = fetchRange(Organization.ORGANIZATION.GITHUB_APP_INSTALLATION_ID, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>github_app_installation_id IN (values)</code>
     */
    fun fetchByGithubAppInstallationId(vararg values: Long): List<xtages.console.query.tables.pojos.Organization> = fetch(Organization.ORGANIZATION.GITHUB_APP_INSTALLATION_ID, *values.toTypedArray())

    /**
     * Fetch a unique record that has <code>github_app_installation_id = value</code>
     */
    fun fetchOneByGithubAppInstallationId(value: Long): xtages.console.query.tables.pojos.Organization? = fetchOne(Organization.ORGANIZATION.GITHUB_APP_INSTALLATION_ID, value)

    /**
     * Fetch records that have <code>github_app_installation_status BETWEEN lowerInclusive AND upperInclusive</code>
     */
    fun fetchRangeOfGithubAppInstallationStatus(lowerInclusive: GithubAppInstallationStatus?, upperInclusive: GithubAppInstallationStatus?): List<xtages.console.query.tables.pojos.Organization> = fetchRange(Organization.ORGANIZATION.GITHUB_APP_INSTALLATION_STATUS, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>github_app_installation_status IN (values)</code>
     */
    fun fetchByGithubAppInstallationStatus(vararg values: GithubAppInstallationStatus): List<xtages.console.query.tables.pojos.Organization> = fetch(Organization.ORGANIZATION.GITHUB_APP_INSTALLATION_STATUS, *values)
}
