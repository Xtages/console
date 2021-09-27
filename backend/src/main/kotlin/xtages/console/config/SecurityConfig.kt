package xtages.console.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.oauth2.jwt.Jwt
import xtages.console.exception.ensure

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${spring.profiles.active}")
    private val activeProfile: String,
) : WebSecurityConfigurerAdapter() {

    val actuatorEndpointsAccessPerEnv = mapOf(
        "dev" to "/actuator/**",
        "prod" to "",
    )

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
                .mvcMatchers("/api/**").authenticated()
                // Allow all access to POSTs made to webhook paths because those will be cryptographically authenticated
                // via signature
                .mvcMatchers(HttpMethod.POST, "/api/v1/*/webhook").permitAll()
                // Denied access to actuator endpoints for prod allow for dev
                .mvcMatchers(actuatorEndpointsAccessPerEnv[activeProfile])
        }
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
