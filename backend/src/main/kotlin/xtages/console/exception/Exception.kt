package xtages.console.exception

enum class ExceptionCode {
    ORG_NOT_FOUND,
    CHECKOUT_SESSION_NOT_FOUND,
    GH_APP_NOT_ALL_REPOSITORIES_SELECTED,
    GH_APP_INSTALLATION_ID_IS_NULL,
    GH_APP_INSTALLATION_INVALID,
    STRIPE_CUSTOMER_ID_IS_NULL,
}

/**
 * Base [Throwable] class for the Xtages console app.
 */
sealed class XtagesConsoleException(
    val code: ExceptionCode,
    private val innerMessage: String,
    cause: Throwable? = null
) :
    Throwable(cause = cause) {

    override val message: String
        get() = "[$code] $innerMessage"
}

class NotFoundException(code: ExceptionCode, innerMessage: String, cause: Throwable? = null) :
    XtagesConsoleException(code, innerMessage, cause)

class IllegalArgumentException(code: ExceptionCode, innerMessage: String, cause: Throwable? = null) :
    XtagesConsoleException(code, innerMessage, cause)

class NullValueException(code: ExceptionCode, innerMessage: String, cause: Throwable? = null) :
    XtagesConsoleException(code, innerMessage, cause)
