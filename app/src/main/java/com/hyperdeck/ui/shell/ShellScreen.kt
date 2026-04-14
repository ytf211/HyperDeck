package com.hyperdeck.ui.shell

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

data class QuickCmd(val label: String, val command: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen() {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<ShellEntry>() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var autoScroll by remember { mutableStateOf(true) }
    var showCommandSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val quickCommands = remember {
        mutableStateListOf(
            QuickCmd("getprop", "getprop ro.build.display.id"),
            QuickCmd("battery", "dumpsys battery"),
            QuickCmd("wm size", "wm size"),
            QuickCmd("wm density", "wm density"),
            QuickCmd("meminfo", "cat /proc/meminfo | head -5")
        )
    }

    LaunchedEffect(history.size, autoScroll) {
        if (autoScroll && history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

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

    fun copyAll() {
        val text = history.joinToString("\n\n") { "$ ${it.command}\n${it.output}" }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("shell", text))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top action bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showCommandSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.quick_commands))
            }
            IconToggleButton(checked = autoScroll, onCheckedChange = { autoScroll = it }) {
                Icon(
                    Icons.Default.VerticalAlignBottom,
                    contentDescription = stringResource(R.string.log_auto_scroll),
                    tint = if (autoScroll) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { copyAll() }) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.log_copy_all))
            }
            IconButton(onClick = { history.clear() }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.log_clear))
            }
        }

        // Terminal output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(TerminalBackground, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (history.isEmpty()) {
                item {
                    Text(
                        "$ _",
                        color = TerminalText.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            }
            items(history) { entry ->
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
            IconButton(onClick = {
                executeCommand(inputText)
                inputText = ""
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
            }
        }
    }

    // Quick commands bottom sheet
    if (showCommandSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommandSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.quick_commands), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                    }
                }
                Spacer(Modifier.height(8.dp))
                quickCommands.forEach { qc ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            executeCommand(qc.command)
                            showCommandSheet = false
                        },
                        label = { Text(qc.label) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }

    // Add command dialog
    if (showAddDialog) {
        var label by remember { mutableStateOf("") }
        var cmd by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_setting)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(stringResource(R.string.title_label)) })
                    OutlinedTextField(value = cmd, onValueChange = { cmd = it }, label = { Text(stringResource(R.string.command_label)) })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (label.isNotBlank() && cmd.isNotBlank()) {
                        quickCommands.add(QuickCmd(label, cmd))
                        showAddDialog = false
                    }
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
