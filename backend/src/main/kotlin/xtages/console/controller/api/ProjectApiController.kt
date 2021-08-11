package xtages.console.controller.api

import mu.KotlinLogging
import org.apache.commons.validator.routines.DomainValidator
import org.apache.commons.validator.routines.UrlValidator
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus.*
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.config.ConsoleProperties
import xtages.console.controller.GitHubUrl
import xtages.console.controller.api.model.*
import xtages.console.controller.model.*
import xtages.console.dao.*
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ExceptionCode.RECIPE_NOT_FOUND
import xtages.console.exception.ExceptionCode.USER_NOT_FOUND
import xtages.console.exception.ensure
import xtages.console.query.enums.BuildStatus
import xtages.console.query.enums.ProjectType
import xtages.console.query.enums.ResourceType
import xtages.console.query.tables.daos.*
import xtages.console.query.tables.pojos.BuildEvent
import xtages.console.query.tables.pojos.GithubUser
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.XtagesUser
import xtages.console.service.*
import xtages.console.service.aws.*
import java.net.URL
import java.util.*
import javax.validation.ValidationException
import xtages.console.query.enums.BuildType as BuildTypePojo
import xtages.console.query.tables.pojos.Build as BuildPojo
import xtages.console.query.tables.pojos.Project as ProjectPojo

private val logger = KotlinLogging.logger { }

private val urlValidator = UrlValidator(UrlValidator.NO_FRAGMENTS)
private val domainValidator = DomainValidator.getInstance()

