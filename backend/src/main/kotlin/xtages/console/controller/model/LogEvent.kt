package xtages.console.controller.model

import java.time.Instant


data class LogEvent(val message: String, val timestamp: Instant)
