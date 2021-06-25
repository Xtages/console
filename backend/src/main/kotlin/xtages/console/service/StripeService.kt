package xtages.console.service

import com.stripe.Stripe
import com.stripe.model.Event
import com.stripe.param.checkout.SessionCreateParams
import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.util.UriComponentsBuilder
import xtages.console.config.ConsoleProperties
import xtages.console.exception.ensure
import xtages.console.query.enums.OrganizationSubscriptionStatus.ACTIVE
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.StripeCheckoutSessionDao
import xtages.console.query.tables.pojos.StripeCheckoutSession
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.exception.ExceptionCode.*
import xtages.console.service.aws.RdsService
import java.net.URI
import com.stripe.model.billingportal.Session as PortalSession
import com.stripe.model.checkout.Session as CheckoutSession
import com.stripe.param.billingportal.SessionCreateParams as CustomerPortalSessionCreateParams

@Service
@RequestScope
class StripeService(
    private val consoleProperties: ConsoleProperties,
    private val organizationDao: OrganizationDao,
    private val stripeCheckoutSessionDao: StripeCheckoutSessionDao,
    private val authenticationService: AuthenticationService,
    private val rdsService: RdsService,
) {
    init {
        Stripe.apiKey = consoleProperties.stripe.apiKey;
    }

    /**
     * Creates a new Stripe Checkout [com.stripe.model.checkout.Session] from the [priceIds] passed in.
     *
     * It will also create a new row in the `stripe_checkout_session` table so we can then get back the organization
     * name when a purchase is confirmed through a Stripe webhook request.
     *
     * @return The id of the Stripe [com.stripe.model.checkout.Session] that was created.
     */
    fun createCheckoutSession(priceIds: List<String>, organizationName: String): String? {
        val successUrl = UriComponentsBuilder.fromUri(URI(consoleProperties.server.basename))
            .pathSegment("account")
            .queryParam("session_id", "{CHECKOUT_SESSION_ID}")
        val cancelUrl = UriComponentsBuilder.fromUri(URI(consoleProperties.server.basename))
            .pathSegment("signup")
        val builder = SessionCreateParams.builder()
            .setSuccessUrl(successUrl.build(false).toString())
            .setCancelUrl(cancelUrl.toUriString())
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION);
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
        stripeCheckoutSessionDao.merge(
            StripeCheckoutSession(
                stripeCheckoutSessionId = session.id,
                organizationName = organizationName
            )
        )
        return session.id
    }

    /**
     * Creates a Stripe Customer Portal [com.stripe.model.billingportal.Session] and returns its [URI].
     *
     * It's possible that this will fail if we haven't recieved the webhook request from Stripe confirming the
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
            StripeWebhookEventType.INVOICE_PAID -> TODO()
            StripeWebhookEventType.INVOICE_PAYMENT_FAILED -> TODO()
        }
    }

    /**
     * Handles the `checkout.session.completed` webhook event.
     *
     * Updates the `organization` table row for an organization with the `customerId` from Stripe and updates its
     * `subscription_status` to `ACTIVE`.
     */
    private fun onCheckoutCompleted(event: Event) {
        val stripeObject = event.dataObjectDeserializer.`object`.get() as CheckoutSession
        val organizationName = ensure.foundOne(
            operation = {
                stripeCheckoutSessionDao.fetchByStripeCheckoutSessionId(stripeObject.id).single()
            },
            code = CHECKOUT_SESSION_NOT_FOUND,
            message = "Checkout session not found"
        ).organizationName!!
        val organization = ensure.foundOne(
            operation = { organizationDao.fetchOneByName(organizationName) },
            code = ORG_NOT_FOUND,
            lazyMessage = { "Organization [$organizationName] not found" }
        )
        organizationDao.update(
            organization.copy(
                stripeCustomerId = stripeObject.customer,
                subscriptionStatus = ACTIVE
            )
        )

        rdsService.provision(organization)
    }
}

enum class StripeWebhookEventType(private val stripeName: String) {
    CHECKOUT_COMPLETED("checkout.session.completed"),
    INVOICE_PAID("invoice.paid"),
    INVOICE_PAYMENT_FAILED("invoice.payment_failed");

    companion object {
        fun fromStripeEventName(stripeEvent: String) =
            values().find { it.stripeName.equals(stripeEvent, ignoreCase = true) }
    }
}
