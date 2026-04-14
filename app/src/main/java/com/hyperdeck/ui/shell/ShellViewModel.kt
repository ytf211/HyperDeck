package com.hyperdeck.ui.shell

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyperdeck.HyperDeckApp
import com.hyperdeck.data.model.QuickCommand
import com.hyperdeck.data.model.ShellEntry
import com.hyperdeck.shizuku.CommandExecutor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ShellViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = (application as HyperDeckApp).preferencesRepository

    private val _entries = MutableStateFlow<List<ShellEntry>>(emptyList())
    val entries: StateFlow<List<ShellEntry>> = _entries.asStateFlow()

    private val _commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private var currentJob: Job? = null

    val quickCommands: StateFlow<List<QuickCommand>> = prefsRepo.quickCommands
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _autoScroll = MutableStateFlow(true)
    val autoScroll: StateFlow<Boolean> = _autoScroll.asStateFlow()

    fun executeCommand(command: String) {
        if (command.isBlank()) return
        _commandHistory.add(command)
        historyIndex = _commandHistory.size
        currentJob = viewModelScope.launch {
            _isRunning.value = true
            val result = CommandExecutor.execute(command)
            _entries.value = _entries.value + ShellEntry(
                command = command,
                output = result.fullOutput.ifBlank { "(no output)" },
                isError = !result.isSuccess
            )
            _isRunning.value = false
        }
    }

    fun cancelCommand() {
        currentJob?.cancel()
        _isRunning.value = false
        CommandExecutor.cancel()
    }

    fun navigateHistoryUp(): String? {
        if (_commandHistory.isEmpty()) return null
        historyIndex = (historyIndex - 1).coerceAtLeast(0)
        return _commandHistory[historyIndex]
    }

    fun navigateHistoryDown(): String? {
        if (_commandHistory.isEmpty()) return null
        historyIndex = (historyIndex + 1).coerceAtMost(_commandHistory.size)
        return if (historyIndex < _commandHistory.size) _commandHistory[historyIndex] else ""
    }

    fun clearHistory() {
        _entries.value = emptyList()
    }

    fun setAutoScroll(enabled: Boolean) {
        _autoScroll.value = enabled
    }

    fun addQuickCommand(label: String, command: String) {
        viewModelScope.launch {
            val current = quickCommands.value.toMutableList()
            val id = UUID.randomUUID().toString()
            current.add(QuickCommand(id, label, command, current.size))
            prefsRepo.setQuickCommands(current)
        }
    }

    fun removeQuickCommand(id: String) {
        viewModelScope.launch {
            val updated = quickCommands.value.filter { it.id != id }
                .mapIndexed { index, cmd -> cmd.copy(order = index) }
            prefsRepo.setQuickCommands(updated)
        }
    }

    fun getAllText(): String {
        return _entries.value.joinToString("\n\n") { "$ ${it.command}\n${it.output}" }
    }
}
