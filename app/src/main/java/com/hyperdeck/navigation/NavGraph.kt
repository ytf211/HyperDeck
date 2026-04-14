package com.hyperdeck.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.hyperdeck.shizuku.ShizukuStatus
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hyperdeck.R
import com.hyperdeck.shizuku.ShizukuManager
import com.hyperdeck.ui.components.HyperDeckTopBar
import com.hyperdeck.ui.settings.AppSettingsScreen
import com.hyperdeck.ui.settings.LogScreen
import com.hyperdeck.ui.shell.ShellScreen
import com.hyperdeck.ui.tools.ToolsScreen
import com.hyperdeck.ui.tools.accessibility.AccessibilityScreen
import com.hyperdeck.ui.tools.settings.SystemSettingsScreen
import kotlinx.serialization.Serializable

@Serializable object ToolsRoute
@Serializable object ShellRoute
@Serializable object SettingsRoute
@Serializable object AccessibilityRoute
@Serializable object LogRoute
@Serializable data class CategoryRoute(val category: String)

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

@Composable
fun HyperDeckNavGraph(shizukuManager: ShizukuManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val shizukuStatus by shizukuManager.status.collectAsState()

    // Check Shizuku on start
    LaunchedEffect(shizukuStatus) {
        delay(1500) // Wait for binder
        if (shizukuStatus != ShizukuStatus.CONNECTED) {
            val msg = if (shizukuStatus == ShizukuStatus.NOT_INSTALLED)
                "Shizuku not running" else "Shizuku not authorized"
            snackbarHostState.showSnackbar(msg)
        }
    }

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
        navBackStackEntry?.destination?.hasRoute<CategoryRoute>() == true -> {
            try { navBackStackEntry?.toRoute<CategoryRoute>()?.category ?: "" } catch (_: Exception) { "" }
        }
        navBackStackEntry?.destination?.hasRoute<LogRoute>() == true -> stringResource(R.string.log_mode)
        else -> stringResource(R.string.app_name)
    }

    val showBackButton = navBackStackEntry?.destination?.let { dest ->
        !bottomNavItems.any { dest.hasRoute(it.route::class) }
    } ?: false

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            enterTransition = {
                fadeIn(tween(350, easing = FastOutSlowInEasing)) +
                    scaleIn(tween(350, easing = FastOutSlowInEasing), initialScale = 0.92f) +
                    slideInHorizontally(tween(350, easing = FastOutSlowInEasing)) { it / 5 }
            },
            exitTransition = {
                fadeOut(tween(250)) +
                    scaleOut(tween(250), targetScale = 0.92f)
            },
            popEnterTransition = {
                fadeIn(tween(350, easing = FastOutSlowInEasing)) +
                    scaleIn(tween(350, easing = FastOutSlowInEasing), initialScale = 0.92f)
            },
            popExitTransition = {
                fadeOut(tween(250)) +
                    scaleOut(tween(250), targetScale = 0.92f) +
                    slideOutHorizontally(tween(250)) { it / 5 }
            }
        ) {
            composable<ToolsRoute> {
                ToolsScreen(
                    onNavigateToAccessibility = { navController.navigate(AccessibilityRoute) },
                    onNavigateToCategory = { cat -> navController.navigate(CategoryRoute(cat)) }
                )
            }
            composable<ShellRoute> {
                ShellScreen()
            }
            composable<SettingsRoute> {
                AppSettingsScreen(
                    shizukuManager = shizukuManager,
                    onNavigateToLog = { navController.navigate(LogRoute) }
                )
            }
            composable<LogRoute> {
                LogScreen()
            }
            composable<AccessibilityRoute> {
                AccessibilityScreen(shizukuManager = shizukuManager)
            }
            composable<CategoryRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CategoryRoute>()
                SystemSettingsScreen(categoryFilter = route.category)
            }
        }
    }
}
