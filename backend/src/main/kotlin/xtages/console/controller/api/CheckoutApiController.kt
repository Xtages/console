package xtages.console.controller.api

import com.stripe.exception.SignatureVerificationException
import com.stripe.net.Webhook
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import xtages.console.config.ConsoleProperties
import xtages.console.controller.api.model.CreateCheckoutSessionReq
import xtages.console.controller.api.model.RecordCheckoutReq
import xtages.console.dao.fetchLatestPlan
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.dao.maybeFetchOneByCognitoUserId
import xtages.console.exception.ensure
import xtages.console.pojo.isSubscriptionInGoodStanding
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.OrganizationToPlanDao
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.service.AuthenticationService
import xtages.console.service.FreeCheckoutService
import xtages.console.service.StripeService
import java.net.URI

private const val STRIPE_SIGNATURE_HEADER = "Stripe-Signature"

private val logger = KotlinLogging.logger { }

@Controller
class CheckoutApiController(
    private val userDao: XtagesUserDao,
    private val authenticationService: AuthenticationService,
    private val stripeService: StripeService,
    private val organizationDao: OrganizationDao,
    private val consoleProperties: ConsoleProperties,
    private val freeCheckoutService: FreeCheckoutService,
    private val organizationToPlanDao: OrganizationToPlanDao,
) :
    CheckoutApiControllerBase {

    override fun createCheckoutSession(createCheckoutSessionReq: CreateCheckoutSessionReq): ResponseEntity<String> {
        return ResponseEntity.status(CREATED).body(
            stripeService.createCheckoutSession(
                priceIds = createCheckoutSessionReq.priceIds,
            )
        )
    }

    override fun getCustomerPortalSession(): ResponseEntity<URI> {
        val user = userDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId.id)
        if (user?.isOwner == true) {
            val organization =
                organizationDao.maybeFetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
            if (organization?.stripeCustomerId != null && organization.isSubscriptionInGoodStanding) {
                return ResponseEntity.ok(stripeService.createCustomerPortalSession())
            }
        }
        return ResponseEntity(HttpStatus.FORBIDDEN)
    }

    override fun recordCheckoutOutcome(recordCheckoutReq: RecordCheckoutReq): ResponseEntity<Unit> {
        stripeService.recordCheckoutOutcome(recordCheckoutReq.checkoutSessionId)
        return ResponseEntity.ok().build()
    }

    override fun freeTierCheckout(): ResponseEntity<Unit> {
        val organization =  organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)

        val plan = organizationToPlanDao.fetchLatestPlan(organization)
        // checking that the organization was not part of a paid plan
        // this is mostly to not break infrastructure at this point as migrations might need to happen.
        if (plan == null) {
            freeCheckoutService.provision(organization)
            return ResponseEntity.ok().build()
        }
        return ResponseEntity(FORBIDDEN)
    }

    @PostMapping("/checkout/webhook")
    fun webhook(
        @RequestBody body: String,
        @RequestHeader(STRIPE_SIGNATURE_HEADER) stripeSignatureHeader: String,
    ): ResponseEntity<String> {
        logger.trace { "Received Stripe webhook request." }
        try {
            val event = Webhook.constructEvent(body, stripeSignatureHeader, consoleProperties.stripe.webhookSecret)
            stripeService.handleWebhookRequest(event)
        } catch (e: SignatureVerificationException) {
            logger.error(e) { "Failed to verify Stripe webhook request signature." }
            return ResponseEntity.badRequest().body("Failed to webhook request.")
        }
        return ResponseEntity.ok().build()
    }
}
