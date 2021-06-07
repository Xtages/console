package xtages.console.controller.api

import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.controller.GitHubAvatarUrl
import xtages.console.controller.GitHubUrl
import xtages.console.controller.api.model.*
import xtages.console.controller.api.model.Build.Status
import xtages.console.controller.model.CodeBuildType
import xtages.console.controller.model.buildEventPojoToBuildPhaseConverter
import xtages.console.controller.model.projectPojoTypeToProjectTypeConverter
import xtages.console.dao.fetchByProjectTypeAndVersion
import xtages.console.dao.fetchLatestByProject
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.dao.fetchOneByNameAndOrganization
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ExceptionCode.RECIPE_NOT_FOUND
import xtages.console.exception.ExceptionCode.USER_NOT_FOUND
import xtages.console.exception.ensure
import xtages.console.query.enums.ProjectType
import xtages.console.query.tables.daos.*
import xtages.console.query.tables.pojos.BuildEvent
import xtages.console.query.tables.pojos.GithubUser
import xtages.console.query.tables.pojos.Organization
import xtages.console.query.tables.pojos.XtagesUser
import xtages.console.service.AuthenticationService
import xtages.console.service.GitHubService
import xtages.console.service.UserService
import xtages.console.service.XtagesUserWithCognitoAttributes
import xtages.console.service.aws.AwsService
import xtages.console.service.aws.CodeBuildService
import xtages.console.time.toUtcMillis
import xtages.console.query.tables.pojos.Build as BuildPojo
import xtages.console.query.tables.pojos.Project as ProjectPojo

