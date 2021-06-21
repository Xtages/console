package xtages.console.controller.model

import org.springframework.core.convert.converter.Converter
import xtages.console.controller.api.model.*
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.enums.ProjectType
import xtages.console.query.tables.pojos.BuildEvent
import xtages.console.service.*
import xtages.console.time.toUtcMillis
import xtages.console.query.tables.pojos.Organization as OrganizationPojo
import xtages.console.service.UsageDetail as UsageDetailPojo

/**
 * Convert an [OrganizationSubscriptionStatus] to an [Organization.SubscriptionStatus].
 */
val organizationPojoSubscriptionStatusToOrganizationSubscriptionStatusConverter =
    Converter { source: OrganizationSubscriptionStatus ->
        Organization.SubscriptionStatus.valueOf(source.name)
    }

/**
 * Convert an [xtages.console.query.tables.pojos.Organization] to an [Organization].
 */
val organizationPojoToOrganizationConverter =
    Converter { source: OrganizationPojo ->
        Organization(
            name = source.name,
            subscriptionStatus = organizationPojoSubscriptionStatusToOrganizationSubscriptionStatusConverter.convert(
                source.subscriptionStatus!!
            )
        )
    }

/** Converts a [ProjectType] into a [Project.Type]. */
val projectPojoTypeToProjectTypeConverter = Converter { source: ProjectType -> Project.Type.valueOf(source.name) }

/** Converts a [BuildEvent] into a [BuildPhase]. */
val buildEventPojoToBuildPhaseConverter = Converter { source: BuildEvent ->
    BuildPhase(
        id = source.id!!,
        name = source.name!!,
        status = source.status!!,
        message = source.message,
        startTimestampInMillis = source.startTime!!.toUtcMillis(),
        endTimestampInMillis = source.endTime!!.toUtcMillis(),
    )
}

/** Converts an [xtages.console.service.UsageDetail] into an [UsageDetail]. */
val usageDetailPojoToUsageDetail = Converter { source: UsageDetailPojo ->
    var limit = -1L
    var usage = -1L
    var status = UsageDetail.Status.UNDER_LIMIT
    when (source) {
        is UsageOverLimitBecauseOfSubscriptionStatus -> status = UsageDetail.Status.ORG_IN_BAD_STANDING
        is UsageOverLimitNoPlan -> status = UsageDetail.Status.ORG_NOT_SUBSCRIBED_TO_PLAN
        is UsageOverLimitWithDetails -> {
            status = UsageDetail.Status.OVER_LIMIT
            usage = source.usage
            limit = source.limit
        }
        is UsageUnderLimitGrandfathered -> status = UsageDetail.Status.GRANDFATHERED
        is UsageUnderLimitWithDetails -> {
            status = UsageDetail.Status.UNDER_LIMIT
            usage = source.usage
            limit = source.limit
        }
    }
    UsageDetail(
        resourceType = ResourceType.valueOf(source.resourceType.name),
        status = status,
        usage = usage,
        limit = limit,
        resetTimestampInMillis = source.resetDateTime?.toUtcMillis()
    )
}
