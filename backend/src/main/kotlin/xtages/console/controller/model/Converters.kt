package xtages.console.controller.model

import org.springframework.core.convert.converter.Converter
import software.amazon.awssdk.services.acm.model.CertificateDetail
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType
import xtages.console.controller.GitHubAvatarUrl
import xtages.console.controller.GitHubUrl
import xtages.console.controller.api.model.*
import xtages.console.controller.api.model.Deployment.Status.RUNNING
import xtages.console.controller.api.model.Deployment.Status.STOPPED
import xtages.console.query.enums.DeployStatus
import xtages.console.query.enums.GithubAppInstallationStatus
import xtages.console.query.enums.OrganizationSubscriptionStatus
import xtages.console.query.enums.ProjectType
import xtages.console.query.tables.pojos.BuildEvent
import xtages.console.query.tables.pojos.GithubUser
import xtages.console.service.*
import xtages.console.time.toUtcMillis
import xtages.console.query.tables.pojos.Build as BuildPojo
import xtages.console.query.tables.pojos.Organization as OrganizationPojo
import xtages.console.query.tables.pojos.Project as ProjectPojo
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
            name = source.name!!,
            subscriptionStatus = organizationPojoSubscriptionStatusToOrganizationSubscriptionStatusConverter.convert(
                source.subscriptionStatus!!
            )!!,
            githubAppInstalled = source.githubAppInstallationId != null
                    && source.githubAppInstallationStatus == GithubAppInstallationStatus.ACTIVE
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

/**
 * Extracts the [CertificateStatus] and first [ResourceRecord] (of [DomainValidation]) from [CertificateDetail] and
 * converts it to an [AssociatedDomain].
 */
val certificateDetailToAssociatedDomain = Converter { source: CertificateDetail ->
    val domainValidationRecord = source.domainValidationOptions().first().resourceRecord()
    AssociatedDomain(
        name = source.domainName(),
        certificateStatus = AssociatedDomain.CertificateStatus.valueOf(source.status().name),
        validationRecord = DomainValidationRecord(
            name = domainValidationRecord.name(),
            recordType = DomainValidationRecord.RecordType.valueOf(domainValidationRecord.type().name),
            value = domainValidationRecord.value(),
        )
    )
}

/**
 * Converts an [XtagesUserWithCognitoAttributes] into an [User].
 */
val xtagesUserWithCognitoAttributesToUser = Converter { source: XtagesUserWithCognitoAttributes ->
    User(
        id = source.user.id!!,
        username = source.attrs["email"]!!,
        name = source.attrs["name"]!!,
        status = when (source.userStatus) {
            UserStatusType.RESET_REQUIRED -> User.Status.EXPIRED
            UserStatusType.FORCE_CHANGE_PASSWORD -> User.Status.INVITED
            else -> User.Status.ACTIVE
        },
        isOwner = source.user.isOwner!!,
    )
}

/**
 * Converts a [BuildPojo] to a [Build]. This is not an instance of [Converter] since it requires more parameters
 * to work.
 */
fun buildPojoToBuild(
    organization: OrganizationPojo,
    project: ProjectPojo,
    build: BuildPojo,
    events: List<BuildEvent>,
    usernameToGithubUser: Map<String, GithubUser>,
    idToXtagesUser: Map<Int, XtagesUserWithCognitoAttributes>
): Build {
    val initiator = getBuildInitiator(build, usernameToGithubUser, idToXtagesUser)
    return Build(
        id = build.id!!,
        buildNumber = build.buildNumber!!,
        type = BuildType.valueOf(build.type!!.name),
        status = Build.Status.valueOf(build.status!!.name),
        initiatorName = initiator.name,
        initiatorEmail = initiator.email,
        initiatorAvatarUrl = initiator.avatarUrl.toUriString(),
        commitHash = build.commitHash!!,
        commitUrl = GitHubUrl(
            organizationName = organization.name!!,
            repoName = project.name,
            commitHash = build.commitHash
        ).toUriString(),
        startTimestampInMillis = build.startTime!!.toUtcMillis(),
        endTimestampInMillis = build.endTime?.toUtcMillis(),
        phases = events.mapNotNull(buildEventPojoToBuildPhaseConverter::convert)
    )
}

/**
 * Converts a [BuildPojo] to a [Deployment]. This is not an instance of [Converter] since it requires more parameters
 * to work.
 */
fun buildPojoToDeployment(
    source: BuildPojo,
    organization: OrganizationPojo,
    project: ProjectPojo,
    usernameToGithubUser: Map<String, GithubUser>,
    idToXtagesUser: Map<Int, XtagesUserWithCognitoAttributes>,
    projectDeploymentStatus: DeployStatus? = null,
    domain: String,
): Deployment {
    val initiator = getBuildInitiator(source, usernameToGithubUser, idToXtagesUser)

    return Deployment(
        id = source.id!!,
        initiatorName = initiator.name,
        initiatorEmail = initiator.email,
        initiatorAvatarUrl = initiator.avatarUrl.toUriString(),
        commitHash = source.commitHash!!,
        commitUrl = GitHubUrl(
            organizationName = organization.name!!,
            repoName = project.name,
            commitHash = source.commitHash
        ).toUriString(),
        env = source.environment!!,
        timestampInMillis = source.endTime!!.toUtcMillis(),
        serviceUrl = "https://${source.environment}-${project.hash!!.substring(0, 12)}.${domain}",
        status = if (projectDeploymentStatus == DeployStatus.DEPLOYED) RUNNING else STOPPED,
    )
}

private fun getBuildInitiator(
    build: BuildPojo,
    usernameToGithubUser: Map<String, GithubUser>,
    idToXtagesUser: Map<Int, XtagesUserWithCognitoAttributes>
): Initiator {
    val initiatorName = when (build.userId) {
        null -> usernameToGithubUser[build.githubUserUsername!!]?.name
        else -> idToXtagesUser[build.userId]?.attrs?.get("name")
    } ?: "Unknown"
    val initiatorEmail = when (build.userId) {
        null -> usernameToGithubUser[build.githubUserUsername!!]?.email
        else -> idToXtagesUser[build.userId]?.attrs?.get("email")
    } ?: ""
    val initiatorAvatarUrl = when (build.userId) {
        null -> GitHubAvatarUrl(usernameToGithubUser[build.githubUserUsername!!]?.username)
        else -> GitHubAvatarUrl.fromUriString(idToXtagesUser[build.userId]?.githubUser?.avatarUrl)
    }
    return Initiator(name = initiatorName, email = initiatorEmail, avatarUrl = initiatorAvatarUrl)
}

private data class Initiator(val name: String, val email: String, val avatarUrl: GitHubAvatarUrl)
