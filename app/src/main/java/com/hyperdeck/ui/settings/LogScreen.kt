package com.hyperdeck.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperdeck.R
import com.hyperdeck.data.model.LogLevel
import com.hyperdeck.data.repository.LogRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen() {
    val context = LocalContext.current
    val entries by LogRepository.entries.collectAsState()
    val minLevel by LogRepository.minLevel.collectAsState()
    val enabled by LogRepository.enabled.collectAsState()
    var autoScroll by remember { mutableStateOf(true) }
    var showLevelMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    val filteredEntries = entries.filter { it.level.priority >= minLevel.priority }

    LaunchedEffect(filteredEntries.size, autoScroll) {
        if (autoScroll && filteredEntries.isNotEmpty()) {
            listState.animateScrollToItem(filteredEntries.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        if (!enabled) LogRepository.setEnabled(true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Action bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level filter
            IconButton(onClick = { showLevelMenu = true }) {
                Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.log_level))
            }
            DropdownMenu(expanded = showLevelMenu, onDismissRequest = { showLevelMenu = false }) {
                LogLevel.entries.forEach { level ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                level.name,
                                color = if (level == minLevel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            LogRepository.setMinLevel(level)
                            showLevelMenu = false
                        }
                    )
                }
            }

            IconToggleButton(checked = autoScroll, onCheckedChange = { autoScroll = it }) {
                Icon(
                    Icons.Default.VerticalAlignBottom,
                    contentDescription = stringResource(R.string.log_auto_scroll),
                    tint = if (autoScroll) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("log", LogRepository.export()))
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.log_copy_all))
            }

            IconButton(onClick = {
                val text = LogRepository.export()
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, null))
            }) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.log_share))
            }

            IconButton(onClick = { LogRepository.clear() }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.log_clear))
            }
        }

        // Log content
        if (filteredEntries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.log_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(8.dp)),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(filteredEntries.size) { index ->
                    val entry = filteredEntries[index]
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(
                            dateFormat.format(Date(entry.timestamp)),
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            " ${entry.level.label}/",
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            color = levelColor(entry.level)
                        )
                        Text(
                            "${entry.tag}: ",
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            entry.message,
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun levelColor(level: LogLevel): Color {
    return when (level) {
        LogLevel.VERBOSE -> MaterialTheme.colorScheme.onSurfaceVariant
        LogLevel.DEBUG -> Color(0xFF4FC3F7)
        LogLevel.INFO -> Color(0xFF66BB6A)
        LogLevel.WARN -> Color(0xFFFFA726)
        LogLevel.ERROR -> Color(0xFFEF5350)
    }
}
