package com.hyperdeck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperdeck.R
import com.hyperdeck.shizuku.ShizukuManager
import com.hyperdeck.shizuku.ShizukuStatus
import com.hyperdeck.ui.theme.ShizukuGray
import com.hyperdeck.ui.theme.ShizukuGreen
import com.hyperdeck.ui.theme.ShizukuRed

private data class StatusConfig(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
fun ShizukuStatusIndicator(
    shizukuManager: ShizukuManager,
    modifier: Modifier = Modifier
) {
    val status by shizukuManager.status.collectAsState()
    val svcConnected by shizukuManager.serviceConnected.collectAsState()

    val config = when {
        status == ShizukuStatus.CONNECTED && svcConnected -> StatusConfig(
            icon = Icons.Outlined.CheckCircle,
            label = stringResource(R.string.status_connected),
            containerColor = ShizukuGreen.copy(alpha = 0.15f),
            contentColor = ShizukuGreen
        )
        status == ShizukuStatus.CONNECTED -> StatusConfig(
            icon = Icons.Outlined.LinkOff,
            label = stringResource(R.string.status_not_bound),
            containerColor = Color(0xFFFFF3E0),
            contentColor = Color(0xFFFF9800)
        )
        status == ShizukuStatus.DISCONNECTED -> StatusConfig(
            icon = Icons.Outlined.ErrorOutline,
            label = stringResource(R.string.status_unauthorized),
            containerColor = ShizukuRed.copy(alpha = 0.15f),
            contentColor = ShizukuRed
        )
        else -> StatusConfig(
            icon = Icons.Outlined.HelpOutline,
            label = stringResource(R.string.status_not_installed),
            containerColor = ShizukuGray.copy(alpha = 0.15f),
            contentColor = ShizukuGray
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = config.containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                config.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = config.contentColor
            )
            Text(
                config.label,
                style = MaterialTheme.typography.labelSmall,
                color = config.contentColor
            )
        }
    }
}
