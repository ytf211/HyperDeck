package com.hyperdeck.shizuku

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class ShellService : IShellService.Stub {

    @Volatile
    private var currentProcess: Process? = null

    constructor() : super() {
        Log.i(TAG, "constructor")
    }

    @Keep
    constructor(context: Context) : super() {
        Log.i(TAG, "constructor with Context: $context")
    }

    override fun destroy() {
        Log.i(TAG, "destroy")
        currentProcess?.destroyForcibly()
        System.exit(0)
    }

    override fun execute(command: String): String {
        return try {
            // Parse optional timeout: "TIMEOUT=30 actual_command"
            val timeoutSeconds: Long
            val actualCommand: String
            if (command.startsWith("TIMEOUT=")) {
                val spaceIdx = command.indexOf(' ')
                if (spaceIdx > 0) {
                    timeoutSeconds = command.substring(8, spaceIdx).toLongOrNull() ?: DEFAULT_TIMEOUT
                    actualCommand = command.substring(spaceIdx + 1)
                } else {
                    timeoutSeconds = DEFAULT_TIMEOUT
                    actualCommand = command
                }
            } else {
                timeoutSeconds = DEFAULT_TIMEOUT
                actualCommand = command
            }

            val process = ProcessBuilder("sh", "-c", actualCommand)
                .redirectErrorStream(true)
                .start()
            currentProcess = process

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            currentProcess = null

            if (!finished) {
                process.destroyForcibly()
                "exit=-1\n${output.trim()}\n(timeout after ${timeoutSeconds}s)"
            } else {
                val exitCode = process.exitValue()
                if (exitCode == 0) output.trim()
                else "exit=$exitCode\n${output.trim()}"
            }
        } catch (e: Exception) {
            currentProcess?.destroyForcibly()
            currentProcess = null
            "error: ${e.message}"
        }
    }

    override fun cancel() {
        Log.i(TAG, "cancel requested")
        currentProcess?.destroyForcibly()
        currentProcess = null
    }

    companion object {
        private const val TAG = "ShellService"
        private const val DEFAULT_TIMEOUT = 900L // 15 minutes
    }
}
