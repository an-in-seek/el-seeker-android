# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ElSeeker is an Android app that wraps the ElSeeker web service (elseeker.com) — a Korean Bible study platform — using WebView. The app provides native capabilities (splash screen, offline handling, back navigation, in-app updates) around server-rendered Thymeleaf content. Documentation is in Korean; the app targets Korean-speaking users.

## Backend / Web Project (Linked)

The `the_bible_project/` directory in this repo root is a **symlink** to `/mnt/c/workspace/java/the_bible_project` — the **ElSeeker web + backend REST API server**. Stack: **Spring Boot 3.5 + Kotlin (JVM 21) + Spring Data JPA + Thymeleaf + Spring Cloud GCP**.

This app is undergoing a **hybrid (WebView) → native migration**. As that work proceeds, the backend's role shifts:

- **Current (hybrid):** the app embeds the server-rendered Thymeleaf pages in a WebView.
- **Target (native):** native Compose screens replace WebView pages and call the backend **REST APIs directly**. The linked project is the authoritative source for those API contracts (endpoints, request/response schemas), authentication/OAuth/consent flows, and the existing page behavior each native screen must reproduce.

When implementing a native screen, first read the corresponding controller/DTO/API in `the_bible_project/` to match the contract exactly. The symlink is read-only reference, local-only, and git-ignored (`/the_bible_project`) — do not commit it.

## Build Commands

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
