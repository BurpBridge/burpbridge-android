package com.kompyler.burpbridge.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kompyler.burpbridge.BurpBridgeVpnService
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
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
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
    val context = LocalContext.current
    val activity = context as? Activity
    val application = activity?.application as? com.kompyler.burpbridge.BurpBridgeApp
    
    val viewModel: BurpBridgeViewModel = viewModel(
        factory = BurpBridgeViewModelFactory(context.applicationContext as com.kompyler.burpbridge.BurpBridgeApp)
    )

    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
    var isVpnConnected by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(ThemeMode.DARK) }

    val proxySettings by viewModel.proxySettings.collectAsState()
    val apps by viewModel.apps.collectAsState()

    var isStopping by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    BurpBridgeVpnService.ACTION_STATUS -> {
                        val status = intent.getStringExtra(BurpBridgeVpnService.EXTRA_VPN_STATUS)
                        
                        when (status) {
                            BurpBridgeVpnService.VPN_STATUS_STARTING -> {
                                isStopping = false
                            }
                            BurpBridgeVpnService.VPN_STATUS_STARTED -> {
                                isVpnConnected = true
                                isStopping = false
                                viewModel.setVpnConnected(true)
                                viewModel.addLog("> Proxy started at ${getCurrentTime()}")
                                if (proxySettings.alertOnIntercept) {
                                    Toast.makeText(context, "BurpBridge started - intercepting traffic", Toast.LENGTH_SHORT).show()
                                }
                            }
                            BurpBridgeVpnService.VPN_STATUS_STOPPING -> {
                                isStopping = true
                            }
                            BurpBridgeVpnService.VPN_STATUS_STOPPED -> {
                                isVpnConnected = false
                                isStopping = false
                                viewModel.setVpnConnected(false)
                                viewModel.addLog("> Proxy stopped at ${getCurrentTime()}")
                                if (proxySettings.alertOnIntercept) {
                                    Toast.makeText(context, "BurpBridge stopped", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(BurpBridgeVpnService.ACTION_STATUS)
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)

        // Check if VPN is actually running when app starts
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isActuallyVpnConnected = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        if (isActuallyVpnConnected) {
            Log.d("BurpBridgeApp", "VPN was already active on app start")
            isVpnConnected = true
            viewModel.setVpnConnected(true)
            viewModel.addLog("> Proxy resumed at ${getCurrentTime()}")
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService(context, viewModel.getTargetAddress(), proxySettings)
        } else {
            Toast.makeText(context, "VPN Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context)
    }

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
                onVpnStatusChange = { 
                    isVpnConnected = it
                    viewModel.setVpnConnected(it)
                },
                onStartProxy = {
                    val intent = VpnService.prepare(context)
                    if (intent != null) {
                        vpnPermissionLauncher.launch(intent)
                    } else {
                        startVpnService(context, viewModel.getTargetAddress(), proxySettings)
                    }
                },
                onStopProxy = {
                    stopVpnService(context)
                },
                onUpdateIp = { viewModel.updateTargetIp(it) },
                onUpdateHttpPort = { viewModel.updateHttpPort(it) },
                onUpdateHttpsPort = { viewModel.updateHttpsPort(it) },
                onToggleApp = { viewModel.toggleAppSelection(it) },
                onSelectAllApps = { viewModel.selectAllApps() },
                onDeselectAllApps = { viewModel.deselectAllApps() },
                onClearLogs = { viewModel.clearLogs() },
                onInstallCertificate = { /* TODO: Handle certificate install */ },
                onUpdateAutoStart = { viewModel.updateAutoStartVpn(it) },
                onUpdatePersistentNotification = { viewModel.updatePersistentNotification(it) },
                onUpdateAlertOnIntercept = { viewModel.updateAlertOnIntercept(it) },
                proxySettings = proxySettings,
                apps = apps,
                logs = viewModel.logs.collectAsState().value
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
    onVpnStatusChange: (Boolean) -> Unit,
    onStartProxy: () -> Unit,
    onStopProxy: () -> Unit,
    onUpdateIp: (String) -> Unit,
    onUpdateHttpPort: (Int) -> Unit,
    onUpdateHttpsPort: (Int) -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAllApps: () -> Unit,
    onDeselectAllApps: () -> Unit,
    onClearLogs: () -> Unit,
    onInstallCertificate: () -> Unit,
    onUpdateAutoStart: (Boolean) -> Unit,
    onUpdatePersistentNotification: (Boolean) -> Unit,
    onUpdateAlertOnIntercept: (Boolean) -> Unit,
    proxySettings: ProxySettings,
    apps: List<AppInfo>,
    logs: List<LogEntry>
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
                        onVpnStatusChange = onVpnStatusChange,
                        onStartProxy = onStartProxy,
                        onStopProxy = onStopProxy,
                        targetIp = proxySettings.targetIp,
                        httpPort = proxySettings.httpPort,
                        onIpChange = onUpdateIp,
                        onPortChange = onUpdateHttpPort,
                        logs = logs.map { it.message }
                    )
                }

                Screen.APPS -> {
                    AppsScreen(
                        onToggleAppSelection = onToggleApp,
                        onSelectAll = onSelectAllApps,
                        onDeselectAll = onDeselectAllApps,
                        apps = apps
                    )
                }

                Screen.SETTINGS -> {
                    SettingsScreen(
                        currentThemeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        onClearLogs = onClearLogs,
                        onInstallCertificate = onInstallCertificate,
                        proxySettings = proxySettings,
                        onUpdateHttpPort = onUpdateHttpPort,
                        onUpdateHttpsPort = onUpdateHttpsPort,
                        onUpdateAutoStart = onUpdateAutoStart,
                        onUpdatePersistentNotification = onUpdatePersistentNotification,
                        onUpdateAlertOnIntercept = onUpdateAlertOnIntercept
                    )
                }

                else -> { }
            }
        }
    }
}

private fun startVpnService(context: Context, targetAddress: String, settings: ProxySettings) {
    val serviceIntent = Intent(context, BurpBridgeVpnService::class.java).apply {
        action = BurpBridgeVpnService.ACTION_START
        putExtra("TARGET_ADDRESS", targetAddress)
        putExtra(BurpBridgeVpnService.EXTRA_PERSISTENT_NOTIFICATION, settings.persistentNotification)
        putExtra(BurpBridgeVpnService.EXTRA_ALERT_ON_INTERCEPT, settings.alertOnIntercept)
    }
    context.startService(serviceIntent)
}

private fun stopVpnService(context: Context) {
    val stopIntent = Intent(context, BurpBridgeVpnService::class.java).apply {
        action = BurpBridgeVpnService.ACTION_STOP
    }
    context.startService(stopIntent)
}

private fun getCurrentTime(): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}