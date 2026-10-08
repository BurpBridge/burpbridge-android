# BurpBridge

An Android VPN proxy tool that routes device traffic through a Burp Suite proxy server. Built with Jetpack Compose and a Go-based VPN backend.

## Features

- **VPN Proxy** — Routes selected app traffic through any Burp Suite proxy (IP + port)
- **App Selector UI** — Choose individual apps or enable "Proxy All"; VPN allowlist integration is still pending ([#1](https://github.com/BurpBridge/burpbridge-android/issues/1))
- **Target Profiles** — Save and switch between multiple proxy configurations
- **Session Timer** — Tracks active proxy session duration
- **System Logs** — Terminal-style log with real-time proxy events
- **CA Certificate Download** — Downloads Burp CA cert from proxy server with install instructions
- **Theme Support** — Cyberpunk dark theme with orange accents, plus light mode variant
- **Persistent Notification** — Foreground service notification with reshow-on-dismiss option

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM with StateFlow |
| VPN Backend | Go (via AAR bindings) |
| Min SDK | 35 (Android 15) |
| Target SDK | 36 (Android 16) |

## Getting Started

### Prerequisites

- Android Studio compatible with Android Gradle Plugin 9.2.1
- JDK 21 (selected by the Gradle daemon criteria)
- Android SDK platform 36.1 and build tools 36.0.0

### Building

```bash
git clone https://github.com/BurpBridge/burpbridge-android.git
cd burpbridge-android
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Installing

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. **Open the app** — Splash screen → Dashboard
2. **Configure proxy** — Enter your Burp Suite listener IP and port
3. **(Optional) Select apps** — Choose which apps to route through the proxy
4. **Start proxy** — Tap the power button and grant VPN permission
5. **Stop proxy** — Tap the power button again

### First-Time Setup

1. Configure your target IP and port on the Dashboard
2. Open Settings → Download Burp CA Certificate
3. Install the certificate on your device (follow the on-screen instructions)
4. Select which apps to intercept (or enable "Proxy All Apps")

## Architecture

```
MainActivity
└── BurpBridgeTheme (Material 3 + cyberpunk palette)
    └── BurpBridgeApp (composable)
        ├── SplashScreen
        └── MainScaffold
            ├── DashboardScreen   — proxy controls, config, logs
            ├── AppsScreen        — app selection
            ├── SettingsScreen    — theme, cert, notifications
            └── TargetProfilesScreen — saved profile CRUD
```

- **BurpBridgeViewModel** — Central state management via StateFlow
- **BurpBridgeVpnService** — Android VPN Service wrapping Go proxy via `burpbridge.aar`
- **CertificateManager** — Downloads and saves Burp CA certificate

## Permissions

| Permission | Purpose |
|------------|---------|
| INTERNET | Connect to proxy server |
| FOREGROUND_SERVICE | Run VPN as foreground service |
| POST_NOTIFICATIONS | VPN status notifications (Android 13+) |
| QUERY_ALL_PACKAGES | List installed apps for selection (Android 11+) |
| BIND_VPN_SERVICE | VPN tunnel creation |

## Color Scheme

| Token | Dark | Light |
|-------|------|-------|
| Background | `#141414` | `#F5F0EB` |
| Surface | `#1E1E1E` | `#FFFFFF` |
| Accent | `#FF6633` | `#C04E1A` |
| Text | `#E0E0E0` | `#3C2A1E` |
| Muted | `#888888` | `#8C7A6B` |

## License

```
Copyright 2026 Kompyler

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for setup, beginner issues, and required DCO/GPG commit signing. Report security issues privately as described in [SECURITY.md](SECURITY.md).

## Known limitations

- App selections do not yet configure the VPN service's capture policy ([#1](https://github.com/BurpBridge/burpbridge-android/issues/1)).
- The dashboard can mistake another active VPN for BurpBridge ([#4](https://github.com/BurpBridge/burpbridge-android/issues/4)).
- Tests are still templates; device traffic behavior needs meaningful coverage ([#7](https://github.com/BurpBridge/burpbridge-android/issues/7)).
- The native AAR needs a documented, pinned source build ([#6](https://github.com/BurpBridge/burpbridge-android/issues/6)).

See the [issue backlog](https://github.com/BurpBridge/burpbridge-android/issues) for scoped contributions.
