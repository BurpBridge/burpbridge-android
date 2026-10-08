# Android development

Install JDK 21 and Android SDK platform 36.1 plus build tools 36.0.0. Open this
repository in Android Studio or set `ANDROID_HOME` to your local SDK location.
The wrapper downloads Gradle 9.4.1 and verifies its distribution SHA-256.

```sh
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

CI validates the wrapper, builds the debug APK, runs JVM tests and Android lint,
and retains reports for 14 days. Instrumented tests can be run on an API 35+
emulator or device with `./gradlew :app:connectedDebugAndroidTest`; they are not
yet run automatically. The current tests do not cover real VPN traffic.

Use a reachable Burp listener on a controlled test network. An emulator's
`127.0.0.1` refers to the emulator; the host is usually reachable at `10.0.2.2`.
Do not capture other people's traffic without authorization. Redact logs.
Installing Burp's CA does not make every target app trust user certificates;
certificate pinning and app network-security policies may prevent interception.

The AAR in `app/libs` is tracked and used directly. CI does not claim to rebuild
or scan its native Go implementation. Reproducible builds from a pinned core
commit and device-level VPN tests are tracked in the project issues.
