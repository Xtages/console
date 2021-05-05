package xtages.console.config

import mu.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
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
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.access.expression.WebExpressionVoter
import org.springframework.stereotype.Component
import xtages.console.dao.findByCognitoUserId
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ensure
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.tables.daos.OrganizationDao
import org.springframework.security.config.web.servlet.invoke

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
                .mvcMatchers("/*.html").permitAll()
                .mvcMatchers("/*.json").permitAll()
                .mvcMatchers("/*.svg").permitAll()
                .mvcMatchers("/*.ico").permitAll()
                .mvcMatchers("/*.txt").permitAll()
                .mvcMatchers("/static/**").permitAll()
                .mvcMatchers("/api/**").authenticated().accessDecisionManager(accessDecisionManager())
                // Allow all access to POSTs made to webhook paths because those will be cryptographically authenticated
                // via signature
                .mvcMatchers(POST, "/api/v1/*/webhook").permitAll()
            // Only allow access to actuator paths in "dev"
            if (Profiles.DEV.name in environment.activeProfiles) {
                authorize.mvcMatchers("/actuator/**").permitAll()
            }
            // Deny access to everything else
            authorize.mvcMatchers("/**").denyAll()
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
class OrganizationInGoodStandingAccessDecisionVoter(val organizationDao: OrganizationDao) : AccessDecisionVoter<Any> {
    override fun supports(attribute: ConfigAttribute) = attribute.attribute == "authenticated"

    override fun supports(clazz: Class<*>?) = true

    override fun vote(
        authentication: Authentication?,
        `object`: Any?,
        attributes: MutableCollection<ConfigAttribute>?
    ): Int {
        if (authentication is JwtAuthenticationToken) {
            val organizationName = ensure.ofType<String>(
                authentication.tokenAttributes["custom:org"],
                "organization"
            )
            val cognitoUserId = CognitoUserId.fromJwt(authentication.principal)
            val organization =
                ensure.foundOne(
                    operation = { organizationDao.findByCognitoUserId(cognitoUserId) },
                    code = ExceptionCode.ORG_NOT_FOUND
                )
            if (organization.name == organizationName &&
                organization.subscriptionStatus in validOrgSubscriptionStatus
            ) {
                return AccessDecisionVoter.ACCESS_GRANTED
            } else {
                if (organization.name != organizationName) {
                    logger.debug {
                        "User [${cognitoUserId.id}] tried to access organization [$organizationName](from JWT claim) but it not an organization the user belongs to."
                    }
                }
                if (organization.subscriptionStatus !in validOrgSubscriptionStatus) {
                    logger.debug {
                        "User [${cognitoUserId.id}] tried to access organization [$organizationName](from JWT claim) but the organization's subscription status is [${organization.subscriptionStatus}"
                    }
                }
            }
            return AccessDecisionVoter.ACCESS_DENIED
        }
        return AccessDecisionVoter.ACCESS_ABSTAIN
    }

}
