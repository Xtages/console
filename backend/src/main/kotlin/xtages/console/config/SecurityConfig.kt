package xtages.console.config

import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.POST
import org.springframework.security.access.AccessDecisionVoter
import org.springframework.security.access.ConfigAttribute
import org.springframework.security.access.vote.AuthenticatedVoter
import org.springframework.security.access.vote.RoleVoter
import org.springframework.security.access.vote.UnanimousBased
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.FilterInvocation
import org.springframework.security.web.access.expression.WebExpressionVoter
import org.springframework.stereotype.Component
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.exception.ensure
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.daos.OrganizationDao

private val logger = KotlinLogging.logger { }

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val environment: Environment,
    private val organizationInGoodStandingAccessDecisionVoter: OrganizationInGoodStandingAccessDecisionVoter
) :
    WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http {
            httpBasic {
                // No HTTP Basic authentication.
                disable()
            }
            logout {
                // Our authentication is stateless therefore there's no point in
                // allowing logout.
                disable()
            }
            csrf {
                ignoringAntMatchers("/api/v1/*/webhook")
            }
            sessionManagement {
                // Don't create an HTTPSession and instead always rely on the
                // Authorization Bearer token.
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            // spring-boot-starter-oauth2-resource-server already does authentication using
            // the `Authorization: Bearer ****` header, we just had configure to configure
            // the `issuer-uri` and the `jwk-set-uri` (see application.properties).
            oauth2ResourceServer {
                jwt { }
            }
        }
        http.authorizeRequests { authorize ->
            authorize
                .mvcMatchers("/**").permitAll()
                .mvcMatchers("/api/**").authenticated().accessDecisionManager(accessDecisionManager())
                // Allow all access to POSTs made to webhook paths because those will be cryptographically authenticated
                // via signature
                .mvcMatchers(POST, "/api/v1/*/webhook").permitAll()
            // Only allow access to actuator paths in "dev"
            if (Profiles.DEV.name in environment.activeProfiles) {
                authorize.mvcMatchers("/actuator/**").permitAll()
            } else {
                // Deny access to actuator paths otherwise
                authorize.mvcMatchers("/actuator/**").denyAll()
            }
        }
    }

    fun accessDecisionManager(): UnanimousBased {
        return UnanimousBased(
            listOf(
                WebExpressionVoter(),
                RoleVoter(),
                AuthenticatedVoter(),
                organizationInGoodStandingAccessDecisionVoter
            )
        );
    }
}

/**
 * Cognito User Id wrapper.
 */
inline class CognitoUserId(val id: String) {
    companion object {
        fun fromJwt(jwt: Jwt) = CognitoUserId(jwt.subject)

        fun fromJwt(jwt: Any) = fromJwt(ensure.ofType(jwt, "jwt"))
    }
}

private val validOrgSubscriptionStatus =
    setOf(
        OrganizationSubscriptionStatus.ACTIVE,
        OrganizationSubscriptionStatus.PENDING_CANCELLATION
    )

/**
 * Determines if the authenticated user belongs to the [Organization] the jwt claims it does (the claim is called
 * `custom:org`) and if the organization is in good standing (the purchase flow has been completed and they are not
 * suspended or cancelled).
 */
@Component
class OrganizationInGoodStandingAccessDecisionVoter(val organizationDao: OrganizationDao) : AccessDecisionVoter<FilterInvocation> {
    override fun supports(attribute: ConfigAttribute) = attribute.attribute == "authenticated"

    override fun supports(clazz: Class<*>?) = true

    override fun vote(
        authentication: Authentication?,
        invocation: FilterInvocation,
        attributes: MutableCollection<ConfigAttribute>?
    ): Int {
        logger.debug { "Handling invocation [$invocation]" }
        if (authentication is JwtAuthenticationToken) {
            // For POST /api/v1/organization we only require that the user has a valid JWT, because this endpoint is
            // how the checkout flow creates an Organization and if we try validate that there's an Organization
            // associated to the user then we run into a chicken & egg problem.
            if (HttpMethod.resolve(invocation.request.method) == POST && invocation.requestUrl == "/api/v1/organization") {
                logger.debug { "[$invocation] Attempting to create organization." }
                return AccessDecisionVoter.ACCESS_ABSTAIN
            }
            val organizationName = ensure.ofType<String>(
                authentication.tokenAttributes["custom:organization"],
                "organization"
            )
            val cognitoUserId = CognitoUserId.fromJwt(authentication.principal)
            val organization = organizationDao.fetchOneByCognitoUserId(cognitoUserId)
            if (organization.name == organizationName &&
                organization.subscriptionStatus in validOrgSubscriptionStatus
            ) {
                MDC.put("cid", cognitoUserId.id)
                MDC.put("org", organization.name)
                return AccessDecisionVoter.ACCESS_GRANTED
            } else {
                if (organization.name != organizationName) {
                    logger.warn {
                        "[$invocation] User [${cognitoUserId.id}] tried to access organization [$organizationName](from JWT claim) but it's not an organization the user belongs to."
                    }
                }
                if (organization.subscriptionStatus !in validOrgSubscriptionStatus) {
                    // Allow for Organization that are not in good standing to create a Stripe CheckoutSession, so we
                    // send them to the Stripe UI.
                    if (HttpMethod.resolve(invocation.request.method) == POST && invocation.requestUrl == "/api/v1/checkout/session") {
                        return AccessDecisionVoter.ACCESS_GRANTED
                    } else {
                        logger.info {
                            "[$invocation] User [${cognitoUserId.id}] tried to access organization [$organizationName](from JWT claim) but the organization's subscription status is [${organization.subscriptionStatus}]"
                        }
                    }
                }
            }
            return AccessDecisionVoter.ACCESS_DENIED
        }
        return AccessDecisionVoter.ACCESS_ABSTAIN
    }

}
