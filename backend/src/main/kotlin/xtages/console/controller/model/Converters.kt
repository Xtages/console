package xtages.console.controller.model

import org.springframework.core.convert.converter.Converter
import org.springframework.web.util.UriComponentsBuilder
import xtages.console.controller.GitHubUrl
import xtages.console.controller.api.model.BuildPhase
import xtages.console.controller.api.model.Organization
import xtages.console.controller.api.model.Project
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.enums.ProjectType
import xtages.console.query.tables.pojos.BuildEvent
import xtages.console.time.toUtcMillis
import java.time.ZoneOffset
import xtages.console.query.tables.pojos.Organization as OrganizationPojo
import xtages.console.query.tables.pojos.Project as ProjectPojo

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

/** Converts a [xtages.console.query.tables.pojos.Project] into a [Project]. */
val projectPojoToProjectConverter = Converter { source: ProjectPojo ->
    Project(
        id = source.id!!,
        name = source.name!!,
        version = source.version!!,
        type = projectPojoTypeToProjectTypeConverter.convert(source.type!!)!!,
        passCheckRuleEnabled = source.passCheckRuleEnabled!!,
        ghRepoUrl = GitHubUrl(organizationName = source.organization!!, repoName = source.name).toUriString(),
        organization = source.organization!!,
        builds = emptyList(),
    )
}

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