@Controller
class ProjectApiController(
    private val userDao: XtagesUserDao,
    private val githubUserDao: GithubUserDao,
    private val organizationDao: OrganizationDao,
    private val projectDao: ProjectDao,
    private val authenticationService: AuthenticationService,
    private val userService: UserService,
    private val gitHubService: GitHubService,
    private val awsService: AwsService,
    private val codeBuildService: CodeBuildService,
    private val usageService: UsageService,
    private val acmService: AcmService,
    private val buildDao: BuildDao,
    private val buildEventDao: BuildEventDao,
    private val recipeDao: RecipeDao,
    private val rdsService: RdsService,
    private val ecsService: EcsService,
    private val projectDeploymentDao: ProjectDeploymentDao,
    private val consoleProperties: ConsoleProperties,
) : ProjectApiControllerBase {

    override fun getProject(
        projectName: String,
        includeBuilds: Boolean,
        includeDeployments: Boolean,
        includeSuccessfulBuildPercentage: Boolean,
    ): ResponseEntity<Project> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val project = projectDao.fetchOneByNameAndOrganization(orgName = organization.name!!, projectName = projectName)
        val percentageOfSuccessfulBuildsInTheLastMonth =
            when {
                includeSuccessfulBuildPercentage -> buildDao.findPercentageOfSuccessfulBuildsInMonth(
                    organizationName = organization.name!!,
                    projectName = projectName
                )
                else -> null
            }
        if (includeBuilds || includeDeployments) {
            val comparator = BuildComparator.reversed()
            val buildPojos = buildDao.fetchByProjectId(project.id!!).sortedWith(comparator)
            val buildIdToBuild = buildPojos.associateBy { build -> build.id!! }
            val usernameToGithubUser = githubUserDao.findFromBuilds(buildPojos)
            val idToXtagesUser = userService.findFromBuilds(buildPojos)
            val buildIdToBuildEvents = buildEventDao
                .fetchByBuildId(*buildIdToBuild.keys.toLongArray())
                .groupBy { buildEvent -> buildEvent.buildId!! }

            val builds = when {
                includeBuilds -> buildPojos.map { build ->
                    val events = buildIdToBuildEvents[build.id] ?: emptyList()
                    buildPojoToBuild(
                        organization = organization,
                        project = project,
                        build = build,
                        events = events,
                        usernameToGithubUser = usernameToGithubUser,
                        idToXtagesUser = idToXtagesUser
                    )
                }
                else -> emptyList()
            }

            val deployments = when {
                includeDeployments -> findLatestDeployments(
                    organization = organization,
                    project = project,
                    builds = buildPojos,
                    usernameToGithubUser = usernameToGithubUser,
                    idToXtagesUser = idToXtagesUser
                )
                else -> emptyList()
            }
            return ResponseEntity.ok(
                convertProjectPojoToProject(
                    source = project,
                    percentageOfSuccessfulBuildsInTheLastMonth = percentageOfSuccessfulBuildsInTheLastMonth,
                    builds = builds,
                    deployments = deployments
                )
            )
        }
        return ResponseEntity.ok(
            convertProjectPojoToProject(
                source = project,
                percentageOfSuccessfulBuildsInTheLastMonth = percentageOfSuccessfulBuildsInTheLastMonth,
            )
        )

    }

    override fun getProjects(includeLastBuild: Boolean): ResponseEntity<List<Project>> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val projects = projectDao.fetchByOrganization(organization.name!!).toMutableList()
        if (includeLastBuild) {
            val projectIdToLatestBuild = buildDao
                .fetchLatestByProject(projects)
                .associateBy { build -> build.projectId!! }
            val usernameToGithubUser = githubUserDao.findFromBuilds(projectIdToLatestBuild.values.toList())
            val idToXtagesUser = userService.findFromBuilds(projectIdToLatestBuild.values.toList())

            val latestBuildEvents = buildEventDao
                .fetchByBuildId(*projectIdToLatestBuild.values.mapNotNull { build -> build.id }.toLongArray())
            val convertedProjects = projects
                .associateWith { project -> projectIdToLatestBuild[project.id] }
                .map { entry ->
                    val buildPojo = entry.value
                    val project = entry.key
                    val convertedBuild = buildPojo?.let { build ->
                        buildPojoToBuild(
                            organization = organization,
                            project = project,
                            build = build,
                            events = latestBuildEvents,
                            usernameToGithubUser = usernameToGithubUser,
                            idToXtagesUser = idToXtagesUser,
                        )
                    }
                    convertProjectPojoToProject(
                        source = project,
                        builds = if (convertedBuild != null) listOf(convertedBuild) else emptyList()
                    )
                }.sortedWith { projectA, projectB ->
                    val startTimestampA = projectA.builds.singleOrNull()?.startTimestampInMillis
                    val startTimestampB = projectB.builds.singleOrNull()?.startTimestampInMillis
                    when {
                        startTimestampA != null && startTimestampB != null -> startTimestampA.compareTo(startTimestampB)
                        startTimestampA != null && startTimestampB == null -> 1
                        startTimestampA == null && startTimestampB != null -> -1
                        else -> projectA.name.compareTo(projectB.name)
                    }
                }
            return ResponseEntity.ok(convertedProjects)
        }
        return ResponseEntity.ok(
            projectDao.fetchByOrganization(organization.name!!).map(this::convertProjectPojoToProject)
        )
    }

    private fun findLatestDeployments(
        organization: Organization,
        project: ProjectPojo,
        builds: List<BuildPojo>,
        usernameToGithubUser: Map<String, GithubUser>,
        idToXtagesUser: Map<Int, XtagesUserWithCognitoAttributes>
    ): List<Deployment> {
        val prodDeployment = builds.firstOrNull { build ->
            build.type == BuildTypePojo.CD && build.status == BuildStatus.SUCCEEDED && build.environment == "production"
        }
        val stagingDeployment = builds.firstOrNull { build ->
            build.type == BuildTypePojo.CD && build.status == BuildStatus.SUCCEEDED && build.environment == "staging"
        }
        val deploymentBuilds = listOfNotNull(prodDeployment, stagingDeployment)
        val projectDeploymentStatus = projectDeploymentDao.fetchLatestByBuilds(deploymentBuilds)

        return (deploymentBuilds zip projectDeploymentStatus).map { tuple ->
            buildPojoToDeployment(
                source = tuple.first,
                organization = organization,
                project = project,
                usernameToGithubUser = usernameToGithubUser,
                idToXtagesUser = idToXtagesUser,
                projectDeploymentStatus = tuple.second.status,
                customerDeploymentDomain = consoleProperties.customerDeploymentDomain
            )
        }
    }

    override fun createProject(createProjectReq: CreateProjectReq): ResponseEntity<Project> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        usageService.checkUsageIsBelowLimit(organization = organization, resourceType = ResourceType.PROJECT)
        val user = ensure.foundOne(
            operation = { userDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId.id) },
            code = USER_NOT_FOUND,
            message = "User not found"
        )
        val recipe = recipeDao.fetchByProjectTypeAndVersion(
            ProjectType.valueOf(createProjectReq.type.name),
            createProjectReq.version
        )

        val projectPojo = ProjectPojo(
            name = createProjectReq.name,
            organization = organization.name,
            recipe = recipe.id,
            passCheckRuleEnabled = createProjectReq.passCheckRuleEnabled,
            user = user.id,
            hash = MD5.md5("${organization.hash}${UUID.randomUUID()}"),
        )
        if (gitHubService.getRepositoryForProject(project = projectPojo, organization = organization) == null) {
            projectDao.insert(projectPojo)
            gitHubService.createRepoForProject(
                project = projectPojo,
                recipe = recipe,
                organization = organization,
                description = createProjectReq.description
            )
            awsService.registerProject(project = projectPojo, recipe = recipe, organization = organization)
            return ResponseEntity.status(CREATED).body(convertProjectPojoToProject(projectPojo))
        }
        logger.error { "Cannot create project [${projectPojo.name}] for organization [${organization.name}]. The repo already exists in GitHub" }
        return ResponseEntity.status(CONFLICT).build()
    }

    override fun ci(projectName: String, ciReq: CIReq): ResponseEntity<CI> {
        val (user, organization, project) = checkRepoBelongsToOrg(projectName)
        val recipe = ensure.foundOne(
            operation = { recipeDao.fetchOneById(project.recipe!!) },
            code = RECIPE_NOT_FOUND
        )
        ensureDbIsAvailable(organization)
        val startCodeBuildResponse = codeBuildService.startCodeBuildProject(
            gitHubAppToken = gitHubService.appToken(organization),
            user = user,
            project = project,
            recipe = recipe,
            organization = organization,
            commitHash = ciReq.commitHash,
            codeBuildType = CodeBuildType.CI,
            environment = "dev",
        )

        return ResponseEntity.ok(CI(id = startCodeBuildResponse.second.id))
    }

    override fun deploy(projectName: String, cdReq: CDReq): ResponseEntity<CD> {
        val (user, organization, project) = checkRepoBelongsToOrg(projectName)

        val userName = ensure.notNull(
            authenticationService.jwt.getClaim<String>("name"),
            "name"
        )

        ensureDbIsAvailable(organization)

        val tag = gitHubService.tagProject(organization, project, userName)
        val recipe = ensure.foundOne(
            operation = { recipeDao.fetchOneById(project.recipe!!) },
            code = RECIPE_NOT_FOUND
        )

        val startCodeBuildResponse = codeBuildService.startCodeBuildProject(
            gitHubAppToken = gitHubService.appToken(organization),
            user = user,
            project = project,
            recipe = recipe,
            organization = organization,
            commitHash = cdReq.commitHash,
            codeBuildType = CodeBuildType.CD,
            environment = "staging",
            gitHubProjectTag = tag,
        )

        return ResponseEntity.ok(CD(id = startCodeBuildResponse.second.id))
    }

    override fun promote(projectName: String): ResponseEntity<CD> {
        val (user, organization, project) = checkRepoBelongsToOrg(projectName)

        val recipe = ensure.foundOne(
            operation = { recipeDao.fetchOneById(project.recipe!!) },
            code = RECIPE_NOT_FOUND
        )
        val lastDeployment = buildDao.findLatestDeploy(
            organizationName = organization.name!!,
            projectName = project.name!!
        )
        if (lastDeployment != null) {
            val startCodeBuildResponse = codeBuildService.startCodeBuildProject(
                gitHubAppToken = gitHubService.appToken(organization),
                user = user,
                project = project,
                recipe = recipe,
                organization = organization,
                commitHash = lastDeployment.commitHash!!,
                codeBuildType = CodeBuildType.CD,
                environment = "production",
                gitHubProjectTag = lastDeployment.tag!!,
            )

            return ResponseEntity.ok(CD(id = startCodeBuildResponse.second.id))
        }
        return ResponseEntity(BAD_REQUEST)
    }

    override fun rollback(projectName: String): ResponseEntity<CD> {
        val (user, organization, project) = checkRepoBelongsToOrg(projectName)

        val recipe = ensure.foundOne(
            operation = { recipeDao.fetchOneById(project.recipe!!) },
            code = RECIPE_NOT_FOUND
        )
        val lastTwoPromotions = buildDao.findLastTwoPreviousPromotions(
            organizationName = organization.name!!,
            projectName = project.name!!,
        )
        if (lastTwoPromotions.size == 2) {
            val lastPromotion = lastTwoPromotions.first()
            val previousPromotion = lastTwoPromotions.last()
            val startCodeBuildResponse = codeBuildService.startCodeBuildProject(
                gitHubAppToken = gitHubService.appToken(organization),
                user = user,
                project = project,
                recipe = recipe,
                organization = organization,
                commitHash = previousPromotion.commitHash!!,
                codeBuildType = CodeBuildType.CD,
                environment = "production",
                gitHubProjectTag = lastPromotion.tag!!,
                previousGitHubProjectTag = previousPromotion.tag!!,
            )

            return ResponseEntity.ok(CD(id = startCodeBuildResponse.second.id))
        }
        return ResponseEntity(BAD_REQUEST)
    }

    /**
     * Retrieve logs from CloudWatch given a [Project] an [BuildEvent] id and
     * an operation id ([CodeBuildType])
     */
    override fun buildLogs(projectName: String, buildId: Long): ResponseEntity<Logs> {
        val (_, organization, project) = checkRepoBelongsToOrg(projectName)
        val build = ensure.foundOne(
            operation = { buildDao.fetchById(buildId).first() },
            code = ExceptionCode.BUILD_NOT_FOUND,
            lazyMessage = { "Build with id [$buildId] was not found" }
        )

        val logs = codeBuildService.getLogsFor(
            codeBuildType = CodeBuildType.valueOf(build.type!!.name),
            build = build,
            project = project,
            organization = organization
        )
        return ResponseEntity.ok(logs)
    }

    override fun getDeployLogs(
        projectName: String,
        buildId: Long,
        env: String,
        startTimeInMillis: Long?,
        endTimeInMillis: Long?,
        token: String?
    ): ResponseEntity<Logs> {
        val (_, _, project) = checkRepoBelongsToOrg(projectName)
        val logs = ecsService.getLogsFor(
            env = env,
            buildId = buildId,
            project = project,
            startTimeInMillis = startTimeInMillis,
            endTimeInMillis = endTimeInMillis,
            token = token,
        )
        return ResponseEntity.ok(logs)
    }

    override fun updateProjectSettings(
        projectName: String,
        updateProjectSettingsReq: UpdateProjectSettingsReq
    ): ResponseEntity<ProjectSettings> {
        val (_, _, project) = checkRepoBelongsToOrg(projectName)
        if (project.associatedDomain != null) {
            return ResponseEntity(CONFLICT)
        }
        var domainName = updateProjectSettingsReq.associatedDomainName
        if (projectDao.fetchByAssociatedDomain(domainName).isNotEmpty()) {
            throw ValidationException("Invalid domain name")
        }
        if (urlValidator.isValid(domainName)) {
            domainName = URL(domainName).host
        } else if (!domainValidator.isValid(domainName) &&
            // It's possible to associate wildcard domains of the form "*.mydomain.com", however DomainValidator doesn't
            // recognize those a valid, so we check if the domain name starts with "*." and strip it and try to validate
            // again.
            !(domainName.startsWith("*.") && domainValidator.isValid(domainName.substring(2)))
        ) {
            throw ValidationException("Invalid domain name")
        }
        val certificateDetail = acmService.requestCertificate(
            project = project,
            domainName = domainName
        )
        project.certArn = certificateDetail.certificateArn()
        project.associatedDomain = certificateDetail.domainName()
        projectDao.update(project)
        return ResponseEntity.ok(
            ProjectSettings(
                projectId = project.id!!,
                associatedDomain = certificateDetailToAssociatedDomain.convert(certificateDetail)
            )
        )
    }

    override fun getProjectSettings(projectName: String): ResponseEntity<ProjectSettings> {
        val (_, _, project) = checkRepoBelongsToOrg(projectName)
        if (project.certArn != null) {
            val certificateDetail = acmService.getCertificateDetail(certificateArn = project.certArn!!)
            ensure.isTrue(
                value = project.associatedDomain == certificateDetail.domainName(),
                code = ExceptionCode.INVALID_DOMAIN,
                message = "The project's associated domain and the project's certificate domain don't match"
            )
            return ResponseEntity.ok(
                ProjectSettings(
                    projectId = project.id!!,
                    associatedDomain = certificateDetailToAssociatedDomain.convert(certificateDetail)
                )
            )
        }
        return ResponseEntity.ok(ProjectSettings(projectId = project.id!!))
    }

    private fun checkRepoBelongsToOrg(projectName: String): Triple<XtagesUser, Organization, ProjectPojo> {
        val user = ensure.foundOne(
            operation = { userDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId.id) },
            code = USER_NOT_FOUND,
            message = "User not found"
        )
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val project = projectDao.fetchOneByNameAndOrganization(organization.name!!, projectName)
        return Triple(user, organization, project)
    }

    /** Converts a [xtages.console.query.tables.pojos.Project] into a [Project]. */
    @Cacheable
    fun convertProjectPojoToProject(
        source: ProjectPojo,
        percentageOfSuccessfulBuildsInTheLastMonth: Double? = null,
        builds: List<Build> = emptyList(),
        deployments: List<Deployment> = emptyList()
    ): Project {
        val recipe = recipeDao.fetchOneById(source.recipe!!)
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

    private fun ensureDbIsAvailable(organization: Organization) {
        if (organization.rdsEndpoint == null) {
            val endpoint = ensure.notNull(
                rdsService.getEndpoint(organization),
                "Database it's still being provisioned. Please try again in 5 minutes"
            )
            organization.rdsEndpoint = endpoint
            organizationDao.merge(organization)
        }
    }
}

/**
 * A [Comparator<Build>] that given build A and build B:
 *
 *  * if both A and B aren't completed (endTime is null) then compare their startTime
 *  * if A isn't completed but B is, then B is more recent
 *  * if A is completed but B isn't, then A is more recent
 *  * if both A and B are completed then compare their endTime
 */
object BuildComparator : Comparator<BuildPojo> {
    override fun compare(
        buildA: xtages.console.query.tables.pojos.Build,
        buildB: xtages.console.query.tables.pojos.Build
    ): Int {
        val startTimeA = buildA.startTime!!
        val startTimeB = buildB.startTime!!
        val endTimeA = buildA.endTime
        val endTimeB = buildB.endTime
        return when {
            endTimeA == null && endTimeB == null -> startTimeA.compareTo(startTimeB)
            endTimeA != null && endTimeB == null -> -1
            endTimeA == null && endTimeB != null -> 1
            endTimeA != null && endTimeB != null -> endTimeA.compareTo(endTimeB)
            else -> 0
        }
    }
}
