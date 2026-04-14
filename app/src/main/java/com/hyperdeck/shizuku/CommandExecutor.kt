package com.hyperdeck.shizuku

import com.hyperdeck.data.repository.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CommandExecutor {

    private const val TAG = "Command"

    data class CommandResult(
        val exitCode: Int,
        val output: String,
        val error: String
    ) {
        val isSuccess: Boolean get() = exitCode == 0
        val fullOutput: String
            get() = buildString {
                if (output.isNotBlank()) append(output)
                if (error.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(error)
                }
            }
    }

    var serviceProvider: (() -> IShellService?)? = null

    suspend fun execute(command: String): CommandResult = withContext(Dispatchers.IO) {
        LogRepository.d(TAG, "$ $command")
        val service = serviceProvider?.invoke()
        if (service == null) {
            LogRepository.e(TAG, "Service not connected, cannot execute")
            return@withContext CommandResult(-1, "", "Shizuku service not connected")
        }
        try {
            val raw = service.execute(command)
            val result = if (raw.startsWith("exit=")) {
                val newlineIdx = raw.indexOf('\n')
                val codeStr = if (newlineIdx > 0) raw.substring(5, newlineIdx) else raw.substring(5)
                val exitCode = codeStr.toIntOrNull() ?: -1
                val output = if (newlineIdx > 0) raw.substring(newlineIdx + 1) else ""
                CommandResult(exitCode, output, "")
            } else if (raw.startsWith("error: ")) {
                CommandResult(-1, "", raw.removePrefix("error: "))
            } else {
                CommandResult(0, raw, "")
            }
            if (result.isSuccess) {
                LogRepository.d(TAG, "OK (${result.output.length} chars)")
            } else {
                LogRepository.w(TAG, "Exit ${result.exitCode}: ${result.error.take(100)}")
            }
            result
        } catch (e: Exception) {
            LogRepository.e(TAG, "Execute failed: ${e.message}")
            CommandResult(-1, "", e.message ?: "Unknown error")
        }
    }
}
