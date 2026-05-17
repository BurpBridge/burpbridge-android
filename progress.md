# BurpBridge Progress Report

## 1. Project Overview

**BurpBridge** is an Android Layer 3 VPN proxy tool that intercepts network traffic through a Burp Suite proxy server. It allows users to configure a target proxy (IP:Port), select which apps to proxy, and manage target profiles for different proxy configurations.

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with StateFlow
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

---

## 2. Architecture

### Application Entry Points
1. **`BurpBridgeApp`** (Application class) - Initializes the ViewModel
2. **`MainActivity`** - Sets up Compose content and theme
3. **`BurpBridgeApp`** (Composable) - Main UI orchestrator

### Key Files

| File | Purpose |
|------|---------|
| `ui/BurpBridgeViewModel.kt` | Central state management, business logic |
| `ui/BurpBridgeApp.kt` | Navigation, permissions, VPN control |
| `BurpBridgeVpnService.kt` | Android VPN Service implementation |
| `util/CertificateManager.kt` | CA certificate download & storage |
| `ui/screens/*.kt` | Individual UI screens |

---

## 3. UI Screens

### 3.1 SplashScreen
- **Purpose**: App initialization, brief branding
- **Flow**: Navigates to Dashboard after a delay

### 3.2 DashboardScreen
- **Purpose**: Main control center
- **Features**:
  - Power button (neumorphic style with Burp Orange glow)
  - VPN status display (PROXY ACTIVE / OFFLINE)
  - Target IP and HTTP Port configuration fields
  - Session timer (HH:mm:ss format)
  - Profile selector dropdown
  - System log terminal with orange bracket highlights
  - Settings gear icon → Target Profiles screen

### 3.3 AppsScreen
- **Purpose**: App selection for proxy interception
- **Features**:
  - Search bar for filtering apps by name
  - Sort dropdown (A-Z / Z-A)
  - Real app icons from PackageManager
  - "Proxy All Apps" toggle switch (hides app list)
  - "Customize Selection" button when Proxy All is active
  - Checkboxes for individual app selection

### 3.4 SettingsScreen
- **Purpose**: Configuration and certificate management
- **Sections**:
  - System Settings (header)
  - Target Profiles (navigation to CRUD screen)
  - CA Certificate download + install instructions
  - Proxy Modes (HTTP/HTTPS ports display)
  - Notification Settings (persistent notification, alert on intercept)
  - Appearance (System/Light/Dark theme toggles)

### 3.5 TargetProfilesScreen
- **Purpose**: Manage saved proxy configurations
- **Features**:
  - List of profiles with selection indicator (orange badge)
  - Add new profile dialog (name, IP, ports)
  - Edit existing profile dialog
  - Delete profile with confirmation
  - Three default profiles: Localhost (127.0.0.1:8080), LAN Proxy (192.168.1.50:8080), Burp Cloud (10.0.0.1:8080)

---

## 4. Data Models

### ProxySettings
- targetIp: String (default: "192.168.1.50")
- httpPort: Int (default: 8080)
- httpsPort: Int (default: 8443)
- autoStartVpn: Boolean (default: false)
- persistentNotification: Boolean (default: true)
- alertOnIntercept: Boolean (default: false)

### AppInfo
- name: String
- packageName: String
- isSystemApp: Boolean
- isSelected: Boolean
- isTrusted: Boolean

### TargetProfile
- id: String (UUID)
- name: String
- targetIp: String
- httpPort: Int
- httpsPort: Int (default: 8443)

### CertificateDownloadState (Sealed Class)
- Idle
- Downloading
- Downloaded(message: String)
- Failed(message: String, downloadUrl: String)

---

## 5. ViewModel State Management

### StateFlows Exposed

| StateFlow | Type | Purpose |
|-----------|------|---------|
| proxySettings | ProxySettings | Current proxy configuration |
| isVpnConnected | Boolean | VPN connection status |
| apps | List\<AppInfo\> | All installed apps |
| logs | List\<LogEntry\> | System logs (max 50 entries) |
| certificateInstalled | Boolean | Whether CA cert is installed |
| proxyAllApps | Boolean | Proxy all apps toggle |
| sessionDuration | String | Timer (HH:mm:ss) |
| targetProfiles | List\<TargetProfile\> | Saved profiles |
| selectedProfileId | String? | Currently selected profile |
| certificateDownloadState | CertificateDownloadState | CA cert download state |

### Key Functions

| Function | Purpose |
|----------|---------|
| updateTargetIp(ip) | Update proxy IP, clears profile selection |
| updateHttpPort(port) | Update HTTP port, clears profile selection |
| updateHttpsPort(port) | Update HTTPS port |
| setVpnConnected(bool) | Update VPN state, starts/stops session timer |
| loadInstalledApps(context) | Load all apps via PackageManager |
| toggleAppSelection(packageName) | Toggle individual app selection |
| selectAllApps() / deselectAllApps() | Bulk select/deselect apps |
| setProxyAllApps(bool) | Enable proxy for all apps |
| getSelectedApps() | Return selected package names |
| addLog(message) | Add log entry with timestamp, capped at 50 |
| clearLogs() | Clear all system logs |
| downloadBurpCertificate() | Download CA cert from proxy server |
| resetCertificateDownloadState() | Reset download state to Idle |
| selectProfile(profileId) | Apply profile settings (IP + ports) |
| addProfile(name, ip, http, https) | Create new profile |
| deleteProfile(profileId) | Remove profile from list |
| updateProfile(profile) | Modify existing profile |

