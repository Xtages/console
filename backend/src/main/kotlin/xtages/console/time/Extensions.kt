package xtages.console.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/** @return An [Instant] set at `UTC`. */
fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)

/** @return An [Instant] set at `UTC` in milliseconds.  */
fun LocalDateTime.toUtcMillis() = toUtcInstant().toEpochMilli()

/** @return A [LocalDateTime] set at `UTC` from `this`. */
fun Instant.toUtcLocalDateTime() = LocalDateTime.ofInstant(this, ZoneOffset.UTC)
