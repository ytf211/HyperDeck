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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperdeck.R
import com.hyperdeck.data.repository.PreferencesRepository
import com.hyperdeck.shizuku.ShizukuManager
import com.hyperdeck.shizuku.ShizukuStatus
import com.hyperdeck.ui.theme.ShizukuGreen
import com.hyperdeck.ui.theme.ShizukuRed
import kotlinx.coroutines.launch

@Composable
fun AppSettingsScreen(shizukuManager: ShizukuManager, onNavigateToLog: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefsRepo = remember(context) { PreferencesRepository(context) }

    val shizukuStatus by shizukuManager.status.collectAsState()
    val svcConnected by shizukuManager.serviceConnected.collectAsState()
    val darkMode by prefsRepo.darkMode.collectAsState(initial = null)

    val uid = if (shizukuStatus == ShizukuStatus.CONNECTED) shizukuManager.getUid() else -1
    val apiVersion = if (shizukuStatus == ShizukuStatus.CONNECTED) shizukuManager.getApiVersion() else -1

    val statusText = when {
        shizukuStatus == ShizukuStatus.NOT_INSTALLED -> stringResource(R.string.shizuku_not_installed)
        shizukuStatus == ShizukuStatus.DISCONNECTED -> stringResource(R.string.shizuku_disconnected)
        svcConnected -> stringResource(R.string.service_connected)
        else -> stringResource(R.string.shizuku_connected)
    }
    val statusColor = if (shizukuStatus == ShizukuStatus.CONNECTED) ShizukuGreen else ShizukuRed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(stringResource(R.string.shizuku_config))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(stringResource(R.string.connection_status), statusText, valueColor = statusColor)
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    stringResource(R.string.run_mode),
                    when (uid) {
                        0 -> "Root (UID 0)"
                        2000 -> "Shell (UID 2000)"
                        -1 -> "N/A"
                        else -> "UID $uid"
                    }
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(stringResource(R.string.api_version), if (apiVersion >= 0) "$apiVersion" else "N/A")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (shizukuStatus == ShizukuStatus.DISCONNECTED) {
                        Button(onClick = { shizukuManager.requestPermission() }) {
                            Text(stringResource(R.string.authorize_shizuku))
                        }
                    }
                    if (shizukuStatus == ShizukuStatus.CONNECTED && !svcConnected) {
                        Button(onClick = { shizukuManager.bindService() }) {
                            Text(stringResource(R.string.connect_service))
                        }
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.app_settings))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.dark_mode), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = darkMode == true,
                    onCheckedChange = { scope.launch { prefsRepo.setDarkMode(it) } }
                )
            }
        }

        Card(
            onClick = onNavigateToLog,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.log_mode), style = MaterialTheme.typography.bodyLarge)
                Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SectionTitle(stringResource(R.string.about))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(stringResource(R.string.version), "0.2.2")
                Spacer(Modifier.height(8.dp))
                InfoRow(stringResource(R.string.package_name), "com.hyperdeck")
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
