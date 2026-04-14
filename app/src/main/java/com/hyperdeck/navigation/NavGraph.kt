package com.hyperdeck.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import com.hyperdeck.R
import com.hyperdeck.shizuku.ShizukuManager
import com.hyperdeck.ui.components.HyperDeckTopBar
import com.hyperdeck.ui.settings.AppSettingsScreen
import com.hyperdeck.ui.shell.ShellScreen
import com.hyperdeck.ui.tools.ToolsScreen
import com.hyperdeck.ui.tools.accessibility.AccessibilityScreen
import com.hyperdeck.ui.tools.settings.SystemSettingsScreen
import kotlinx.serialization.Serializable

@Serializable object ToolsRoute
@Serializable object ShellRoute
@Serializable object SettingsRoute
@Serializable object AccessibilityRoute
@Serializable object SystemSettingsRoute

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

@Composable
fun HyperDeckNavGraph(shizukuManager: ShizukuManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val bottomNavItems = listOf(
        BottomNavItem(stringResource(R.string.tab_tools), Icons.Default.Build, ToolsRoute),
        BottomNavItem(stringResource(R.string.tab_shell), Icons.Default.Terminal, ShellRoute),
        BottomNavItem(stringResource(R.string.tab_settings), Icons.Default.Settings, SettingsRoute),
    )

    val showBottomBar = navBackStackEntry?.destination?.let { dest ->
        bottomNavItems.any { dest.hasRoute(it.route::class) }
    } ?: true

    val currentTitle = when {
        navBackStackEntry?.destination?.hasRoute<AccessibilityRoute>() == true -> stringResource(R.string.accessibility_management)
        navBackStackEntry?.destination?.hasRoute<SystemSettingsRoute>() == true -> stringResource(R.string.system_settings)
        else -> stringResource(R.string.app_name)
    }

    val showBackButton = navBackStackEntry?.destination?.let { dest ->
        !bottomNavItems.any { dest.hasRoute(it.route::class) }
    } ?: false

    Scaffold(
        topBar = {
            HyperDeckTopBar(
                title = currentTitle,
                showBack = showBackButton,
                onBack = { navController.popBackStack() },
                shizukuManager = shizukuManager
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ToolsRoute,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } }
        ) {
            composable<ToolsRoute> {
                ToolsScreen(
                    onNavigateToAccessibility = { navController.navigate(AccessibilityRoute) },
                    onNavigateToSystemSettings = { navController.navigate(SystemSettingsRoute) }
                )
            }
            composable<ShellRoute> {
                ShellScreen()
            }
            composable<SettingsRoute> {
                AppSettingsScreen(shizukuManager = shizukuManager)
            }
            composable<AccessibilityRoute> {
                AccessibilityScreen()
            }
            composable<SystemSettingsRoute> {
                SystemSettingsScreen()
            }
        }
    }
}
