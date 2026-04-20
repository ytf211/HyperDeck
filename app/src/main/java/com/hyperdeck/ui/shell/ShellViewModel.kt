package com.hyperdeck.ui.shell

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyperdeck.HyperDeckApp
import com.hyperdeck.R
import com.hyperdeck.data.model.QuickCommand
import com.hyperdeck.data.model.ShellEntry
import com.hyperdeck.data.model.ShellEntryStatus
import com.hyperdeck.shizuku.CommandExecutor
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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
            val entryId = UUID.randomUUID().toString()
            _isRunning.value = true
            _entries.value = _entries.value + ShellEntry(
                id = entryId,
                command = command,
                output = "",
                status = ShellEntryStatus.PENDING
            )

            val runningJob = launch {
                delay(3_000)
                updateEntry(entryId) { entry ->
                    if (entry.status == ShellEntryStatus.PENDING) {
                        entry.copy(status = ShellEntryStatus.RUNNING)
                    } else {
                        entry
                    }
                }
            }

            try {
                val result = CommandExecutor.execute(command)
                runningJob.cancel()
                updateEntry(entryId) {
                    it.copy(
                        output = result.fullOutput,
                        status = if (result.isSuccess) {
                            ShellEntryStatus.COMPLETED
                        } else {
                            ShellEntryStatus.FAILED
                        }
                    )
                }
            } catch (e: CancellationException) {
                runningJob.cancel()
                updateEntry(entryId) {
                    it.copy(
                        output = getApplication<Application>().getString(R.string.shell_cancelled),
                        status = ShellEntryStatus.CANCELLED
                    )
                }
                throw e
            } finally {
                _isRunning.value = false
                currentJob = null
            }
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

    fun updateQuickCommand(id: String, label: String, command: String) {
        viewModelScope.launch {
            val updated = quickCommands.value
                .map { quickCommand ->
                    if (quickCommand.id == id) {
                        quickCommand.copy(label = label, command = command)
                    } else {
                        quickCommand
                    }
                }
                .mapIndexed { index, quickCommand ->
                    quickCommand.copy(order = index)
                }
            prefsRepo.setQuickCommands(updated)
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
        return _entries.value.joinToString("\n\n") { entry ->
            if (entry.output.isBlank()) {
                "$ ${entry.command}"
            } else {
                "$ ${entry.command}\n${entry.output}"
            }
        }
    }

    private fun updateEntry(entryId: String, transform: (ShellEntry) -> ShellEntry) {
        _entries.value = _entries.value.map { entry ->
            if (entry.id == entryId) transform(entry) else entry
        }
    }
}
