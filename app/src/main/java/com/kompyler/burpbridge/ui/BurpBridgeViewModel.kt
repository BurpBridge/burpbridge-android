package com.kompyler.burpbridge.ui

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppInfo(
    val name: String,
    val packageName: String,
    val isSystemApp: Boolean,
    val isSelected: Boolean,
    val isTrusted: Boolean = false
)

data class ProxySettings(
    val targetIp: String = "192.168.1.50",
    val httpPort: Int = 8080,
    val httpsPort: Int = 8443,
    val autoStartVpn: Boolean = false,
    val persistentNotification: Boolean = true,
    val alertOnIntercept: Boolean = false
)

data class LogEntry(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BurpBridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("burpbridge_prefs", Context.MODE_PRIVATE)

    private val _proxySettings = MutableStateFlow(loadProxySettings())
    val proxySettings: StateFlow<ProxySettings> = _proxySettings.asStateFlow()

    private val _isVpnConnected = MutableStateFlow(false)
    val isVpnConnected: StateFlow<Boolean> = _isVpnConnected.asStateFlow()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _certificateInstalled = MutableStateFlow(false)
    val certificateInstalled: StateFlow<Boolean> = _certificateInstalled.asStateFlow()

    init {
        loadCertificateStatus()
    }

    private fun loadProxySettings(): ProxySettings {
        return ProxySettings(
            targetIp = prefs.getString("target_ip", "192.168.1.50") ?: "192.168.1.50",
            httpPort = prefs.getInt("http_port", 8080),
            httpsPort = prefs.getInt("https_port", 8443),
            autoStartVpn = prefs.getBoolean("auto_start_vpn", false),
            persistentNotification = prefs.getBoolean("persistent_notification", true),
            alertOnIntercept = prefs.getBoolean("alert_on_intercept", false)
        )
    }

    fun saveProxySettings(settings: ProxySettings) {
        _proxySettings.value = settings
        prefs.edit().apply {
            putString("target_ip", settings.targetIp)
            putInt("http_port", settings.httpPort)
            putInt("https_port", settings.httpsPort)
            putBoolean("auto_start_vpn", settings.autoStartVpn)
            putBoolean("persistent_notification", settings.persistentNotification)
            putBoolean("alert_on_intercept", settings.alertOnIntercept)
            apply()
        }
    }

    fun updateTargetIp(ip: String) {
        saveProxySettings(_proxySettings.value.copy(targetIp = ip))
    }

    fun updateHttpPort(port: Int) {
        saveProxySettings(_proxySettings.value.copy(httpPort = port))
    }

    fun updateHttpsPort(port: Int) {
        saveProxySettings(_proxySettings.value.copy(httpsPort = port))
    }

    fun updateAutoStartVpn(enabled: Boolean) {
        saveProxySettings(_proxySettings.value.copy(autoStartVpn = enabled))
    }

    fun updatePersistentNotification(enabled: Boolean) {
        saveProxySettings(_proxySettings.value.copy(persistentNotification = enabled))
    }

    fun updateAlertOnIntercept(enabled: Boolean) {
        saveProxySettings(_proxySettings.value.copy(alertOnIntercept = enabled))
    }

    fun setVpnConnected(connected: Boolean) {
        _isVpnConnected.value = connected
    }

    fun loadInstalledApps(context: Context) {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val appList = installedApps
            .filter { it.packageName != context.packageName }
            .map { appInfo ->
                AppInfo(
                    name = pm.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isSelected = false,
                    isTrusted = isTrustedApp(appInfo.packageName)
                )
            }
            .sortedBy { it.name.lowercase() }

        _apps.value = appList
    }

    fun toggleAppSelection(packageName: String) {
        _apps.value = _apps.value.map { app ->
            if (app.packageName == packageName) {
                app.copy(isSelected = !app.isSelected)
            } else app
        }
    }

    fun selectAllApps() {
        _apps.value = _apps.value.map { it.copy(isSelected = true) }
    }

    fun deselectAllApps() {
        _apps.value = _apps.value.map { it.copy(isSelected = false) }
    }

    fun getSelectedApps(): List<String> {
        return _apps.value.filter { it.isSelected }.map { it.packageName }
    }

    private fun isTrustedApp(packageName: String): Boolean {
        val trustedApps = listOf(
            "com.android.chrome",
            "com.google.android.gms",
            "com.android.vending"
        )
        return trustedApps.contains(packageName)
    }

    fun addLog(message: String) {
        _logs.value = _logs.value + LogEntry(message)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun loadCertificateStatus() {
        _certificateInstalled.value = prefs.getBoolean("certificate_installed", false)
    }

    fun setCertificateInstalled(installed: Boolean) {
        _certificateInstalled.value = installed
        prefs.edit().putBoolean("certificate_installed", installed).apply()
    }

    fun getTargetAddress(): String {
        val settings = _proxySettings.value
        return "${settings.targetIp}:${settings.httpPort}"
    }
}