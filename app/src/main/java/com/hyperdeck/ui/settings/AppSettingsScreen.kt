package com.hyperdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hyperdeck.data.repository.PreferencesRepository
import com.hyperdeck.shizuku.ShizukuManager
import com.hyperdeck.ui.theme.ShizukuGreen
import com.hyperdeck.ui.theme.ShizukuRed
import kotlinx.coroutines.launch

@Composable
fun AppSettingsScreen(shizukuManager: ShizukuManager) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefsRepo = remember(context) { PreferencesRepository(context) }

    var shizukuAvailable by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var serviceConnected by remember { mutableStateOf(false) }
    var uid by remember { mutableIntStateOf(-1) }
    var apiVersion by remember { mutableIntStateOf(-1) }
    val darkMode by prefsRepo.darkMode.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        shizukuManager.onBinderReceived = {
            shizukuAvailable = true
            hasPermission = shizukuManager.hasPermission()
            if (hasPermission) {
                uid = shizukuManager.getUid()
                apiVersion = shizukuManager.getApiVersion()
            }
        }
        shizukuManager.onBinderDead = {
            shizukuAvailable = false
            serviceConnected = false
        }
        shizukuManager.onPermissionResult = { granted ->
            hasPermission = granted
            if (granted) {
                uid = shizukuManager.getUid()
                apiVersion = shizukuManager.getApiVersion()
                shizukuManager.bindService()
            }
        }
        shizukuManager.onServiceConnected = { serviceConnected = true }
        shizukuManager.onServiceDisconnected = { serviceConnected = false }

        shizukuAvailable = shizukuManager.isShizukuAvailable()
        if (shizukuAvailable) {
            hasPermission = shizukuManager.hasPermission()
            if (hasPermission) {
                uid = shizukuManager.getUid()
                apiVersion = shizukuManager.getApiVersion()
            }
        }
    }

    val statusText = when {
        !shizukuAvailable -> "未运行"
        !hasPermission -> "未授权"
        serviceConnected -> "已连接"
        else -> "已授权"
    }
    val statusColor = if (shizukuAvailable && hasPermission) ShizukuGreen else ShizukuRed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shizuku Section
        SectionTitle("Shizuku 配置")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("连接状态", statusText, valueColor = statusColor)
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    "运行模式",
                    when (uid) {
                        0 -> "Root (UID 0)"
                        2000 -> "Shell (UID 2000)"
                        -1 -> "N/A"
                        else -> "UID $uid"
                    }
                )
                Spacer(Modifier.height(8.dp))
                InfoRow("API 版本", if (apiVersion >= 0) "$apiVersion" else "N/A")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (shizukuAvailable && !hasPermission) {
                        Button(onClick = { shizukuManager.requestPermission() }) {
                            Text("授权 Shizuku")
                        }
                    }
                    if (shizukuAvailable && hasPermission && !serviceConnected) {
                        Button(onClick = { shizukuManager.bindService() }) {
                            Text("连接服务")
                        }
                    }
                }
            }
        }

        // App Settings Section
        SectionTitle("应用设置")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("深色模式", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = darkMode == true,
                    onCheckedChange = { enabled ->
                        scope.launch { prefsRepo.setDarkMode(enabled) }
                    }
                )
            }
        }

        // About Section
        SectionTitle("关于")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("版本", "0.1.0")
                Spacer(Modifier.height(8.dp))
                InfoRow("包名", "com.hyperdeck")
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
