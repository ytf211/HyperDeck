package com.hyperdeck.data.repository

import com.hyperdeck.data.model.LogEntry
import com.hyperdeck.data.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogRepository {

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    private val _minLevel = MutableStateFlow(LogLevel.INFO)
    val minLevel: StateFlow<LogLevel> = _minLevel.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

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
        _entries.value = _entries.value + entry
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
            "${dateFormat.format(Date(entry.timestamp))} ${entry.level.label}/${entry.tag}: ${entry.message}"
        }
    }
}
