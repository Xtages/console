package xtages.console.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.web.servlet.invoke
import xtages.console.config.Profiles.PROD
import xtages.console.config.Profiles.STAGING

@Configuration
class SecurityConfig(@Autowired private val environment: Environment) : WebSecurityConfigurerAdapter() {

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
            sessionManagement {
                // Don't create an HTTPSession and instead always rely on the
                // Authorization Bearer token.
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            authorizeRequests {
                authorize("/", permitAll)
                // Allow access to `/actuator` endpoints when not in `PROD` or `STAGING`.
                if (!environment.activeProfiles.contains(PROD.name) ||
                    !environment.activeProfiles.contains(STAGING.name)
                ) {
                    authorize("/actuator", permitAll)
                } else {
                    // This is a callout for use to figure out what to do about
                    // being able to access actuator endpoints when running in prod.
                    authorize("/actuator", authenticated)
                }
                // Everything under `/api` requires authn.
                authorize("/api/*", authenticated)
            }
            // spring-boot-starter-oauth2-resource-server already does authentication using
            // the `Authorization: Bearer ****` header, we just had configure to configure
            // the `issuer-uri` and the `jwk-set-uri` (see application.properties).
            oauth2ResourceServer {
                jwt { }
            }
        }
    }
}
