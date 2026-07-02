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

**전부 네이티브 (Single Activity + Jetpack Compose + MVVM + Hilt)** — `MainActivity` 하나가 Compose Navigation 으로 모든 화면을 렌더링한다. **WebView 는 사용하지 않는다**(PRD §4-A). 콘텐츠는 REST API(`the_bible_project` 백엔드) 또는 정적 번들로 네이티브 렌더링한다.

```
MainActivity (@AndroidEntryPoint)
  ├─ AppViewModel  → 콜드 스타트 세션 복원 → AuthState
  └─ ElSeekerApp(authState)
       ├─ Unknown        → 스플래시 유지
       ├─ Unauthenticated → LoginScreen (소셜 3종)
       ├─ NeedsConsent    → ConsentScreen (약관 동의)
       └─ Authenticated   → MainScaffold (하단탭: 홈/성경/학습/마이) + NavHost
```

Key design decisions:
- **전부 네이티브 화면 + Compose Navigation** — WebView/Thymeleaf 렌더링 미사용
- **Retrofit + OkHttp + Kotlinx Serialization** — Bearer 인터셉터 + `/reissue` Authenticator(회전 없음)
- **Hilt** DI — `core/di/NetworkModule` 가 auth/no-auth 클라이언트와 API 서비스 제공
- **EncryptedSharedPreferences** 토큰 저장, JWT `scope=SIGNUP` 로컬 판별로 세션 복원 게이트(PRD §5.4)
- **No Room/database (v1)** — 데이터는 서버. 오프라인 캐시는 2차

### Source Layout (`app/src/main/java/com/elseeker/android/`)

- `MainActivity.kt` — 단일 Activity. 소셜 SDK 호출(Google CM/Kakao/Naver)·스플래시·인앱 업데이트
- `ElSeekerApplication.kt` — `@HiltAndroidApp`, Kakao/Naver SDK 초기화
- `app/` — `AppViewModel`(세션 복원), `ElSeekerApp`(인증 라우팅), `MainScaffold`(하단탭+NavHost), `navigation/Destinations`
- `core/network/` — `AuthInterceptor`, `TokenAuthenticator`, `TokenRefreshService`, `ApiException`, `SafeApiCall`, `ErrorResponse`
- `core/auth/` — `AuthTokenStore`(암호화 저장), `SessionManager`(AuthState), `JwtDecoder`(scope 판별)
- `core/di/NetworkModule.kt` — Hilt: Json/OkHttp/Retrofit/API 서비스
- `core/ui/` — `UiResource`, `ResourceContent`(로딩/오류 표준)
- `feature/auth/` — data(API/DTO)·domain(`AuthRepository`)·ui(Login/Consent/`AuthViewModel`)
- `feature/bible/` — data(`BibleApi`/DTO)·domain(`BibleRepository`)·ui(Books/Reader 화면+VM)
- `feature/study/` — data(`DictionaryApi`/Repository)·ui(StudyScreen 사전)
- `feature/home/`, `feature/my/` — 오늘의 말씀, 프로필/로그아웃
- `ui/theme/Theme.kt`, `Color.kt` — Material3 테마(라이트/다크)

> 오프라인/네트워크 오류는 요청 단위로 `ApiException.isNetwork` → `UiResource.Error`(재시도 버튼)로 처리한다. 전역 연결 배너는 후속.

### 외부 위임 (WebView 미사용 예외)

- 외부 링크(YouTube 등)·약관 페이지 등은 Chrome Custom Tabs(`androidx.browser`) 또는 시스템 브라우저로 위임

## Build Configuration

- **Kotlin 2.0.21**, Java 17 target / KSP `2.0.21-1.0.28`
- **AGP 8.13.2**, Compile/Target SDK 35, Min SDK 26
- **Hilt 2.52**, Retrofit 2.11.0 + OkHttp 4.12.0 + Kotlinx Serialization 1.7.3, Navigation Compose 2.8.5, Coil 2.7.0
- **Gradle Kotlin DSL** with version catalog (`gradle/libs.versions.toml`)
- **Compose BOM 2024.12.01** (Material3)
- Build variants: `debug` (debuggable) / `release` (HTTPS enforced, R8 minification)
- ProGuard: Signature/애노테이션 유지(Retrofit 제네릭), Kotlinx Serialization keep, `BuildConfig`
- ⚠️ **WSL에서는 빌드 불가** (Android SDK가 Windows 측, `gradlew` 부재). 빌드/실행은 Windows의 Android Studio 또는 `gradlew` 로 수행

## Phased Roadmap

- **Phase 1 (current MVP)**: WebView shell, OAuth, offline/error handling, splash, in-app update
- **Phase 2**: Deep links / App Links, offline Bible cache, native share via JS Bridge
- **Phase 3**: FCM push notifications, local reminders, home widget

Detailed specs in `docs/android-app-spec.md` and `docs/android-app-architecture.md`.
