package com.hyperdeck.ui.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperdeck.R
import com.hyperdeck.data.config.SettingsConfigParser

data class ToolItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

private val categoryColors = listOf(
    Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0),
    Color(0xFF009688), Color(0xFFE91E63), Color(0xFF795548)
)

@Composable
fun ToolsScreen(
    onNavigateToAccessibility: () -> Unit,
    onNavigateToCategory: (String) -> Unit
) {
    val context = LocalContext.current
    val categories = SettingsConfigParser.loadFromInternal(context)

    val tools = mutableListOf(
        ToolItem(
            id = "accessibility",
            title = stringResource(R.string.accessibility_management),
            description = stringResource(R.string.accessibility_desc),
            icon = Icons.Default.Accessibility,
            accentColor = Color(0xFF4CAF50),
            onClick = onNavigateToAccessibility
        )
    )

    categories.forEachIndexed { index, cat ->
        tools.add(
            ToolItem(
                id = "cat_${cat.category}",
                title = cat.category,
                description = "${cat.items.size} items",
                icon = Icons.Default.Settings,
                accentColor = categoryColors[index % categoryColors.size],
                onClick = { onNavigateToCategory(cat.category) }
            )
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tools, key = { it.id }) { tool ->
            ToolCard(tool)
        }
    }
}

@Composable
private fun ToolCard(tool: ToolItem) {
    Card(
        onClick = tool.onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(2.dp, tool.accentColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    tool.icon,
                    contentDescription = null,
                    tint = tool.accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(tool.title, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
