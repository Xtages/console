package xtages.console.exception

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.lang.Exception

@ControllerAdvice
class XtagesConsoleExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(XtagesConsoleException::class)
    fun handleXtagesConsoleException(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        logger.error(ex.message, ex)
        val headers = HttpHeaders()
        return super.handleExceptionInternal(ex, null, headers, INTERNAL_SERVER_ERROR, request)
    }
}
