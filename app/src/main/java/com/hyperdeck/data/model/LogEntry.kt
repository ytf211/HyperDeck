package com.hyperdeck.data.model

enum class LogLevel(val label: String, val priority: Int) {
    VERBOSE("V", 0),
    DEBUG("D", 1),
    INFO("I", 2),
    WARN("W", 3),
    ERROR("E", 4)
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
)
