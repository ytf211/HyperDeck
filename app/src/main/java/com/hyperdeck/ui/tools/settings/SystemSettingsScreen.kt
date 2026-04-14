package com.hyperdeck.ui.tools.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyperdeck.R
import com.hyperdeck.data.model.SettingsItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun SystemSettingsScreen(
    categoryFilter: String? = null,
    viewModel: SystemSettingsViewModel = viewModel()
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var editingItem by remember { mutableStateOf<SettingsItem?>(null) }
    var deletingItem by remember { mutableStateOf<SettingsItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(categoryFilter) {
        viewModel.loadForCategory(categoryFilter)
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Find which category the items belong to and reorder
        val currentItems = categories.flatMap { it.items }
        if (from.index < currentItems.size && to.index < currentItems.size) {
            categories.firstOrNull()?.let { cat ->
                val mutable = cat.items.toMutableList()
                // Adjust indices for category header items
                val fromIdx = from.index - 1 // account for category header
                val toIdx = to.index - 1
                if (fromIdx in mutable.indices && toIdx in mutable.indices) {
                    val moved = mutable.removeAt(fromIdx)
                    mutable.add(toIdx, moved)
                    viewModel.reorderItems(cat.category, mutable)
                }
            }
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
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                item(key = "header_${category.category}") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        category.category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(category.items, key = { "${category.category}:${it.title}" }) { item ->
                    ReorderableItem(reorderableLazyListState, key = "${category.category}:${item.title}") { isDragging ->
                        SettingsItemCard(
                            item = item,
                            viewModel = viewModel,
                            onEdit = { editingItem = item },
                            onDelete = { deletingItem = item },
                            dragModifier = Modifier.draggableHandle()
                        )
                    }
                }
                }
            }
        }
    }

    // Edit dialog
    if (editingItem != null) {
        EditSettingsDialog(
            item = editingItem!!,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                viewModel.updateItem(editingItem!!.title, updated)
                editingItem = null
            }
        )
    }

    // Delete confirmation dialog
    if (deletingItem != null) {
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text("${deletingItem!!.title}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(deletingItem!!)
                    deletingItem = null
                }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Add dialog
    if (showAddDialog) {
        AddSettingsDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newItem ->
                viewModel.addItem(newItem)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SettingsItemCard(
    item: SettingsItem,
    viewModel: SystemSettingsViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragModifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val typeIcon: ImageVector = when (item.type) {
        "toggle" -> Icons.Outlined.ToggleOn
        "slider" -> Icons.Outlined.Tune
        "select" -> Icons.Outlined.List
        "input" -> Icons.Outlined.Edit
        else -> Icons.Outlined.Edit
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: type icon + title + more button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                    if (item.description.isNotBlank()) {
                        Text(
                            item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragModifier.size(20.dp)
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_setting)) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Control based on type
            when (item.type) {
                "toggle" -> ToggleControl(item, viewModel)
                "slider" -> SliderControl(item, viewModel)
                "select" -> SelectControl(item, viewModel)
                "input" -> InputControl(item, viewModel)
            }
        }
    }
}

@Composable
private fun ToggleControl(item: SettingsItem, viewModel: SystemSettingsViewModel) {
    var checked by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(item.check_command) {
        if (item.check_command.isNotBlank()) {
            viewModel.executeCommand(item.check_command) { result ->
                checked = SystemSettingsViewModel.parseToggleState(result.output)
                loaded = true
            }
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
                val cmd = if (newValue) item.command_on else item.command_off
                viewModel.executeCommand(cmd) {
                    // Re-read actual state
                    if (item.check_command.isNotBlank()) {
                        viewModel.executeCommand(item.check_command) { result ->
                            checked = SystemSettingsViewModel.parseToggleState(result.output)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun SliderControl(item: SettingsItem, viewModel: SystemSettingsViewModel) {
    var value by remember { mutableFloatStateOf(item.min) }
    var showInput by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(item.check_command) {
        if (item.check_command.isNotBlank()) {
            viewModel.executeCommand(item.check_command) { result ->
                result.output.trim().toFloatOrNull()?.let { value = it }
            }
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
                    val cmd = item.command.replace("{value}", value.toString())
                    viewModel.executeCommand(cmd) {
                        if (item.check_command.isNotBlank()) {
                            viewModel.executeCommand(item.check_command) { result ->
                                result.output.trim().toFloatOrNull()?.let { value = it }
                            }
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
                        val cmd = item.command.replace("{value}", value.toString())
                        viewModel.executeCommand(cmd) {}
                    }
                    showInput = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInput = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectControl(item: SettingsItem, viewModel: SystemSettingsViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var selectedLabel by remember { mutableStateOf("") }

    LaunchedEffect(item.check_command) {
        if (item.check_command.isNotBlank()) {
            viewModel.executeCommand(item.check_command) { result ->
                val currentValue = result.output.trim()
                selectedLabel = item.options.find { it.value == currentValue }?.label ?: currentValue
            }
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
                        val cmd = item.command.replace("{value}", option.value)
                        viewModel.executeCommand(cmd) {}
                    }
                )
            }
        }
    }
}

@Composable
private fun InputControl(item: SettingsItem, viewModel: SystemSettingsViewModel) {
    var text by remember { mutableStateOf("") }

    LaunchedEffect(item.check_command) {
        if (item.check_command.isNotBlank()) {
            viewModel.executeCommand(item.check_command) { result ->
                val trimmed = result.output.trim()
                if (trimmed != "null") text = trimmed
            }
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
            val cmd = item.command.replace("{value}", text)
            viewModel.executeCommand(cmd) {}
        }) {
            Text(stringResource(R.string.apply))
        }
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
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (item.type == "toggle") {
                    OutlinedTextField(
                        value = commandOn,
                        onValueChange = { commandOn = it },
                        label = { Text(stringResource(R.string.command_on)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = commandOff,
                        onValueChange = { commandOff = it },
                        label = { Text(stringResource(R.string.command_off)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text(stringResource(R.string.command_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = checkCommand,
                    onValueChange = { checkCommand = it },
                    label = { Text(stringResource(R.string.check_command)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    item.copy(
                        title = title,
                        description = description,
                        command_on = commandOn,
                        command_off = commandOff,
                        command = command,
                        check_command = checkCommand
                    )
                )
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
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
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val typeLabels = mapOf(
                        "toggle" to stringResource(R.string.type_toggle),
                        "slider" to stringResource(R.string.type_slider),
                        "input" to stringResource(R.string.type_input)
                    )
                    typeLabels.forEach { (t, label) ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(label) }
                        )
                    }
                }

                if (type == "toggle") {
                    OutlinedTextField(
                        value = commandOn,
                        onValueChange = { commandOn = it },
                        label = { Text(stringResource(R.string.command_on)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = commandOff,
                        onValueChange = { commandOff = it },
                        label = { Text(stringResource(R.string.command_off)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text(stringResource(R.string.command_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = checkCommand,
                    onValueChange = { checkCommand = it },
                    label = { Text(stringResource(R.string.check_command)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
