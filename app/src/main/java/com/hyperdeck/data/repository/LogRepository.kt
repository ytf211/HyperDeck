package com.hyperdeck.data.repository

import com.hyperdeck.data.model.LogEntry
import com.hyperdeck.data.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object LogRepository {

    private const val MAX_ENTRIES = 5000

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    private val _minLevel = MutableStateFlow(LogLevel.INFO)
    val minLevel: StateFlow<LogLevel> = _minLevel.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.getDefault())

    fun setEnabled(value: Boolean) {
        _enabled.value = value
    }

    fun setMinLevel(level: LogLevel) {
        _minLevel.value = level
    }

    fun log(level: LogLevel, tag: String, message: String) {
        if (!_enabled.value) return
        if (level.priority < _minLevel.value.priority) return
        val entry = LogEntry(level = level, tag = tag, message = message)
        _entries.update { current ->
            val updated = current + entry
            if (updated.size > MAX_ENTRIES) updated.drop(updated.size - MAX_ENTRIES) else updated
        }
    }

    fun v(tag: String, message: String) = log(LogLevel.VERBOSE, tag, message)
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String) = log(LogLevel.ERROR, tag, message)

    fun clear() {
        _entries.value = emptyList()
    }

    fun export(): String {
        return _entries.value.joinToString("\n") { entry ->
            "${dateFormatter.format(LocalTime.ofInstant(java.time.Instant.ofEpochMilli(entry.timestamp), java.time.ZoneId.systemDefault()))} ${entry.level.label}/${entry.tag}: ${entry.message}"
        }
    }
}
