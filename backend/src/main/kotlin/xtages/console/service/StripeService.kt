package xtages.console.service

import com.stripe.Stripe
import com.stripe.model.Event
import com.stripe.model.Invoice
import com.stripe.model.Subscription
import com.stripe.param.checkout.SessionCreateParams
import com.stripe.param.checkout.SessionCreateParams.SubscriptionData
import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.util.UriComponentsBuilder
import xtages.console.config.ConsoleProperties
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.dao.insertIfNotExists
import xtages.console.exception.ExceptionCode.ORG_NOT_FOUND
import xtages.console.exception.ensure
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.enums.OrganizationSubscriptionStatus.ACTIVE
import xtages.console.query.enums.OrganizationSubscriptionStatus.SUSPENDED
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.OrganizationToPlanDao
import xtages.console.query.tables.daos.PlanDao
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.OrganizationToPlan
import xtages.console.service.aws.RdsService
import java.net.URI
import java.time.LocalDateTime
import com.stripe.model.billingportal.Session as PortalSession
import com.stripe.model.checkout.Session as CheckoutSession
import com.stripe.param.billingportal.SessionCreateParams as CustomerPortalSessionCreateParams


private val logger = KotlinLogging.logger { }
const val ORGANIZATION_NAME_PARAM = "organizationName"
const val OWNER_COGNITO_USER_ID_PARAM = "ownerCognitoUserId"

