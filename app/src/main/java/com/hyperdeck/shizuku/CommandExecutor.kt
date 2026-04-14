package com.hyperdeck.shizuku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

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
        if (!ShizukuManager.hasPermission()) {
            return@withContext CommandResult(-1, "", "Shizuku permission not granted")
        }
        try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(false)
                .start()
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            CommandResult(exitCode, stdout.trim(), stderr.trim())
        } catch (e: Exception) {
            CommandResult(-1, "", e.message ?: "Unknown error")
        }
    }
}
