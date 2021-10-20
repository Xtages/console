package xtages.console.controller.model

import org.springframework.core.convert.converter.Converter
import org.springframework.web.util.UriComponentsBuilder
import software.amazon.awssdk.services.acm.model.CertificateDetail
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType
import xtages.console.controller.GitHubAvatarUrl
import xtages.console.controller.GitHubUrl
import xtages.console.controller.api.model.*
import xtages.console.controller.api.model.BuildType
import xtages.console.controller.api.model.ResourceType
import xtages.console.query.enums.*
import xtages.console.query.tables.pojos.BuildEvent
import xtages.console.query.tables.pojos.GithubUser
import xtages.console.query.tables.pojos.ProjectDeployment
import xtages.console.query.tables.pojos.Recipe
import xtages.console.service.*
import xtages.console.time.toUtcMillis
import xtages.console.query.enums.ResourceStatus as ResourceStatusPojo
import xtages.console.query.enums.ResourceType as ResourceTypePojo
import xtages.console.query.tables.pojos.Build as BuildPojo
import xtages.console.query.tables.pojos.Organization as OrganizationPojo
import xtages.console.query.tables.pojos.Project as ProjectPojo
import xtages.console.query.tables.pojos.Resource as ResourcePojo
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
                    && source.githubAppInstallationStatus == GithubAppInstallationStatus.ACTIVE,
            gitHubOrganizationType = githubOrganizationTypePojoToGithubOrganizationType
                .convert(source.githubOrganizationType!!)!!,
            githubOauthAuthorized = source.githubOauthAuthorized,
        )
    }

/** Converts a [GithubOrganizationType] into a [Organization.GitHubOrganizationType]. */
val githubOrganizationTypePojoToGithubOrganizationType = Converter { source: GithubOrganizationType ->
    when (source) {
        GithubOrganizationType.INDIVIDUAL -> Organization.GitHubOrganizationType.INDIVIDUAL
        GithubOrganizationType.ORGANIZATION -> Organization.GitHubOrganizationType.ORGANIZATION
    }
}

/** Converts a [ProjectType] into a [Project.Type]. */
val projectPojoTypeToProjectTypeConverter = Converter { source: ProjectType -> Project.Type.valueOf(source.name) }


/** Converts a [ProjectPojo] to a [Project]. */
fun projectPojoToProject(
    source: ProjectPojo,
    recipe: Recipe?,
    percentageOfSuccessfulBuildsInTheLastMonth: Double?,
    builds: List<Build>,
    deployments: List<Deployment>
): Project {
    return Project(
        id = source.id!!,
        name = source.name!!,
        version = recipe?.version!!,
        type = projectPojoTypeToProjectTypeConverter.convert(recipe.projectType!!)!!,
        passCheckRuleEnabled = source.passCheckRuleEnabled!!,
        ghRepoUrl = GitHubUrl(organizationName = source.organization!!, repoName = source.name).toUriString(),
        organization = source.organization!!,
        percentageOfSuccessfulBuildsInTheLastMonth = percentageOfSuccessfulBuildsInTheLastMonth,
        builds = builds,
        deployments = deployments,
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
        resourceType = resourceTypePojoToResourceType.convert(source.resourceType)!!,
        billingModel = resourceTypeToResourceBillingModel.convert(source.resourceType)!!,
        status = status,
        usage = usage,
        limit = limit,
        resetTimestampInMillis = source.resetDateTime?.toUtcMillis()
    )
}

/** Converts a [ResourceTypePojo] to a [ResourceType]. */
val resourceTypePojoToResourceType = Converter { source: ResourceTypePojo ->
    when (source) {
        ResourceTypePojo.PROJECT -> ResourceType.PROJECT
        ResourceTypePojo.BUILD_MINUTES -> ResourceType.BUILD_MINUTES
        ResourceTypePojo.DATA_TRANSFER -> ResourceType.DATA_TRANSFER
        ResourceTypePojo.POSTGRESQL -> ResourceType.POSTGRESQL
    }
}

