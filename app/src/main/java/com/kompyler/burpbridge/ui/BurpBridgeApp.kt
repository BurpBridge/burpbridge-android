package com.kompyler.burpbridge.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kompyler.burpbridge.ui.screens.AppsScreen
import com.kompyler.burpbridge.ui.screens.DashboardScreen
import com.kompyler.burpbridge.ui.screens.SettingsScreen
import com.kompyler.burpbridge.ui.screens.SplashScreen
import com.kompyler.burpbridge.ui.theme.ThemeMode

enum class Screen {
    SPLASH,
    DASHBOARD,
    APPS,
    SETTINGS
}

sealed class BottomNavItem(
    val route: Screen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : BottomNavItem(
        Screen.DASHBOARD,
        "Dashboard",
        Icons.Filled.Dashboard,
        Icons.Outlined.Dashboard
    )

    data object Apps : BottomNavItem(
        Screen.APPS,
        "Apps",
        Icons.Filled.Apps,
        Icons.Outlined.Apps
    )

    data object Settings : BottomNavItem(
        Screen.SETTINGS,
        "Settings",
        Icons.Filled.Settings,
        Icons.Outlined.Settings
    )
}

@Composable
fun BurpBridgeApp() {
    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
    var isVpnConnected by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(ThemeMode.DARK) }

    when (currentScreen) {
        Screen.SPLASH -> {
            SplashScreen(
                onNavigateToDashboard = {
                    currentScreen = Screen.DASHBOARD
                }
            )
        }

        else -> {
            MainScaffold(
                currentScreen = currentScreen,
                onScreenChange = { currentScreen = it },
                themeMode = themeMode,
                onThemeModeChange = { themeMode = it },
                isVpnConnected = isVpnConnected,
                onVpnStatusChange = { isVpnConnected = it }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    isVpnConnected: Boolean,
    onVpnStatusChange: (Boolean) -> Unit
) {
    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Apps,
        BottomNavItem.Settings
    )

    val showBottomBar = currentScreen != Screen.SPLASH

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentScreen == item.route

                        NavigationBarItem(
                            selected = selected,
                            onClick = { onScreenChange(item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> {
                    DashboardScreen(
                        isVpnConnected = isVpnConnected,
                        onVpnStatusChange = onVpnStatusChange
                    )
                }

                Screen.APPS -> {
                    AppsScreen(
                        onToggleAppSelection = { /* TODO: Handle app selection */ },
                        onSelectAll = { /* TODO: Handle select all */ },
                        onDeselectAll = { /* TODO: Handle deselect all */ }
                    )
                }

                Screen.SETTINGS -> {
                    SettingsScreen(
                        currentThemeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        onClearLogs = { /* TODO: Handle clear logs */ },
                        onInstallCertificate = { /* TODO: Handle certificate install */ }
                    )
                }

                else -> { }
            }
        }
    }
}