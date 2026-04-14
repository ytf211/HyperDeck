package com.hyperdeck.ui.tools.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.hyperdeck.R
import com.hyperdeck.data.config.SettingsConfigParser
import com.hyperdeck.data.model.SettingsCategory
import com.hyperdeck.data.model.SettingsItem
import com.hyperdeck.shizuku.CommandExecutor
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SystemSettingsScreen(categoryFilter: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categories = remember { mutableStateListOf<SettingsCategory>() }
    var editingItem by remember { mutableStateOf<SettingsItem?>(null) }
    var deletingItem by remember { mutableStateOf<SettingsItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded = SettingsConfigParser.loadFromInternal(context)
        categories.clear()
        if (categoryFilter != null) {
            loaded.filter { it.category == categoryFilter }.let { categories.addAll(it) }
        } else {
            categories.addAll(loaded)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_setting))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        category.category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(category.items, key = { "${category.category}:${it.title}" }) { item ->
                    SettingsItemCard(
                        item = item,
                        onLongClick = { editingItem = item },
                        onDelete = { deletingItem = item }
                    )
                }
            }
        }
    }

    if (editingItem != null) {
        EditSettingsDialog(
            item = editingItem!!,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                val catIdx = categories.indexOfFirst { cat ->
                    cat.items.any { it.title == editingItem!!.title }
                }
                if (catIdx >= 0) {
                    val cat = categories[catIdx]
                    val itemIdx = cat.items.indexOfFirst { it.title == editingItem!!.title }
                    if (itemIdx >= 0) {
                        val newItems = cat.items.toMutableList()
                        newItems[itemIdx] = updated
                        categories[catIdx] = cat.copy(items = newItems)
                        SettingsConfigParser.saveToInternal(context, categories)
                    }
                }
                editingItem = null
            }
        )
    }

    if (deletingItem != null) {
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text("${deletingItem!!.title}?") },
            confirmButton = {
                TextButton(onClick = {
                    val item = deletingItem!!
                    val catIdx = categories.indexOfFirst { cat -> cat.items.contains(item) }
                    if (catIdx >= 0) {
                        val cat = categories[catIdx]
                        val newItems = cat.items.filter { it != item }
                        if (newItems.isEmpty()) {
                            categories.removeAt(catIdx)
                        } else {
                            categories[catIdx] = cat.copy(items = newItems)
                        }
                        SettingsConfigParser.saveToInternal(context, categories)
                    }
                    deletingItem = null
                }) { Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showAddDialog) {
        AddSettingsDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newItem ->
                if (categories.isNotEmpty()) {
                    val cat = categories[0]
                    categories[0] = cat.copy(items = cat.items + newItem)
                } else {
                    categories.add(SettingsCategory("Custom", listOf(newItem)))
                }
                SettingsConfigParser.saveToInternal(context, categories)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsItemCard(
    item: SettingsItem,
    onLongClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (showMenu) {
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_setting)) },
                        onClick = { showMenu = false; onLongClick() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            if (item.description.isNotBlank()) {
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))

            when (item.type) {
                "toggle" -> ToggleControl(item, scope)
                "slider" -> SliderControl(item, scope)
                "select" -> SelectControl(item, scope)
                "input" -> InputControl(item, scope)
            }
        }
    }
}

