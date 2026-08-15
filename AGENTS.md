# Repository Guidelines

## Project Structure & Module Organization
This repository contains a single Android app module, `:app`, built with Gradle Kotlin DSL. Kotlin sources live under `app/src/main/java/com/elseeker/android`, split by concern: `ui/screen` for Compose screens, `ui/theme` for theming, `webview` for WebView setup and clients, `auth` for social login and token/cookie handling, and `network` for connectivity monitoring. Android resources are in `app/src/main/res`. Product and architecture notes live in `docs/`, especially `docs/android-app-spec.md` and `docs/android-app-architecture.md`.

## Build, Test, and Development Commands
The Gradle wrapper is not checked in, so use a local Gradle 8.7+ installation or Android Studio.

- `gradle assembleDebug`: build the debug APK. Both `debug` and `release` set `BuildConfig.BASE_URL=https://elseeker.com`.
- `gradle assembleRelease`: build the Play-ready release APK with ProGuard enabled.
- `gradle lint`: run Android lint checks for Compose, resources, and manifest issues.
- `gradle test`: run JVM unit tests in `app/src/test` when present.
- `gradle connectedAndroidTest`: run device/emulator tests in `app/src/androidTest`.

## Coding Style & Naming Conventions
Use Kotlin with 4-space indentation and standard Android Studio formatting. Keep package names lowercase, classes and composables in `PascalCase`, methods and properties in `camelCase`, and constants in `UPPER_SNAKE_CASE`. Name screens by role (`MainScreen`, `OfflineScreen`) and helpers by responsibility (`WebViewSetup`, `NetworkMonitor`). Prefer small, focused files and keep WebView-specific logic out of Compose UI where possible.

## Testing Guidelines
There are no committed tests yet, so new behavior should add tests alongside the change. Put JVM tests in `app/src/test/java/...` for ViewModel and routing logic, and instrumentation or UI tests in `app/src/androidTest/java/...` for WebView, back handling, and Compose flows. Mirror production package paths and use `*Test` / `*AndroidTest` suffixes. No coverage threshold is enforced yet, but changes to navigation, connectivity, or auth flows should include regression coverage.

## Commit & Pull Request Guidelines
Follow the convention already in this repository's history: AngularJS-style prefixes (`feat:`, `fix:`, `docs:`, `style:`, `refactor:`, `test:`, `chore:`, `build:`) with the summary written in Korean — e.g. `fix: R8 Full Mode 비활성화를 통한 카카오 SDK 크래시 해결`. Keep the summary short and imperative. Pull requests should describe user-visible behavior, list tested devices or API levels, link the related issue, and include screenshots or recordings for UI changes.

## Security & Configuration Tips
Do not commit secrets or machine-specific SDK paths from `local.properties`. Keep debug-only hosts in build config, preserve `network_security_config.xml`, and verify release changes continue to enforce HTTPS and safe WebView settings.
