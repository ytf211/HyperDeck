package com.hyperdeck.shizuku

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class ShellService : IShellService.Stub() {

    override fun destroy() {
        Log.i(TAG, "destroy")
        System.exit(0)
    }

    override fun execute(command: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode == 0) output.trim()
            else "exit=$exitCode\n${output.trim()}"
        } catch (e: Exception) {
            "error: ${e.message}"
        }
    }

    companion object {
        private const val TAG = "ShellService"
    }
}
