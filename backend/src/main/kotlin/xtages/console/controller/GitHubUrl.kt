package xtages.console.controller

import org.springframework.web.util.UriComponentsBuilder
import xtages.console.exception.ExceptionCode
import xtages.console.exception.ExceptionCode.*
import xtages.console.exception.ensure
import java.net.URI

/** An URL to a resource in the GitHub UI. */
data class GitHubUrl(val organizationName: String, val repoName: String? = null, val commitHash: String? = null) {
    fun toUri(): URI {
        val componentsBuilder = UriComponentsBuilder.fromUriString("https://github.com")
            .pathSegment(organizationName)
        if (repoName != null) {
            componentsBuilder.pathSegment(repoName)
        }
        if (commitHash != null) {
            componentsBuilder.pathSegment("commit", commitHash)
        }
        return componentsBuilder.build().toUri()
    }

    fun toUriString() = toUri().toString()
}

private val BASE_URI = URI.create("https://avatars.githubusercontent.com/")

/** An URL to a user's GitHub Avatar */
data class GitHubAvatarUrl constructor(val innerUri: URI) {
    companion object {
        private val UNKNOWN = GitHubAvatarUrl("404")


        fun fromUriString(uriString: String?): GitHubAvatarUrl {
            return when {
                uriString != null -> {
                    val uri = UriComponentsBuilder.fromUriString(uriString).build().toUri()
                    ensure.isTrue(
                        value = uri.host == BASE_URI.host && uri.scheme == BASE_URI.scheme,
                        code = INVALID_GITHUB_AVATAR_URL,
                        message = "$uriString is not a valid GitHub Avatar URL"
                    )
                    GitHubAvatarUrl(uri)
                }
                else -> UNKNOWN
            }
        }
    }


    constructor(username: String?) :
        this(UriComponentsBuilder
            .fromUri(BASE_URI)
            .pathSegment(username ?: "404").build().toUri())

    fun toUriString() = innerUri.toString()
}
