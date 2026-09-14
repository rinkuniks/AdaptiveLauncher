# Adaptive Launcher

One-handed, minimal, adaptive Android launcher — 14 phases, Kotlin + Compose, Room + DataStore + Hilt.

## Quick resume (tomorrow)

### Paths
- Primary (Studio): `D:/AIProjects/Android Launcher/AdaptiveLauncher`
- Mirror (CLI, no-space): `D:/AdaptiveLauncher`  (cp -r primary -> mirror before CLI builds)

### Verified stack (Sep 14, 2026)
- AGP 8.13.0 / Gradle 8.14.3 / JDK 21.0.10 (`C:/tmpjbr_test/jbr`)
- Kotlin 2.1.20 / KSP 2.1.20-1.0.32 / Compose BOM 2025.08.00 / Room 2.8.4 / DataStore 1.1.4 / Hilt 2.56.2
- compileSdk 36 / targetSdk 36 / minSdk 29

### Build
```bash
export JAVA_HOME="/c/tmpjbr_test/jbr"
export ANDROID_HOME="C:/Users/Nikhil/AppData/Local/Android/Sdk"
export PATH="$JAVA_HOME/bin:$PATH"
export MSYS_NO_PATHCONV=1
# from D:/AdaptiveLauncher:
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk` (63M, vCode 1 vName 0.1.0)
Last verified on Nothing A001 (00198656U001704) Android 16 API 36 — 1080x2392 @375dpi sw460dp — `adb install -r` Success, HomeActivity is default.

### What's done (82%)
Phases 0-5 done earlier; this session fixed:
- Theme now reactive (HomeActivity/MainActivity observe ThemeRepository.mode)
- Filter All/Installed(System) + HomeMode AutoMajor/Custom/ShowAll + swipe-up drawer
- 8 test suites, 42 tests passing

Tests: `./gradlew testDebugUnitTest` — 42/42

Open: press Home or `adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME`

See `AdaptiveLauncher_Roadmap.md` for full roadmap.
