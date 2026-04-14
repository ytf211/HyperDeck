package com.hyperdeck.ui.tools.accessibility

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperdeck.R
import com.hyperdeck.data.model.AccessibilityServiceInfo
import com.hyperdeck.shizuku.CommandExecutor
import com.hyperdeck.ui.theme.AccessibilityRunning
import com.hyperdeck.ui.theme.AccessibilityStopped
import kotlinx.coroutines.launch

@Composable
fun AccessibilityScreen() {
    val scope = rememberCoroutineScope()
    val services = remember { mutableStateListOf<AccessibilityServiceInfo>() }
    var searchQuery by remember { mutableStateOf("") }
    var showRunningOnly by remember { mutableStateOf(false) }

    val filteredServices by remember {
        derivedStateOf {
            services.filter { svc ->
                val matchesSearch = searchQuery.isBlank() ||
                    svc.label.contains(searchQuery, ignoreCase = true) ||
                    svc.packageName.contains(searchQuery, ignoreCase = true)
                val matchesFilter = !showRunningOnly || svc.isEnabled
                matchesSearch && matchesFilter
            }
        }
    }

    val runningCount by remember { derivedStateOf { services.count { it.isEnabled } } }
    val totalCount by remember { derivedStateOf { services.size } }

    fun loadServices() {
        scope.launch {
            val enabledResult = CommandExecutor.execute("settings get secure enabled_accessibility_services")
            val enabledList = enabledResult.output
                .split(":")
                .filter { it.contains("/") }
                .map { it.trim() }
                .toSet()

            val dumpResult = CommandExecutor.execute(
                "pm query-services --components -a android.accessibilityservice.AccessibilityService"
            )
            val allComponents = dumpResult.output.lines()
                .map { it.trim() }
                .filter { it.contains("/") }

            services.clear()
            allComponents.forEach { component ->
                val parts = component.split("/")
                if (parts.size == 2) {
                    val pkg = parts[0]
                    val svcClass = parts[1]
                    val fullComponent = "$pkg/$svcClass"
                    val labelResult = CommandExecutor.execute(
                        "pm resolve-activity --brief $pkg 2>/dev/null | tail -1"
                    )
                    val label = labelResult.output.takeIf { it.isNotBlank() && !it.contains("/") }
                        ?: pkg.substringAfterLast(".")
                    services.add(
                        AccessibilityServiceInfo(
                            label = label,
                            packageName = pkg,
                            serviceName = svcClass,
                            isEnabled = enabledList.contains(fullComponent),
                            isRunning = enabledList.contains(fullComponent)
                        )
                    )
                }
            }
        }
    }

    fun toggleService(service: AccessibilityServiceInfo, enable: Boolean) {
        scope.launch {
            CommandExecutor.execute("settings put secure accessibility_enabled 1")

            val currentResult = CommandExecutor.execute("settings get secure enabled_accessibility_services")
            val currentList = currentResult.output
                .split(":")
                .filter { it.contains("/") }
                .map { it.trim() }
                .toMutableList()

            if (enable) {
                if (!currentList.contains(service.componentName)) {
                    currentList.add(service.componentName)
                }
            } else {
                currentList.remove(service.componentName)
            }

            val newValue = currentList.joinToString(":")
            CommandExecutor.execute("settings put secure enabled_accessibility_services \"$newValue\"")
            loadServices()
        }
    }

    LaunchedEffect(Unit) { loadServices() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(stringResource(R.string.running), runningCount.toString(), AccessibilityRunning, Modifier.weight(1f))
            StatCard(stringResource(R.string.installed), totalCount.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // Search & filter
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(24.dp),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showRunningOnly,
                onClick = { showRunningOnly = false },
                label = { Text(stringResource(R.string.all)) }
            )
            FilterChip(
                selected = showRunningOnly,
                onClick = { showRunningOnly = true },
                label = { Text(stringResource(R.string.running)) }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Service list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredServices, key = { it.componentName }) { service ->
                ServiceCard(
                    service = service,
                    onToggle = { enabled -> toggleService(service, enabled) }
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ServiceCard(
    service: AccessibilityServiceInfo,
    onToggle: (Boolean) -> Unit
) {
    val borderColor = if (service.isEnabled) AccessibilityRunning else AccessibilityStopped
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(service.label, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    service.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = service.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
