package com.kompyler.burpbridge.ui

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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

data class TargetProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetIp: String,
    val httpPort: Int,
    val httpsPort: Int = 8443
)

sealed class CertificateDownloadState {
    object Idle : CertificateDownloadState()
    object Downloading : CertificateDownloadState()
    data class Downloaded(val message: String) : CertificateDownloadState()
    data class Failed(val message: String, val downloadUrl: String) : CertificateDownloadState()
}

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

    private val _proxyAllApps = MutableStateFlow(false)
    val proxyAllApps: StateFlow<Boolean> = _proxyAllApps.asStateFlow()

    private val _sessionDuration = MutableStateFlow("00:00:00")
    val sessionDuration: StateFlow<String> = _sessionDuration.asStateFlow()
    private var sessionTimerJob: Job? = null

    private val _targetProfiles = MutableStateFlow<List<TargetProfile>>(emptyList())
    val targetProfiles: StateFlow<List<TargetProfile>> = _targetProfiles.asStateFlow()

    private val _selectedProfileId = MutableStateFlow<String?>(null)
    val selectedProfileId: StateFlow<String?> = _selectedProfileId.asStateFlow()

    private val _certificateDownloadState = MutableStateFlow<CertificateDownloadState>(CertificateDownloadState.Idle)
    val certificateDownloadState: StateFlow<CertificateDownloadState> = _certificateDownloadState.asStateFlow()

    init {
        loadCertificateStatus()
        loadProfiles()
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
        _selectedProfileId.value = null
        saveProxySettings(_proxySettings.value.copy(targetIp = ip))
    }

    fun updateHttpPort(port: Int) {
        _selectedProfileId.value = null
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
        if (connected) {
            startSessionTimer()
        } else {
            stopSessionTimer()
        }
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        _sessionDuration.value = "00:00:00"
        var elapsed = 0L
        sessionTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                elapsed++
                val h = elapsed / 3600
                val m = (elapsed % 3600) / 60
                val s = elapsed % 60
                _sessionDuration.value = String.format("%02d:%02d:%02d", h, m, s)
            }
        }
    }

    private fun stopSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = null
        _sessionDuration.value = "00:00:00"
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

    fun setProxyAllApps(enabled: Boolean) {
        _proxyAllApps.value = enabled
    }

    fun getSelectedApps(): List<String> {
        if (_proxyAllApps.value) {
            return _apps.value.map { it.packageName }
        }
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
        _logs.value = (_logs.value + LogEntry(message)).takeLast(50)
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

    fun downloadBurpCertificate() {
        val settings = _proxySettings.value
        val app = getApplication<Application>()
        Log.d("BurpBridge-Cert", "User tapped Download CA cert (target=${settings.targetIp}:${settings.httpPort})")
        _certificateDownloadState.value = CertificateDownloadState.Downloading

        viewModelScope.launch {
            val certManager = com.kompyler.burpbridge.util.CertificateManager(app)
            val downloadUrl = certManager.getDownloadUrl(settings.targetIp, settings.httpPort)
            Log.d("BurpBridge-Cert", "Beginning download from $downloadUrl")
            val result = certManager.downloadCertificate(settings.targetIp, settings.httpPort)

            _certificateDownloadState.value = result.fold(
                onSuccess = {
                    Log.d("BurpBridge-Cert", "Download succeeded, opening Security Settings")
                    CertificateDownloadState.Downloaded(certManager.getDownloadedMessage())
                },
                onFailure = { error ->
                    Log.e("BurpBridge-Cert", "Download failed: ${error.message}")
                    CertificateDownloadState.Failed(
                        message = certManager.getFailedMessage(downloadUrl),
                        downloadUrl = downloadUrl
                    )
                }
            )
            Log.d("BurpBridge-Cert", "Final certificate state: ${_certificateDownloadState.value.javaClass.simpleName}")
        }
    }

    fun resetCertificateDownloadState() {
        _certificateDownloadState.value = CertificateDownloadState.Idle
    }

    fun getTargetAddress(): String {
        val settings = _proxySettings.value
        return "${settings.targetIp}:${settings.httpPort}:${settings.httpsPort}"
    }

    fun selectProfile(profileId: String?) {
        if (profileId == null) {
            _selectedProfileId.value = null
            return
        }
        val profile = _targetProfiles.value.find { it.id == profileId } ?: return
        _selectedProfileId.value = profileId
        saveProxySettings(_proxySettings.value.copy(
            targetIp = profile.targetIp,
            httpPort = profile.httpPort,
            httpsPort = profile.httpsPort
        ))
    }

    fun addProfile(name: String, targetIp: String, httpPort: Int, httpsPort: Int = 8443) {
        val profile = TargetProfile(
            name = name,
            targetIp = targetIp,
            httpPort = httpPort,
            httpsPort = httpsPort
        )
        _targetProfiles.value = _targetProfiles.value + profile
        saveProfiles()
    }

    fun deleteProfile(profileId: String) {
        _targetProfiles.value = _targetProfiles.value.filter { it.id != profileId }
        if (_selectedProfileId.value == profileId) {
            _selectedProfileId.value = null
        }
        saveProfiles()
    }

    fun updateProfile(profile: TargetProfile) {
        _targetProfiles.value = _targetProfiles.value.map {
            if (it.id == profile.id) profile else it
        }
        if (_selectedProfileId.value == profile.id) {
            saveProxySettings(_proxySettings.value.copy(
                targetIp = profile.targetIp,
                httpPort = profile.httpPort,
                httpsPort = profile.httpsPort
            ))
        }
        saveProfiles()
    }

    private fun loadProfiles() {
        val json = prefs.getString("target_profiles", null)
        if (json != null) {
            try {
                val jsonArray = JSONArray(json)
                val profiles = mutableListOf<TargetProfile>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    profiles.add(TargetProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        targetIp = obj.getString("targetIp"),
                        httpPort = obj.getInt("httpPort"),
                        httpsPort = obj.getInt("httpsPort")
                    ))
                }
                _targetProfiles.value = profiles
                return
            } catch (_: Exception) { }
        }
        val defaults = getDefaultProfiles()
        _targetProfiles.value = defaults
        saveProfiles()
    }

    private fun saveProfiles() {
        val jsonArray = JSONArray()
        _targetProfiles.value.forEach { profile ->
            val obj = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("targetIp", profile.targetIp)
                put("httpPort", profile.httpPort)
                put("httpsPort", profile.httpsPort)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("target_profiles", jsonArray.toString()).apply()
    }

    private fun getDefaultProfiles(): List<TargetProfile> {
        return listOf(
            TargetProfile(name = "Localhost", targetIp = "127.0.0.1", httpPort = 8080),
            TargetProfile(name = "LAN Proxy", targetIp = "192.168.1.50", httpPort = 8080),
            TargetProfile(name = "Burp Cloud", targetIp = "10.0.0.1", httpPort = 8080)
        )
    }
}
