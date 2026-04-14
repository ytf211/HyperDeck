package com.hyperdeck.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.hyperdeck.R
import com.hyperdeck.shizuku.CommandExecutor
import com.hyperdeck.ui.theme.TerminalBackground
import com.hyperdeck.ui.theme.TerminalText
import kotlinx.coroutines.launch

data class ShellEntry(
    val command: String,
    val output: String,
    val isError: Boolean = false
)

@Composable
fun ShellScreen() {
    var inputText by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<ShellEntry>() }
    val scope = rememberCoroutineScope()
    val outputScrollState = rememberScrollState()

    val quickCommands = listOf(
        stringResource(R.string.clipboard) to "dumpsys clipboard",
        stringResource(R.string.memory_info) to "cat /proc/meminfo | head -5",
        stringResource(R.string.battery_info) to "dumpsys battery",
        stringResource(R.string.screen_resolution) to "wm size",
        "DPI" to "wm density",
        stringResource(R.string.system_version) to "getprop ro.build.display.id"
    )

    fun executeCommand(cmd: String) {
        if (cmd.isBlank()) return
        scope.launch {
            val result = CommandExecutor.execute(cmd)
            history.add(
                ShellEntry(
                    command = cmd,
                    output = result.fullOutput.ifBlank { "(no output)" },
                    isError = !result.isSuccess
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Quick commands
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickCommands.forEach { (label, cmd) ->
                AssistChip(
                    onClick = { executeCommand(cmd) },
                    label = { Text(label) }
                )
            }
        }

        // Terminal output
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalBackground)
                .verticalScroll(outputScrollState)
                .padding(12.dp)
        ) {
            if (history.isEmpty()) {
                Text(
                    "$ _",
                    color = TerminalText.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
            history.forEach { entry ->
                Text(
                    "$ ${entry.command}",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    entry.output,
                    color = if (entry.isError) MaterialTheme.colorScheme.error else TerminalText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.shell_input_hint)) },
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    executeCommand(inputText)
                    inputText = ""
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
