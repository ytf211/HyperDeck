package com.hyperdeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hyperdeck.shizuku.ShizukuManager
import com.hyperdeck.shizuku.ShizukuStatus
import com.hyperdeck.ui.theme.ShizukuGray
import com.hyperdeck.ui.theme.ShizukuGreen
import com.hyperdeck.ui.theme.ShizukuRed

@Composable
fun ShizukuStatusDot(
    shizukuManager: ShizukuManager,
    modifier: Modifier = Modifier
) {
    var status by remember { mutableStateOf(ShizukuStatus.DISCONNECTED) }

    LaunchedEffect(Unit) {
        shizukuManager.onBinderReceived = {
            status = if (shizukuManager.hasPermission()) ShizukuStatus.CONNECTED
            else ShizukuStatus.DISCONNECTED
        }
        shizukuManager.onBinderDead = {
            status = ShizukuStatus.DISCONNECTED
        }
        shizukuManager.onPermissionResult = { granted ->
            status = if (granted) ShizukuStatus.CONNECTED else ShizukuStatus.DISCONNECTED
        }

        status = when {
            !shizukuManager.isShizukuAvailable() -> ShizukuStatus.NOT_INSTALLED
            shizukuManager.hasPermission() -> ShizukuStatus.CONNECTED
            else -> ShizukuStatus.DISCONNECTED
        }
    }

    val color = when (status) {
        ShizukuStatus.CONNECTED -> ShizukuGreen
        ShizukuStatus.DISCONNECTED -> ShizukuRed
        ShizukuStatus.NOT_INSTALLED -> ShizukuGray
    }
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}