private val logger = KotlinLogging.logger { }

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
    private val buildDao: BuildDao,
    private val buildEventDao: BuildEventDao,
    private val recipeDao: RecipeDao,
) : ProjectApiControllerBase {

    override fun getProject(projectName: String, includeBuilds: Boolean): ResponseEntity<Project> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val project = projectDao.fetchOneByNameAndOrganization(orgName = organization.name!!, projectName = projectName)
        val projectPojo = projectPojoToProjectConverter(project)
        if (includeBuilds) {
            val comparator = BuildComparator.reversed()
            val buildPojos = buildDao.fetchByProjectId(project.id!!).sortedWith(comparator)
            val buildIdToBuild = buildPojos.associateBy { build -> build.id!! }
            val usernameToGithubUser = getUsernameToGithubUserMap(*buildPojos.toTypedArray())
            val idToXtagesUser = getUserIdToXtagesUserMap(*buildPojos.toTypedArray())
            val buildIdToBuildEvents = buildEventDao
                .fetchByBuildId(*buildIdToBuild.keys.toLongArray())
                .groupBy { buildEvent -> buildEvent.buildId!! }

            val builds = buildPojos.map { build ->
                val events = buildIdToBuildEvents[build.id]!!
                buildPojoToBuild(
                    organization = organization,
                    project = project,
                    build = build,
                    events = events,
                    usernameToGithubUser = usernameToGithubUser,
                    idToXtagesUser = idToXtagesUser
                )
            }
            return ResponseEntity.ok(projectPojo.copy(builds = builds))
        }
        return ResponseEntity.ok(projectPojo)
    }

    override fun getProjects(includeLastBuild: Boolean): ResponseEntity<List<Project>> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val projects = projectDao.fetchByOrganization(organization.name!!).toMutableList()
        if (includeLastBuild) {
            val projectIdToLatestBuild = buildDao
                .fetchLatestByProject(projects)
                .associateBy { build -> build.projectId!! }
            val usernameToGithubUser = getUsernameToGithubUserMap(*projectIdToLatestBuild.values.toTypedArray())
            val idToXtagesUser = getUserIdToXtagesUserMap(*projectIdToLatestBuild.values.toTypedArray())

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
                    var convertedProject = projectPojoToProjectConverter(project)
                    if (convertedBuild != null) {
                        convertedProject = convertedProject.copy(builds = listOf(convertedBuild))
                    }
                    convertedProject
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
            projectDao.fetchByOrganization(organization.name!!).map { projectPojoToProjectConverter(it) }
        )
    }

    private fun getUserIdToXtagesUserMap(vararg builds: BuildPojo): Map<Int, XtagesUserWithCognitoAttributes> {
        val xtagesUserIds = builds.mapNotNull { build -> build.userId }
        return when {
            xtagesUserIds.isNotEmpty() -> userService.findCognitoUsersByXtagesUserId(*xtagesUserIds.toIntArray())
                .associateBy { user -> user.user.id!! }
            else -> emptyMap()
        }
    }

    private fun getUsernameToGithubUserMap(vararg builds: BuildPojo): Map<String, GithubUser> {
        val githubUserNames = builds.mapNotNull { build -> build.githubUserUsername }
        return when {
            githubUserNames.isNotEmpty() -> githubUserDao.fetchByEmail(*githubUserNames.toTypedArray())
                .associateBy { githubUser -> githubUser.username!! }
            else -> emptyMap()
        }
    }

    private fun buildPojoToBuild(
        organization: Organization,
        project: ProjectPojo,
        build: BuildPojo,
        events: List<BuildEvent>,
        usernameToGithubUser: Map<String, GithubUser>,
        idToXtagesUser: Map<Int, XtagesUserWithCognitoAttributes>
    ): Build {
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

        return Build(
            id = build.id!!,
            type = BuildType.valueOf(build.type!!.name),
            status = Status.valueOf(build.status!!.name),
            initiatorName = initiatorName,
            initiatorEmail = initiatorEmail,
            initiatorAvatarUrl = initiatorAvatarUrl.toUriString(),
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

    override fun createProject(createProjectReq: CreateProjectReq): ResponseEntity<Project> {
        val user = ensure.foundOne(
            operation = { userDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId.id) },
            code = USER_NOT_FOUND,
            message = "User not found"
        )
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
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
            return ResponseEntity.status(CREATED).body(projectPojoToProjectConverter(projectPojo))
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
        val startCodeBuildResponse = codeBuildService.startCodeBuildProject(
            gitHubAppToken = gitHubService.appToken(organization),
            user = user,
            project = project,
            recipe = recipe,
            organization = organization,
            commitHash = ciReq.commitHash,
            codeBuildType = CodeBuildType.CI
        )

        return ResponseEntity.ok(CI(id = startCodeBuildResponse.second.id))
    }

    override fun cd(projectName: String, cdReq: CDReq): ResponseEntity<CD> {
        val (user, organization, project) = checkRepoBelongsToOrg(projectName)

        val userName = ensure.notNull(
            authenticationService.jwt.getClaim<String>("name"),
            "name"
        )
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
            environment = cdReq.env,
            gitHubProjectTag = tag,
        )

        return ResponseEntity.ok(CD(id = startCodeBuildResponse.second.id))
    }

    /**
     * Retrieve logs from CloudWatch given a [Project] an [BuildEvent] id and
     * an operation id ([CodeBuildType])
     */
    override fun logs(projectName: String, buildId: Long): ResponseEntity<CILogs> {
        val (_, organization, project) = checkRepoBelongsToOrg(projectName)
        val build = ensure.foundOne(
            operation = { buildDao.fetchById(buildId).first() },
            code = ExceptionCode.BUILD_NOT_FOUND,
            lazyMessage = { "Build with id [$buildId] was not found" }
        )

        val logs = codeBuildService.getLogsFor(CodeBuildType.valueOf(build.type!!.name), build, project, organization)
        return ResponseEntity.ok(CILogs(events = logs))
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
    fun projectPojoToProjectConverter(source: ProjectPojo): Project {
        val recipe = recipeDao.fetchOneById(source.recipe!!)
        return Project(
            id = source.id!!,
            name = source.name!!,
            version = recipe?.version!!,
            type = projectPojoTypeToProjectTypeConverter.convert(recipe.projectType!!)!!,
            passCheckRuleEnabled = source.passCheckRuleEnabled!!,
            ghRepoUrl = GitHubUrl(organizationName = source.organization!!, repoName = source.name).toUriString(),
            organization = source.organization!!,
            builds = emptyList(),
        )
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
object BuildComparator: Comparator<BuildPojo> {
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