@Composable
private fun ToggleControl(item: SettingsItem, scope: kotlinx.coroutines.CoroutineScope) {
    var checked by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(item.check_command) {
        if (item.check_command.isNotBlank()) {
            val result = CommandExecutor.execute(item.check_command)
            checked = result.output != "null" && result.output.isNotBlank()
            loaded = true
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Switch(
            checked = checked,
            enabled = loaded,
            onCheckedChange = { newValue ->
                checked = newValue
                scope.launch {
                    val cmd = if (newValue) item.command_on else item.command_off
                    CommandExecutor.execute(cmd)
                    // Re-read actual state
                    if (item.check_command.isNotBlank()) {
                        val result = CommandExecutor.execute(item.check_command)
                        checked = result.output != "null" && result.output.isNotBlank()
                    }
                }
            }
        )
    }
}

@Composable
private fun SliderControl(item: SettingsItem, scope: kotlinx.coroutines.CoroutineScope) {
    var value by remember { mutableFloatStateOf(item.min) }
    var showInput by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(item.check_command) {
        if (item.check_command.isNotBlank()) {
            val result = CommandExecutor.execute(item.check_command)
            result.output.toFloatOrNull()?.let { value = it }
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = {
                    scope.launch {
                        val cmd = item.command.replace("{value}", value.toString())
                        CommandExecutor.execute(cmd)
                        if (item.check_command.isNotBlank()) {
                            val result = CommandExecutor.execute(item.check_command)
                            result.output.toFloatOrNull()?.let { value = it }
                        }
                    }
                },
                valueRange = item.min..item.max,
                steps = ((item.max - item.min) / item.step).toInt() - 1,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                inputText = value.toString()
                showInput = true
            }) {
                Text(String.format("%.2f", value))
            }
        }
    }

    if (showInput) {
        AlertDialog(
            onDismissRequest = { showInput = false },
            title = { Text(stringResource(R.string.enter_value)) },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    inputText.toFloatOrNull()?.let { v ->
                        value = v.coerceIn(item.min, item.max)
                        scope.launch {
                            val cmd = item.command.replace("{value}", value.toString())
                            CommandExecutor.execute(cmd)
                        }
                    }
                    showInput = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showInput = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectControl(item: SettingsItem, scope: kotlinx.coroutines.CoroutineScope) {
    var expanded by remember { mutableStateOf(false) }
    var selectedLabel by remember { mutableStateOf("") }

    LaunchedEffect(item.check_command) {
        if (item.check_command.isNotBlank()) {
            val result = CommandExecutor.execute(item.check_command)
            val currentValue = result.output.trim()
            selectedLabel = item.options.find { it.value == currentValue }?.label ?: currentValue
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            item.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        selectedLabel = option.label
                        expanded = false
                        scope.launch {
                            val cmd = item.command.replace("{value}", option.value)
                            CommandExecutor.execute(cmd)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InputControl(item: SettingsItem, scope: kotlinx.coroutines.CoroutineScope) {
    var text by remember { mutableStateOf("") }

    LaunchedEffect(item.check_command) {
        if (item.check_command.isNotBlank()) {
            val result = CommandExecutor.execute(item.check_command)
            if (result.output != "null") text = result.output
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = {
            scope.launch {
                val cmd = item.command.replace("{value}", text)
                CommandExecutor.execute(cmd)
            }
        }) { Text(stringResource(R.string.apply)) }
    }
}

@Composable
private fun EditSettingsDialog(
    item: SettingsItem,
    onDismiss: () -> Unit,
    onSave: (SettingsItem) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var description by remember { mutableStateOf(item.description) }
    var commandOn by remember { mutableStateOf(item.command_on) }
    var commandOff by remember { mutableStateOf(item.command_off) }
    var command by remember { mutableStateOf(item.command) }
    var checkCommand by remember { mutableStateOf(item.check_command) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_setting)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.title_label)) })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(R.string.description_label)) })
                if (item.type == "toggle") {
                    OutlinedTextField(value = commandOn, onValueChange = { commandOn = it }, label = { Text(stringResource(R.string.command_on)) })
                    OutlinedTextField(value = commandOff, onValueChange = { commandOff = it }, label = { Text(stringResource(R.string.command_off)) })
                } else {
                    OutlinedTextField(value = command, onValueChange = { command = it }, label = { Text(stringResource(R.string.command_label)) })
                }
                OutlinedTextField(value = checkCommand, onValueChange = { checkCommand = it }, label = { Text(stringResource(R.string.check_command)) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(item.copy(title = title, description = description, command_on = commandOn, command_off = commandOff, command = command, check_command = checkCommand))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun AddSettingsDialog(
    onDismiss: () -> Unit,
    onAdd: (SettingsItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("toggle") }
    var commandOn by remember { mutableStateOf("") }
    var commandOff by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var checkCommand by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_setting)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.title_label)) })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(R.string.description_label)) })

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val typeLabels = mapOf(
                        "toggle" to stringResource(R.string.type_toggle),
                        "slider" to stringResource(R.string.type_slider),
                        "input" to stringResource(R.string.type_input)
                    )
                    typeLabels.forEach { (t, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(label) }
                        )
                    }
                }

                if (type == "toggle") {
                    OutlinedTextField(value = commandOn, onValueChange = { commandOn = it }, label = { Text(stringResource(R.string.command_on)) })
                    OutlinedTextField(value = commandOff, onValueChange = { commandOff = it }, label = { Text(stringResource(R.string.command_off)) })
                } else {
                    OutlinedTextField(value = command, onValueChange = { command = it }, label = { Text(stringResource(R.string.command_label)) })
                }
                OutlinedTextField(value = checkCommand, onValueChange = { checkCommand = it }, label = { Text(stringResource(R.string.check_command)) })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            SettingsItem(
                                title = title,
                                description = description,
                                type = type,
                                command = command,
                                command_on = commandOn,
                                command_off = commandOff,
                                check_command = checkCommand
                            )
                        )
                    }
                }
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
