package com.hyperdeck.shizuku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CommandExecutor {

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

    suspend fun execute(command: String): CommandResult = withContext(Dispatchers.IO) {
        val service = ShizukuManager.getService()
        if (service == null) {
            return@withContext CommandResult(-1, "", "Shizuku service not connected")
        }
        try {
            val raw = service.execute(command)
            if (raw.startsWith("exit=")) {
                val firstNewline = raw.indexOf('\n')
                val codeStr = raw.substring(5, if (firstNewline > 0) firstNewline else raw.length)
                val exitCode = codeStr.toIntOrNull() ?: -1
                val output = if (firstNewline > 0) raw.substring(firstNewline + 1) else ""
                CommandResult(exitCode, output, "")
            } else if (raw.startsWith("error: ")) {
                CommandResult(-1, "", raw.removePrefix("error: "))
            } else {
                CommandResult(0, raw, "")
            }
        } catch (e: Exception) {
            CommandResult(-1, "", e.message ?: "Unknown error")
        }
    }
}
