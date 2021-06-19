package xtages.console.exception

import xtages.console.query.enums.ResourceType
import xtages.console.service.UsageOverLimit
import xtages.console.service.UsageOverLimitBecauseOfSubscriptionStatus
import xtages.console.service.UsageOverLimitNoPlan
import xtages.console.service.UsageOverLimitWithDetails

enum class ExceptionCode {
    ORG_NOT_FOUND,
    CHECKOUT_SESSION_NOT_FOUND,
    GH_APP_NOT_ALL_REPOSITORIES_SELECTED,
    NULL_VALUE,
    INVALID_TYPE,
    NOT_EQUALS,
    GH_APP_INSTALLATION_INVALID,
    USER_NOT_FOUND,
    INVALID_ENVIRONMENT,
    INVALID_GITHUB_AVATAR_URL,
    PROJECT_NOT_FOUND,
    BUILD_NOT_FOUND,
    RECIPE_NOT_FOUND,
    USAGE_OVER_LIMIT,
}

/**
 * Base [Throwable] class for the Xtages console app.
 */
sealed class XtagesConsoleException(
    val code: ExceptionCode,
    innerMessage: String,
) : Exception("[$code] $innerMessage")

class NotFoundException(code: ExceptionCode, innerMessage: String) :
    XtagesConsoleException(code, innerMessage)

class IllegalArgumentException(code: ExceptionCode, innerMessage: String) :
    XtagesConsoleException(code, innerMessage)

class NullValueException(code: ExceptionCode, valueDesc: String, innerMessage: String) :
    XtagesConsoleException(code, "[$valueDesc] $innerMessage")

class UsageOverLimitException(details: UsageOverLimit) :
    XtagesConsoleException(ExceptionCode.USAGE_OVER_LIMIT, usageToMessage(details)) {

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