@Service
@RequestScope
class StripeService(
    private val consoleProperties: ConsoleProperties,
    private val organizationDao: OrganizationDao,
    private val organizationService: OrganizationService,
    private val planDao: PlanDao,
    private val organizationToPlanDao: OrganizationToPlanDao,
    private val authenticationService: AuthenticationService,
    private val rdsService: RdsService,
) {
    init {
        Stripe.apiKey = consoleProperties.stripe.apiKey
    }

    /**
     * Creates a new Stripe Checkout [com.stripe.model.checkout.Session] from the [priceIds] passed in.
     *
     * It will also create a new row in the `stripe_checkout_session` table so we can then get back the organization
     * name when a purchase is confirmed through a Stripe webhook request.
     *
     * @return The id of the Stripe [com.stripe.model.checkout.Session] that was created.
     */
    fun createCheckoutSession(priceIds: List<String>, organizationName: String, ownerCognitoUserId: String): String? {
        val successUrl = UriComponentsBuilder.fromUri(URI(consoleProperties.server.basename))
            .pathSegment("account")
            .queryParam("session_id", "{CHECKOUT_SESSION_ID}")
        val cancelUrl = UriComponentsBuilder.fromUri(URI(consoleProperties.server.basename))
            .pathSegment("signup")
        val isTrialProduct = priceIds.any {
            consoleProperties.stripe.starterPriceIds.split(",").contains(it)
        }

        val metadata = mapOf(
            ORGANIZATION_NAME_PARAM to organizationName,
            OWNER_COGNITO_USER_ID_PARAM to ownerCognitoUserId
        )

        val builder = SessionCreateParams.builder()
            .setSuccessUrl(successUrl.build(false).toString())
            .setCancelUrl(cancelUrl.toUriString())
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setAllowPromotionCodes(true)
            .putAllMetadata(metadata)
            .setSubscriptionData(buildSubscriptionData(isTrialProduct, metadata))

        priceIds.forEach { price ->
            builder.addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPrice(price)
                    .build()
            )
        }
        val sessionParams = builder.build()

        val session = CheckoutSession.create(sessionParams)
        return session.id
    }

    private fun buildSubscriptionData(isTrialProduct: Boolean, metadata: Map<String, String>): SubscriptionData? {
        val builder = SubscriptionData.builder()
            .putAllMetadata(metadata)
        if (isTrialProduct) {
            builder.setTrialPeriodDays(consoleProperties.stripe.trialPeriod)
        }
        return builder.build()

    }

    /**
     * Creates a Stripe Customer Portal [com.stripe.model.billingportal.Session] and returns its [URI].
     *
     * It's possible that this will fail if we haven't received the webhook request from Stripe confirming the
     * checkout transaction.
     *
     * @return An [URI] pointing to the customer's Stripe portal.
     */
    fun createCustomerPortalSession(): URI {
        val returnUrl = UriComponentsBuilder.fromUri(URI(consoleProperties.server.basename)).pathSegment("account")
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val stripeCustomerId = ensure.notNull(
            value = organization.stripeCustomerId,
            valueDesc = "organization.stripeCustomerId",
            lazyMessage = { "A Stripe customer id was not found for ${organization.name}" }
        )
        val sessionParams =
            CustomerPortalSessionCreateParams.builder()
                .setReturnUrl(returnUrl.toUriString())
                .setCustomer(stripeCustomerId)
                .build()
        val session = PortalSession.create(sessionParams)
        return URI(session.url)
    }

    fun handleWebhookRequest(event: Event) {
        when (StripeWebhookEventType.fromStripeEventName(event.type)) {
            StripeWebhookEventType.CHECKOUT_COMPLETED -> onCheckoutCompleted(event)
            StripeWebhookEventType.INVOICE_PAYMENT_FAILED -> onInvoicePaymentFailed(event)
            StripeWebhookEventType.INVOICE_PAID -> onInvoicePaid(event)
            StripeWebhookEventType.INVOICE_PAYMENT_SUCCEEDED -> onInvoicePaymentSucceeded(event)
        }
    }

    private fun onInvoicePaymentSucceeded(event: Event) {
        val invoice = event.dataObjectDeserializer.`object`.get() as Invoice
        val subscription = Subscription.retrieve(invoice.subscription)
        createOrUpdateOrganization(
            organizationName = subscription.metadata[ORGANIZATION_NAME_PARAM]!!,
            ownerCognitoUserId = subscription.metadata[OWNER_COGNITO_USER_ID_PARAM]!!,
            stripeCustomerId = subscription.customer,
            subscriptionStatus = ACTIVE
        )
    }

    /**
     * Handles the `invoice.paid` webhook event
     *
     * The [Organization] changes its state to `ACTIVE`. In most cases the Organization will be `ACTIVE` but in case
     * it's `SUSPENDED` will get access to the app again
     */
    private fun onInvoicePaid(event: Event) {
        val invoice = event.dataObjectDeserializer.`object`.get() as Invoice
        val subscription = Subscription.retrieve(invoice.subscription)
        createOrUpdateOrganization(
            organizationName = subscription.metadata[ORGANIZATION_NAME_PARAM]!!,
            ownerCognitoUserId = subscription.metadata[OWNER_COGNITO_USER_ID_PARAM]!!,
            stripeCustomerId = subscription.customer,
            subscriptionStatus = ACTIVE
        )
    }

    private fun createOrUpdateOrganization(
        organizationName: String,
        ownerCognitoUserId: String,
        stripeCustomerId: String,
        subscriptionStatus: OrganizationSubscriptionStatus
    ): Organization {
        var organization = organizationDao.fetchOneByName(organizationName)
        if (organization == null) {
            logger.info { "Organization $organizationName doesn't exist, creating it and marking it [$subscriptionStatus]." }
            organization = organizationService.create(
                organizationName = organizationName,
                ownerCognitoUserId = ownerCognitoUserId,
                stripeCustomerId = stripeCustomerId,
                subscriptionStatus = subscriptionStatus
            )
        } else if (organization.subscriptionStatus != subscriptionStatus) {
            organization.subscriptionStatus = subscriptionStatus
            organizationDao.update(organization)
            logger.info { "Organization ${organization.name} has transitioned to [$subscriptionStatus] state" }
        }
        return organization
    }

    @Cacheable
    private fun getOrganizationNameFromSubscription(invoice: Invoice): String {
        val subscription = Subscription.retrieve(invoice.subscription)
        return subscription.metadata[ORGANIZATION_NAME_PARAM]!!
    }

    /**
     * Handles the `invoice.payment_failed` webhook event
     *
     * The [Organization] changes its state to `SUSPENDED`
     */
    private fun onInvoicePaymentFailed(event: Event) {
        val invoice = event.dataObjectDeserializer.`object`.get() as Invoice
        val organization = getOrganization(getOrganizationNameFromSubscription(invoice))
        organization.subscriptionStatus = SUSPENDED
        organizationDao.update(organization)
        logger.info { "Organization ${organization.name} has transitioned to SUSPENDED state" }
    }

    private fun getOrganization(organizationName: String): Organization {
        return ensure.foundOne(
            operation = { organizationDao.fetchOneByName(organizationName) },
            code = ORG_NOT_FOUND,
            lazyMessage = { "Organization [$organizationName] not found" }
        )
    }

    /**
     * Handles the `checkout.session.completed` webhook event.
     *
     * Updates the `organization` table row for an organization with the `customerId` from Stripe and updates its
     * `subscription_status` to `ACTIVE`.
     */
    private fun onCheckoutCompleted(event: Event) {
        val checkout = event.dataObjectDeserializer.`object`.get() as CheckoutSession
        val organizationName = checkout.metadata[ORGANIZATION_NAME_PARAM]!!
        val ownerCognitoUserId = checkout.metadata[OWNER_COGNITO_USER_ID_PARAM]!!
        val organization = createOrUpdateOrganization(
            organizationName = organizationName,
            ownerCognitoUserId = ownerCognitoUserId,
            stripeCustomerId = checkout.customer,
            subscriptionStatus = ACTIVE
        )

        val subscription = Subscription.retrieve(checkout.subscription)
        val plan = planDao.fetchByProductId(subscription.items.data.single().price.product).single()

        organizationToPlanDao.insertIfNotExists(
            OrganizationToPlan(
                organizationName = organization.name,
                planId = plan.id,
                startTime = LocalDateTime.now()
            )
        )
        if (organization.rdsArn == null) {
            rdsService.provision(organization)
        }
    }
}

enum class StripeWebhookEventType(private val stripeName: String) {
    CHECKOUT_COMPLETED("checkout.session.completed"),
    INVOICE_PAID("invoice.paid"),
    INVOICE_PAYMENT_SUCCEEDED("invoice.payment_succeeded"),
    INVOICE_PAYMENT_FAILED("invoice.payment_failed");

    companion object {
        fun fromStripeEventName(stripeEvent: String) =
            values().find { it.stripeName.equals(stripeEvent, ignoreCase = true) }
    }
}
