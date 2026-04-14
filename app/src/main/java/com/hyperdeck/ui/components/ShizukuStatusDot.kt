package com.hyperdeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val status by shizukuManager.status.collectAsState()
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
