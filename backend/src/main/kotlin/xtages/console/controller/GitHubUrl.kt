package xtages.console.controller

import org.springframework.web.util.UriComponentsBuilder
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
