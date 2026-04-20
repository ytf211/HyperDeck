package com.hyperdeck.ui.shell

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyperdeck.R
import com.hyperdeck.data.model.QuickCommand

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShellScreen(viewModel: ShellViewModel = viewModel()) {
    val context = LocalContext.current

    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val quickCommands by viewModel.quickCommands.collectAsStateWithLifecycle()
    val autoScroll by viewModel.autoScroll.collectAsStateWithLifecycle()

    // Terminal: warm dark background, soft contrast text
    val terminalBackground = Color(0xFF1E2028)
    val terminalTextColor = Color(0xFFBFC7D5)
    val terminalCmdColor = Color(0xFF82AAFF)

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var showCommandSheet by remember { mutableStateOf(false) }
    var showQuickCommandDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var quickCommandAction by remember { mutableStateOf<QuickCommand?>(null) }
    var editingCommand by remember { mutableStateOf<QuickCommand?>(null) }
    var deletingCommand by remember { mutableStateOf<QuickCommand?>(null) }

    LaunchedEffect(entries.size, autoScroll) {
        if (autoScroll && entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Action bar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showCommandSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.quick_commands))
            }
            IconToggleButton(
                checked = autoScroll,
                onCheckedChange = { viewModel.setAutoScroll(it) }
            ) {
                Icon(
                    Icons.Default.VerticalAlignBottom,
                    contentDescription = stringResource(R.string.log_auto_scroll),
                    tint = if (autoScroll) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {
                val text = viewModel.getAllText()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("shell", text))
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.log_copy_all))
            }
            IconButton(onClick = { showClearConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.log_clear))
            }
        }

        // Terminal area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(terminalBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isRunning) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    if (entries.isEmpty()) {
                        item {
                            Text(
                                "$ _",
                                color = terminalTextColor.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        }
                    }
                    items(entries, key = { it.id }) { entry ->
                        SelectionContainer {
                            Column {
                                Text(
                                    "$ ${entry.command}",
                                    color = terminalCmdColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                                when {
                                    entry.isRunning -> {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            stringResource(R.string.shell_running),
                                            color = terminalTextColor.copy(alpha = 0.72f),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp
                                        )
                                    }

                                    entry.output.isNotBlank() -> {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            entry.output,
                                            color = if (entry.isError) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                terminalTextColor
                                            },
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(
                            color = terminalTextColor.copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.navigateHistoryUp()?.let { inputText = it }
            }) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null)
            }
            IconButton(onClick = {
                viewModel.navigateHistoryDown()?.let { inputText = it }
            }) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null)
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.shell_input_hint)) },
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            if (isRunning) {
                IconButton(onClick = { viewModel.cancelCommand() }) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = stringResource(R.string.stop),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                IconButton(onClick = {
                    viewModel.executeCommand(inputText)
                    inputText = ""
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send)
                    )
                }
            }
        }
    }

    // Clear history confirmation dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.confirm_clear)) },
            text = { Text(stringResource(R.string.confirm_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearConfirm = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete quick command confirmation dialog
    deletingCommand?.let { cmd ->
        AlertDialog(
            onDismissRequest = { deletingCommand = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_quick_command)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeQuickCommand(cmd.id)
                    deletingCommand = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCommand = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
                    Text(
                        stringResource(R.string.quick_commands),
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = {
                        editingCommand = null
                        showQuickCommandDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                    }
                }
                Spacer(Modifier.height(8.dp))
                quickCommands.forEach { qc ->
                    FilterChip(
                        selected = false,
                        onClick = {},
                        label = { Text(qc.label) },
                        modifier = Modifier
                            .padding(end = 8.dp, bottom = 4.dp)
                            .combinedClickable(
                                onClick = {
                                    viewModel.executeCommand(qc.command)
                                    showCommandSheet = false
                                },
                                onLongClick = {
                                    quickCommandAction = qc
                                }
                            )
                    )
                }
            }
        }
    }

    quickCommandAction?.let { quickCommand ->
        AlertDialog(
            onDismissRequest = { quickCommandAction = null },
            title = { Text(quickCommand.label) },
            text = {
                Text(quickCommand.command)
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        editingCommand = quickCommand
                        quickCommandAction = null
                        showQuickCommandDialog = true
                    }) {
                        Text(stringResource(R.string.edit))
                    }
                    TextButton(onClick = {
                        deletingCommand = quickCommand
                        quickCommandAction = null
                    }) {
                        Text(
                            stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = { quickCommandAction = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
            dismissButton = null
        )
    }

    // Add/edit quick command dialog
    if (showQuickCommandDialog) {
        QuickCommandDialog(
            title = if (editingCommand == null) {
                stringResource(R.string.quick_commands)
            } else {
                stringResource(R.string.edit_quick_command)
            },
            confirmLabel = if (editingCommand == null) {
                stringResource(R.string.add)
            } else {
                stringResource(R.string.save)
            },
            initialLabel = editingCommand?.label.orEmpty(),
            initialCommand = editingCommand?.command.orEmpty(),
            onDismiss = {
                editingCommand = null
                showQuickCommandDialog = false
            },
            onConfirm = { label, command ->
                if (editingCommand == null) {
                    viewModel.addQuickCommand(label, command)
                } else {
                    viewModel.updateQuickCommand(
                        id = editingCommand!!.id,
                        label = label,
                        command = command
                    )
                }
                editingCommand = null
                showQuickCommandDialog = false
            }
        )
    }
}

@Composable
private fun QuickCommandDialog(
    title: String,
    confirmLabel: String,
    initialLabel: String,
    initialCommand: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var label by remember(initialLabel) { mutableStateOf(initialLabel) }
    var cmd by remember(initialCommand) { mutableStateOf(initialCommand) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.title_label)) }
                )
                OutlinedTextField(
                    value = cmd,
                    onValueChange = { cmd = it },
                    label = { Text(stringResource(R.string.command_label)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (label.isNotBlank() && cmd.isNotBlank()) {
                    onConfirm(label, cmd)
                }
            }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
