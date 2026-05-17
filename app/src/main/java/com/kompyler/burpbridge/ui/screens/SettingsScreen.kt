package com.kompyler.burpbridge.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompyler.burpbridge.ui.CertificateDownloadState
import com.kompyler.burpbridge.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onClearLogs: () -> Unit,
    onInstallCertificate: () -> Unit,
    certificateDownloadState: CertificateDownloadState = CertificateDownloadState.Idle,
    onResetCertificateState: () -> Unit = {},
    proxySettings: com.kompyler.burpbridge.ui.ProxySettings,
    onUpdateHttpPort: (Int) -> Unit,
    onUpdateHttpsPort: (Int) -> Unit,
    onUpdateAutoStart: (Boolean) -> Unit,
    onUpdatePersistentNotification: (Boolean) -> Unit,
    onUpdateAlertOnIntercept: (Boolean) -> Unit,
    onNavigateToTargetProfiles: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SettingsHeader()

        Spacer(modifier = Modifier.height(24.dp))

        SystemSettingsSection()

        Spacer(modifier = Modifier.height(24.dp))

        TargetProfilesSection(onNavigate = onNavigateToTargetProfiles)

        Spacer(modifier = Modifier.height(24.dp))

        CACertificateSection(
            isDownloading = certificateDownloadState is CertificateDownloadState.Downloading,
            onInstallCertificate = onInstallCertificate
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProxyModesSection(
            httpPort = proxySettings.httpPort,
            httpsPort = proxySettings.httpsPort,
            onHttpPortChange = onUpdateHttpPort,
            onHttpsPortChange = onUpdateHttpsPort
        )

        Spacer(modifier = Modifier.height(24.dp))

        NotificationSettingsSection(
            persistentNotification = proxySettings.persistentNotification,
            alertOnIntercept = proxySettings.alertOnIntercept,
            onPersistentNotificationChange = onUpdatePersistentNotification,
            onAlertOnInterceptChange = onUpdateAlertOnIntercept
        )

        Spacer(modifier = Modifier.height(24.dp))

        ThemeSection(
            currentThemeMode = currentThemeMode,
            onThemeModeChange = onThemeModeChange
        )

        Spacer(modifier = Modifier.height(100.dp))
    }

    when (val state = certificateDownloadState) {
        is CertificateDownloadState.Installed -> {
            AlertDialog(
                onDismissRequest = onResetCertificateState,
                title = { Text("Certificate Installation") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                        onResetCertificateState()
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onResetCertificateState) {
                        Text("Done")
                    }
                }
            )
        }
        is CertificateDownloadState.Downloaded -> {
            AlertDialog(
                onDismissRequest = onResetCertificateState,
                title = { Text("Certificate Downloaded") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                        onResetCertificateState()
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onResetCertificateState) {
                        Text("Cancel")
                    }
                }
            )
        }
        is CertificateDownloadState.Failed -> {
            AlertDialog(
                onDismissRequest = onResetCertificateState,
                title = { Text("Download Failed") },
                text = {
                    Column {
                        Text(state.message)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("Burp CA URL", state.downloadUrl)
                                    )
                                    Toast.makeText(
                                        context,
                                        "URL copied to clipboard",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Copy URL", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.downloadUrl))
                                try {
                                    context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            "No browser available",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Open URL", fontSize = 11.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                        onResetCertificateState()
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onResetCertificateState) {
                        Text("Cancel")
                    }
                }
            )
        }
        else -> {}
    }
}

@Composable
private fun SettingsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "BURPBRIDGE",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = getCurrentTime(),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SystemSettingsSection() {
    Column {
        Text(
            text = "System Settings",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Configure proxy routing, certificates, and alerts.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun TargetProfilesSection(onNavigate: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Target Profiles",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage saved proxy targets and ports",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CACertificateSection(
    isDownloading: Boolean = false,
    onInstallCertificate: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "CA Certificate Required",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Install this in system settings to decrypt HTTPS traffic. Without this, intercepted secure connections will fail.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = onInstallCertificate,
                    enabled = !isDownloading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isDownloading) "DOWNLOADING..." else "DOWNLOAD BURP CA CERTIFICATE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ProxyModesSection(
    httpPort: Int,
    httpsPort: Int,
    onHttpPortChange: (Int) -> Unit,
    onHttpsPortChange: (Int) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SettingsEthernet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Proxy Modes",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = true,
                        onClick = { },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HTTP Proxy Port",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = httpPort.toString(),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = false,
                        onClick = { },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HTTPS Intercept Port",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = httpsPort.toString(),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsSection(
    persistentNotification: Boolean,
    alertOnIntercept: Boolean,
    onPersistentNotificationChange: (Boolean) -> Unit,
    onAlertOnInterceptChange: (Boolean) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Notification Settings",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                NotificationToggle(
                    title = "Persistent Notification",
                    description = "Keep service active in background",
                    isChecked = persistentNotification,
                    onToggle = onPersistentNotificationChange
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                NotificationToggle(
                    title = "Alert on Intercept",
                    description = "Vibrate and pop-up when paused",
                    isChecked = alertOnIntercept,
                    onToggle = onAlertOnInterceptChange
                )
            }
        }
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    description: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isChecked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun ThemeSection(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Appearance",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeButton(
                        label = "System",
                        icon = Icons.Default.BrightnessAuto,
                        isSelected = currentThemeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeButton(
                        label = "Light",
                        icon = Icons.Default.LightMode,
                        isSelected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeButton(
                        label = "Dark",
                        icon = Icons.Default.DarkMode,
                        isSelected = currentThemeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = label,
            fontSize = 12.sp
        )
    }
}

private fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}