---

## 6. VPN Service (BurpBridgeVpnService)

### Service Configuration
- **Type**: Foreground Service with `specialUse` subtype
- **Permission**: `BIND_VPN_SERVICE`
- **Actions**:
  - `ACTION_START` — Initialize VPN with target address
  - `ACTION_STOP` — Gracefully shutdown

### Core Methods

| Method | Purpose |
|--------|---------|
| startVpn(targetAddress) | Configure VPN interface, start Go proxy |
| stopVpn() | Stop Go proxy, close VPN interface |
| broadcastStatus(status) | Send status broadcast to app |
| onRevoke() | Handle VPN permission revocation |

### Safety Features
- **stopProxyGuard** (AtomicBoolean) — prevents double call to `Mobile.stopProxy()`, avoids Go "close of closed channel" panic
- **5-second join timeout** — wait for Go proxy to stop gracefully before proceeding
- **START_STICKY** — service restarts if killed by system

---

## 7. Certificate Management (CertificateManager)

### Purpose
Download Burp Suite CA certificate from the proxy server and guide the user through manual installation via Android Settings.

### Data Flow
1. **Input**: Target IP + Port from current proxy settings
2. **Download**: HTTP GET to `http://<ip>:<port>/cert`
3. **Storage**: Save to `Downloads/burpsuite/burp_cacert.der` via MediaStore
4. **Output**: Show AlertDialog with manual install instructions

### Key Methods

| Method | Purpose |
|--------|---------|
| downloadCertificate(ip, port) | Downloads cert from proxy (10s timeout) |
| saveToDownloads(data) | Saves to public Downloads with API-aware strategy |
| saveViaMediaStore(data) | API 29+: MediaStore.Downloads |
| saveLegacy(data) | Below API 29: direct file write |
| getDownloadUrl(ip, port) | Returns `http://<ip>:<port>/cert` |
| getDownloadedMessage() | Returns install instructions text |
| getFailedMessage(url) | Returns error + manual steps |

### Network Security Config
Allows cleartext HTTP to proxy server and trusts user-installed certificates:

```xml
<base-config cleartextTrafficPermitted="true">
    <trust-anchors>
        <certificates src="system" />
        <certificates src="user" />
    </trust-anchors>
</base-config>
```

---

## 8. User Interaction Flows

### VPN Start Flow
1. User taps Power Button on Dashboard
2. Check `VpnService.prepare()` — if permission needed, launch system dialog
3. On permission granted, start service with target address
4. VPN Service configures interface and starts Go proxy
5. Broadcast `VPN_STATUS_STARTED` → ViewModel updates state → UI shows "PROXY ACTIVE"

### VPN Stop Flow
1. User taps Power Button (when connected)
2. Service receives `ACTION_STOP`
3. Go proxy stop attempted (5s timeout)
4. VPN interface closed
5. Broadcast `VPN_STATUS_STOPPED` → UI shows "OFFLINE"

### Certificate Download Flow
1. User taps "DOWNLOAD BURP CA CERTIFICATE" in Settings
2. App HTTP GETs `http://<proxyIp>:<port>/cert`
3. **On success**: Save to `Downloads/burpsuite/burp_cacert.der`, show AlertDialog with manual install instructions + "Open Settings" button
4. **On failure**: Show AlertDialog with error message + "Copy URL" button + "Open URL in Browser" button + "Open Settings" button

### Profile Selection Flow
1. User selects profile from dropdown on Dashboard
2. ViewModel applies profile settings (IP, HTTP port, HTTPS port)
3. Profile ID stored in `_selectedProfileId`
4. Manual IP/Port edits in config card clear profile selection

---

## 9. Permissions

| Permission | Purpose |
|------------|---------|
| INTERNET | Connect to proxy server |
| ACCESS_NETWORK_STATE | Detect VPN connection status |
| FOREGROUND_SERVICE | Run VPN as foreground service |
| FOREGROUND_SERVICE_SPECIAL_USE | VPN special use subtype |
| POST_NOTIFICATIONS | Show notifications (Android 13+) |
| QUERY_ALL_PACKAGES | List all installed apps (Android 11+) |

---

## 10. Completed Features

- [x] Cyberpunk-themed UI (dark mode, orange accents, dotted grid)
- [x] VPN Service with Go proxy integration
- [x] App selector with search and sort
- [x] "Proxy All Apps" toggle
- [x] Target Profiles (CRUD + JSON persistence in SharedPreferences)
- [x] Session timer with HH:mm:ss format
- [x] System logs (terminal style, 50 entry cap)
- [x] CA Certificate downloader with manual install instructions
- [x] Network security config for cleartext HTTP
- [x] Theme selector (System/Light/Dark)
- [x] Notification settings (persistent, alert on intercept)
- [x] VPN crash fix (double-close channel guard)

---

## 11. Color Scheme

| Color | Hex | Usage |
|-------|-----|-------|
| CyberBackground | #141414 | Main background |
| CyberCardSurface | #1E1E1E | Card backgrounds |
| CyberCardOutline | #2A2A2A | Card borders |
| CyberGridDot | #242424 | Background dotted grid |
| CyberSecondaryText | #888888 | Muted text |
| Burp Orange | #FF6633 | Active states, accents, glows |
| PrimaryContainerDark | #FF663333 | Container backgrounds |

*Report generated on 2026-05-17 from codebase analysis*