/** Converts a [ResourceType] to a [ResourceTypePojo]. */
val resourceTypeToResourceTypePojo = Converter { source: ResourceType ->
    when (source) {
        ResourceType.PROJECT -> ResourceTypePojo.PROJECT
        ResourceType.BUILD_MINUTES -> ResourceTypePojo.BUILD_MINUTES
        ResourceType.DATA_TRANSFER -> ResourceTypePojo.DATA_TRANSFER
        ResourceType.POSTGRESQL -> ResourceTypePojo.POSTGRESQL
        else -> throw UnsupportedOperationException("Invalid ResourceType [$source]")
    }
}

/** Converts a [ResourceType] to a [ResourceBillingModel]. */
val resourceTypeToResourceBillingModel = Converter { source: ResourceTypePojo ->
    when (source) {
        ResourceTypePojo.PROJECT -> ResourceBillingModel.TOTAL_NUMBER
        ResourceTypePojo.BUILD_MINUTES -> ResourceBillingModel.MINUTES_PER_MONTH
        ResourceTypePojo.DATA_TRANSFER -> ResourceBillingModel.GB_PER_MONTH
        ResourceTypePojo.POSTGRESQL -> ResourceBillingModel.TOTAL_GB
    }
}

val resourcePojoToResource = Converter { source : ResourcePojo ->
    Resource(
        resourceType = resourceTypePojoToResourceType.convert(source.resourceType!!)!!,
        billingModel = resourceTypeToResourceBillingModel.convert(source.resourceType!!)!!,
        status = resourceStatusPojoToResourceStatus.convert(source.resourceStatus!!)!!,
    )
}

val resourceStatusPojoToResourceStatus = Converter { source: ResourceStatusPojo ->
    when (source) {
        ResourceStatusPojo.REQUESTED -> ResourceStatus.REQUESTED
        ResourceStatusPojo.PROVISIONED -> ResourceStatus.PROVISIONED
        ResourceStatusPojo.WAIT_LISTED -> ResourceStatus.WAIT_LISTED
    }
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
    idToXtagesUser: Map<Int, XtagesUserWithCognitoAttributes>,
    actions: Set<BuildActions>
): Build {
    val initiator = getBuildInitiator(build, usernameToGithubUser, idToXtagesUser)
    return Build(
        id = build.id!!,
        buildNumber = build.buildNumber!!,
        type = BuildType.valueOf(build.type!!.name),
        env = build.environment!!,
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
        actions = actions.toList(),
        phases = events.mapNotNull(buildEventPojoToBuildPhaseConverter::convert)
    )
}

/**
 * Converts a [BuildPojo] to a [Deployment]. This is not an instance of [Converter] since it requires more parameters
 * to work.
 */
fun buildPojoToDeployment(
    source: BuildPojo,
    projectDeployment: ProjectDeployment,
    organization: OrganizationPojo,
    project: ProjectPojo,
    usernameToGithubUser: Map<String, GithubUser>,
    idToXtagesUser: Map<Int, XtagesUserWithCognitoAttributes>,
    customerDeploymentDomain: String,
): Deployment {
    val initiator = getBuildInitiator(source, usernameToGithubUser, idToXtagesUser)

    val serviceUrls = mutableListOf(
        "https://${source.environment}-${project.hash!!.substring(0, 12)}.$customerDeploymentDomain"
    )
    if (source.environment == Environment.PRODUCTION.name.toLowerCase() && project.associatedDomain != null) {
        serviceUrls.add(
            0,
            UriComponentsBuilder.newInstance().scheme("https").host(project.associatedDomain!!).toUriString()
        )
    }
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
        timestampInMillis = source.startTime!!.toUtcMillis(),
        serviceUrls = serviceUrls,
        status = deployStatusToDeploymentStatus.convert(projectDeployment.status!!)!!,
    )
}

val deployStatusToDeploymentStatus = Converter { source: DeployStatus ->
    when (source) {
        DeployStatus.PROVISIONING -> Deployment.Status.STARTING
        DeployStatus.DEPLOYED -> Deployment.Status.RUNNING
        DeployStatus.DRAINING -> Deployment.Status.STOPPING
        DeployStatus.DRAINED -> Deployment.Status.STOPPED
    }
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
