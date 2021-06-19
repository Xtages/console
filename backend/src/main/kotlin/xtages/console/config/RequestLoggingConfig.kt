package xtages.console.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter

/**
 * Enables request logging.
 */
@Configuration
class RequestLoggingConfig {
    @Bean
    fun logFilter(): CommonsRequestLoggingFilter {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludeQueryString(true)
        filter.setIncludePayload(true)
        filter.setMaxPayloadLength(10000)
        filter.setIncludeHeaders(true)
        filter.setHeaderPredicate { header -> header != "cookie" && header != "authorization" }
        filter.setAfterMessagePrefix("REQUEST DATA: ")
        return filter
    }
}
