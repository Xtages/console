package xtages.console.controller.model

import org.springframework.core.convert.converter.Converter
import xtages.console.controller.api.model.Organization
import xtages.console.controller.api.model.Project
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.enums.ProjectType
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

val projectPojoTypeToProjectTypeConverter = Converter { source: ProjectType -> Project.Type.valueOf(source.name) }

val projectPojoToProjectConverter = Converter { source: ProjectPojo ->
    Project(
        id = source.id,
        name = source.name,
        version = source.version,
        type = projectPojoTypeToProjectTypeConverter.convert(source.type!!),
        passCheckRuleEnabled = source.passCheckRuleEnabled,
    )
}

