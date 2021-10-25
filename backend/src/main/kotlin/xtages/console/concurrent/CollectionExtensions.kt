package xtages.console.concurrent

import java.util.concurrent.CompletableFuture
import kotlin.streams.toList

/**
 * Waits for a [Collection] of [CompletableFuture] of type [T] and returns a [List<T>] with their results.
 */
fun <T> Collection<CompletableFuture<T>>.waitForAll(): List<T> {
    return stream().map(CompletableFuture<T>::join).toList()
}
