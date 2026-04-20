package com.hyperdeck.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyperdeck.BuildConfig
import com.hyperdeck.R
import com.hyperdeck.shizuku.ShizukuStatus
import com.hyperdeck.ui.theme.ShizukuGreen
import com.hyperdeck.ui.theme.ShizukuRed

@Composable
fun AppSettingsScreen(
    shizukuManager: com.hyperdeck.shizuku.ShizukuManager,
    onNavigateToLog: () -> Unit = {},
    viewModel: AppSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val svcConnected by viewModel.serviceConnected.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    var restoreSummary by remember { mutableStateOf<String?>(null) }

    val uid = if (shizukuStatus == ShizukuStatus.CONNECTED) shizukuManager.getUid() else -1
    val apiVersion = if (shizukuStatus == ShizukuStatus.CONNECTED) shizukuManager.getApiVersion() else -1

    val statusText = when {
        shizukuStatus == ShizukuStatus.NOT_INSTALLED -> stringResource(R.string.shizuku_not_installed)
        shizukuStatus == ShizukuStatus.DISCONNECTED -> stringResource(R.string.shizuku_disconnected)
        svcConnected -> stringResource(R.string.service_connected)
        else -> stringResource(R.string.shizuku_connected)
    }
    val statusColor = if (shizukuStatus == ShizukuStatus.CONNECTED) ShizukuGreen else ShizukuRed

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
            json?.let { text -> viewModel.importConfig(text) { /* done */ } }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shizuku section
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

        // Shizuku not installed guidance
        if (shizukuStatus == ShizukuStatus.NOT_INSTALLED) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.shizuku_not_installed),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.shizuku_install_guide),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                            try { context.startActivity(intent) } catch (_: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")))
                            }
                        }) { Text("Google Play") }
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/RikkaApps/Shizuku/releases")))
                        }) { Text("GitHub") }
                    }
                }
            }
        }

        // Theme section
        SectionTitle(stringResource(R.string.app_settings))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.dark_mode), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                val options = listOf(
                    stringResource(R.string.theme_system),
                    stringResource(R.string.theme_light),
                    stringResource(R.string.theme_dark)
                )
                val selectedIndex = when (darkMode) {
                    null -> 0
                    false -> 1
                    true -> 2
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            onClick = {
                                viewModel.setDarkMode(
                                    when (index) {
                                        0 -> null
                                        1 -> false
                                        2 -> true
                                        else -> null
                                    }
                                )
                            },
                            selected = index == selectedIndex
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }

        // Log viewer
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

        // Config import/export
        SectionTitle(stringResource(R.string.config_management))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.exportConfig { json ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.export_config)) }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.import_config)) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.restoreMissingDefaults { success ->
                            restoreSummary = if (success) {
                                context.getString(R.string.restore_missing_defaults_done)
                            } else {
                                context.getString(R.string.restore_missing_defaults_failed)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.restore_missing_defaults))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.restore_missing_defaults_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                restoreSummary?.let { summary ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // About
        SectionTitle(stringResource(R.string.about))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(stringResource(R.string.version), BuildConfig.VERSION_NAME)
                Spacer(Modifier.height(8.dp))
                InfoRow(stringResource(R.string.package_name), BuildConfig.APPLICATION_ID)
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
