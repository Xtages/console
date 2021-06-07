package xtages.console.controller

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest

private val logger = KotlinLogging.logger { }

/**
 * An [ErrorViewResolver] that handles all requests that are not matched by an existing
 * controller and responds with the contents of `index.html`. This is necessary
 * to enable client side routing of the React frontend app.
 */
@Component
class XtagesErrorViewResolver : ErrorViewResolver {
    override fun resolveErrorView(
        request: HttpServletRequest,
        status: HttpStatus,
        model: MutableMap<String, Any>
    ): ModelAndView {
        logger.debug { "Attempting request to ${request.contextPath} which is not handled, serving index.html instead" }
        return ModelAndView("index.html", HttpStatus.OK)
    }
}
