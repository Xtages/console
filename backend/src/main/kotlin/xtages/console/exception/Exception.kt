package xtages.console.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import xtages.console.controller.model.usageDetailPojoToUsageDetail
import xtages.console.query.enums.ResourceType
import xtages.console.service.UsageOverLimit
import xtages.console.service.UsageOverLimitBecauseOfSubscriptionStatus
import xtages.console.service.UsageOverLimitNoPlan
import xtages.console.service.UsageOverLimitWithDetails

enum class ExceptionCode {
    ORG_NOT_FOUND,
    GH_APP_NOT_ALL_REPOSITORIES_SELECTED,
    NULL_VALUE,
    INVALID_TYPE,
    NOT_EQUALS,
    USER_NOT_FOUND,
    INVALID_ENVIRONMENT,
    INVALID_GITHUB_AVATAR_URL,
    INVALID_DOMAIN,
    PROJECT_NOT_FOUND,
    BUILD_NOT_FOUND,
    RECIPE_NOT_FOUND,
    USAGE_OVER_LIMIT,
    PROJECT_DEPLOYMENT_NOT_FOUND,
    COGNITO_ERROR,
}

/**
 * Base [Throwable] class for the Xtages console app. If [exceptionDetails] is not `null` then it will be serialized and
 * sent to the client.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
sealed class XtagesConsoleException(
    val code: ExceptionCode,
    message: String,
    val exceptionDetails: Any? = null,
) : Exception("[$code] $message") {
    val messageWithoutCode = message
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(code: ExceptionCode, innerMessage: String) :
    XtagesConsoleException(code = code, message = innerMessage)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class IllegalArgumentException(code: ExceptionCode, innerMessage: String) :
    XtagesConsoleException(code = code, message = innerMessage)

class NullValueException(code: ExceptionCode, valueDesc: String, innerMessage: String) :
    XtagesConsoleException(code = code, message = "[$valueDesc] $innerMessage")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class UsageOverLimitException(usageDetails: UsageOverLimit) :
    XtagesConsoleException(
        code = ExceptionCode.USAGE_OVER_LIMIT,
        message = usageToMessage(usageDetails),
        exceptionDetails = usageDetailPojoToUsageDetail.convert(usageDetails)
    ) {

    companion object {
        private fun usageToMessage(details: UsageOverLimit) = when (details) {
            is UsageOverLimitWithDetails -> {
                when (details.resourceType) {
                    ResourceType.PROJECT -> "Projects: ${details.usage} of ${details.limit}"
                    ResourceType.MONTHLY_BUILD_MINUTES -> "Build minutes: ${details.usage} of ${details.limit} min"
                    ResourceType.MONTHLY_DATA_TRANSFER_GBS -> "Data egress: ${details.usage} of ${details.limit} GB"
                }
            }
            is UsageOverLimitBecauseOfSubscriptionStatus -> "The organization's account is not in good standing"
            is UsageOverLimitNoPlan -> "The organization doesn't have a current Plan they are subscribed to."
        }
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class CognitoException(innerMessage: String) :
    XtagesConsoleException(code = ExceptionCode.COGNITO_ERROR, message = innerMessage)
