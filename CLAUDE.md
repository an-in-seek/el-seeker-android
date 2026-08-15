# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ElSeeker is an Android app that wraps the ElSeeker web service (elseeker.com) — a Korean Bible study platform — using WebView. The app provides native capabilities (splash screen, offline handling, back navigation, in-app updates) around server-rendered Thymeleaf content. Documentation is in Korean; the app targets Korean-speaking users.

## Build Commands

**The Gradle wrapper is not committed** — `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` are absent from a fresh clone (only `gradle-wrapper.properties` is tracked). Android Studio generates them on first project sync. Until then, use a local Gradle 8.7+ installation and drop the `./` prefix (`gradle assembleDebug`).

```bash
# Debug build (uses https://elseeker.com as BASE_URL)
./gradlew assembleDebug

# Release build (uses https://elseeker.com, minified with R8)
./gradlew assembleRelease

# Install on connected device/emulator
./gradlew installDebug

# Android Lint
./gradlew lint
```

No test infrastructure exists yet — no test dependencies or test source files.

## Architecture

**Single Activity + WebView + MVVM** — one activity (`ElSeekerActivity`) hosts Jetpack Compose UI that wraps a WebView for all content.

```
ElSeekerActivity
  └─ ElSeekerViewModel (StateFlow<UiState>)
       ├─ UiState.Loading  → SplashScreen (Android 12+ API)
       ├─ UiState.Ready    → WebView + progress bar
       ├─ UiState.NoNetwork → OfflineScreen (Compose)
       └─ UiState.Error    → ErrorScreen (Compose)
```

Key design decisions:
- **No native navigation/screens** — all pages are Thymeleaf SSR; the web already has a mobile-optimized bottom tab bar
- **No networking library** (no Retrofit/OkHttp) — content loads via WebView
- **No DI framework** (no Hilt/Dagger) — simple enough for direct construction
- **No Room/database** — all data lives on the server

### Source Layout (`app/src/main/java/com/elseeker/android/`)

- `ElSeekerApplication.kt` — Application class, WebView debug setup
- `ElSeekerActivity.kt` — Single activity, splash screen, back button handling
- `ElSeekerViewModel.kt` — UI state management, network state
- `network/NetworkMonitor.kt` — Connectivity monitoring via StateFlow
- `webview/WebViewSetup.kt` — Centralized WebView configuration (settings, cookies, user-agent)
- `webview/ElSeekerWebViewClient.kt` — URL routing (internal vs external), error handling
- `webview/ElSeekerWebChromeClient.kt` — Progress tracking, console logging
- `ui/screen/MainScreen.kt` — Main Compose screen with WebView
- `ui/screen/ErrorScreen.kt`, `OfflineScreen.kt` — Native error/offline overlays
- `ui/theme/Theme.kt`, `Color.kt` — Material3 theming

### URL Routing

- Internal URLs (elseeker.com) load in WebView
- Google OAuth uses Chrome Custom Tabs (`androidx.browser`)
- Kakao/Naver OAuth loads in WebView
- External URLs (YouTube, etc.) open in system browser

## Build Configuration

- **Kotlin 2.0.21**, Java 17 target
- **AGP 8.13.2**, Compile/Target SDK 35, Min SDK 26
- **Gradle Kotlin DSL** with version catalog (`gradle/libs.versions.toml`)
- **Compose BOM 2024.12.01** (Material3)
- Build variants: `debug` (debuggable) / `release` (HTTPS enforced, R8 minification)
- ProGuard keeps `@JavascriptInterface` methods on future JS Bridge class and `BuildConfig`

## Phased Roadmap

- **Phase 1 (current MVP)**: WebView shell, OAuth, offline/error handling, splash, in-app update
- **Phase 2**: Deep links / App Links, offline Bible cache, native share via JS Bridge
- **Phase 3**: FCM push notifications, local reminders, home widget

Detailed specs in `docs/android-app-spec.md` and `docs/android-app-architecture.md`.
