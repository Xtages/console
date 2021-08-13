package xtages.console.pojo

import software.amazon.awssdk.services.ecs.model.Service

/**
 * Returns the [Build.id] contained in tags of the [service]
 */
fun Service.buildId(): Long {
    return tags()?.find { tag -> tag.key() == "build_id" }?.value()!!.toLong()
}
