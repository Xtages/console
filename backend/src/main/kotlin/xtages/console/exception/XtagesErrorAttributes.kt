package xtages.console.exception

import mu.KotlinLogging
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.stereotype.Component
import org.springframework.web.context.request.WebRequest

private val logger = KotlinLogging.logger { }

/**
 * A [DefaultErrorAttributes] handler that adds `error_code` ([ExceptionCode]) to the response and logs the `error` is
 * one occurred.
 */
@Component
class XtagesErrorAttributes : DefaultErrorAttributes() {
    override fun getErrorAttributes(webRequest: WebRequest?, options: ErrorAttributeOptions?): MutableMap<String, Any> {
        val errorAttributes = super.getErrorAttributes(webRequest, options)
        val error = getError(webRequest)
        if (error is XtagesConsoleException) {
            errorAttributes["error_code"] = error.code.name
        }
        if (error != null) {
            logger.error(error) { "An error occurred." }
        }
        return errorAttributes
    }

    override fun getMessage(webRequest: WebRequest?, error: Throwable?): String {
        if (error is XtagesConsoleException) {
            return error.messageWithoutCode
        }
        return super.getMessage(webRequest, error)
    }
}
