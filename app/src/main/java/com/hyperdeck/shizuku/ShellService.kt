package com.hyperdeck.shizuku

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader

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
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            currentProcess = process
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val exitCode = process.waitFor()
            currentProcess = null
            if (exitCode == 0) output.trim()
            else "exit=$exitCode\n${output.trim()}"
        } catch (e: Exception) {
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
    }
}
