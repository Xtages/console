package xtages.console.config

import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
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
import xtages.console.dao.maybeFetchOneByCognitoUserId
import xtages.console.exception.ensure
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.daos.OrganizationDao

private val logger = KotlinLogging.logger { }

@Configuration
@EnableWebSecurity
class SecurityConfig(
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
                ignoringAntMatchers("/api/v1/*/webhook", "/api/v1/organization/eligibility")
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
                // This endpoint is called from the signup page so we don't have JWT yet.
                .mvcMatchers(HttpMethod.PUT, "/api/v1/organization/eligibility").permitAll()
                // Allow all access to POSTs made to webhook paths because those will be cryptographically authenticated
                // via signature
                .mvcMatchers(HttpMethod.POST, "/api/v1/*/webhook").permitAll()
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
class OrganizationInGoodStandingAccessDecisionVoter(val organizationDao: OrganizationDao) :
    AccessDecisionVoter<FilterInvocation> {
    override fun supports(attribute: ConfigAttribute) = attribute.attribute == "authenticated"

    override fun supports(clazz: Class<*>?) = true

    override fun vote(
        authentication: Authentication?,
        invocation: FilterInvocation,
        attributes: MutableCollection<ConfigAttribute>?
    ): Int {
        logger.debug { "Handling invocation [$invocation]" }
        if (authentication is JwtAuthenticationToken) {
            // For GET /api/v1/organization we only require that the user has a valid JWT, because this endpoint
            // allows the FE to determine if the user has an organization associated and in which status the
            // organization is.
            // For POST /api/v1/checkout/session we only require that the user has a valid JWT because this endpoint is
            // used before an organization exists.
            val method = HttpMethod.resolve(invocation.request.method)
            if ((method == HttpMethod.GET && invocation.requestUrl == "/api/v1/organization")
                || (method == HttpMethod.POST && invocation.requestUrl == "/api/v1/checkout/session")
            ) {
                logger.debug { "[$invocation] Attempting to get organization or create checkout session." }
                return AccessDecisionVoter.ACCESS_ABSTAIN
            }
            val organizationName = ensure.ofType<String>(
                authentication.tokenAttributes["custom:organization"],
                "organization"
            )
            val cognitoUserId = CognitoUserId.fromJwt(authentication.principal)
            val organization = organizationDao.maybeFetchOneByCognitoUserId(cognitoUserId)
            if (organization != null) {
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
