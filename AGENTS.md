# Repository Guidelines

## Project Structure & Module Organization
This repository contains a single Android app module, `:app`, built with Gradle Kotlin DSL. Kotlin sources live under `app/src/main/java/com/elseeker/android`, split by concern: `ui/screen` for Compose screens, `ui/theme` for theming, `webview` for WebView setup and clients, and `network` for connectivity monitoring. Android resources are in `app/src/main/res`. Product and architecture notes live in `docs/`, especially `docs/android-app-spec.md` and `docs/android-app-architecture.md`.

The `the_bible_project/` directory is a local, git-ignored **symlink** to `/mnt/c/workspace/java/the_bible_project` — the **ElSeeker web + backend REST API server** (Spring Boot 3.5 + Kotlin + JPA + Thymeleaf + Spring Cloud GCP). This app is migrating from a **hybrid WebView wrapper to a native app**: as native Compose screens replace embedded web pages, they call the backend **REST APIs directly** instead of loading Thymeleaf pages. Treat the linked project as the authoritative read-only reference for REST API contracts (endpoints, DTOs), OAuth/consent flows, and the existing web behavior each native screen must reproduce; do not commit the symlink.

## Build, Test, and Development Commands
The Gradle wrapper is not checked in, so use a local Gradle 8.7+ installation or Android Studio.

- `gradle assembleDebug`: build the debug APK using `BuildConfig.BASE_URL=http://10.0.2.2:8080`.
- `gradle assembleRelease`: build the Play-ready release APK with ProGuard enabled.
- `gradle lint`: run Android lint checks for Compose, resources, and manifest issues.
- `gradle test`: run JVM unit tests in `app/src/test` when present.
- `gradle connectedAndroidTest`: run device/emulator tests in `app/src/androidTest`.

## Coding Style & Naming Conventions
Use Kotlin with 4-space indentation and standard Android Studio formatting. Keep package names lowercase, classes and composables in `PascalCase`, methods and properties in `camelCase`, and constants in `UPPER_SNAKE_CASE`. Name screens by role (`MainScreen`, `OfflineScreen`) and helpers by responsibility (`WebViewSetup`, `NetworkMonitor`). Prefer small, focused files and keep WebView-specific logic out of Compose UI where possible.

## Testing Guidelines
There are no committed tests yet, so new behavior should add tests alongside the change. Put JVM tests in `app/src/test/java/...` for ViewModel and routing logic, and instrumentation or UI tests in `app/src/androidTest/java/...` for WebView, back handling, and Compose flows. Mirror production package paths and use `*Test` / `*AndroidTest` suffixes. No coverage threshold is enforced yet, but changes to navigation, connectivity, or auth flows should include regression coverage.

## Commit & Pull Request Guidelines
This workspace snapshot does not include `.git` history, so no local commit convention can be inferred. Use short, imperative Conventional Commit style messages such as `feat(webview): open external video links in browser` or `fix(network): restore pending URL after reconnect`. Pull requests should describe user-visible behavior, list tested devices or API levels, link the related issue, and include screenshots or recordings for UI changes.

## Security & Configuration Tips
Do not commit secrets or machine-specific SDK paths from `local.properties`. Keep debug-only hosts in build config, preserve `network_security_config.xml`, and verify release changes continue to enforce HTTPS and safe WebView settings.
