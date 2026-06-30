# ElSeeker - 성경 플랫폼 Android 앱

[ElSeeker](https://elseeker.com) 웹 서비스를 WebView로 감싼 Android 네이티브 앱입니다.
웹의 모든 기능을 그대로 제공하면서, 스플래시 스크린·오프라인 처리·하드웨어 뒤로가기 등 네이티브 앱 경험을 추가합니다.

## 주요 기능

- **WebView 기반 콘텐츠** — 성경 읽기, 학습, 커뮤니티, 게임 등 모든 웹 페이지를 앱에서 제공
- **소셜 로그인** — Google (Chrome Custom Tabs), Kakao, Naver OAuth2 지원
- **스플래시 스크린** — Android 12+ SplashScreen API 기반
- **오프라인 처리** — 네트워크 상태 모니터링, 네이티브 에러/오프라인 화면 + 자동 복구
- **하드웨어 뒤로가기** — WebView 히스토리 탐색, 두 번 누르면 종료
- **외부 링크 라우팅** — YouTube는 앱으로, 기타 외부 링크는 시스템 브라우저로 이동
- **In-App Update** — Google Play In-App Update API로 앱 업데이트 유도

## 기술 스택

| 항목 | 기술 |
|------|------|
| 패키지 이름 | `com.elseeker.android` |
| 언어 | Kotlin 2.0.21 |
| Min SDK | 26 (Android 8.0) |
| Target / Compile SDK | 35 |
| UI | Jetpack Compose (Material3) + WebView |
| 빌드 | Gradle Kotlin DSL, Version Catalog |
| 아키텍처 | Single Activity + ViewModel (MVVM) |

### 주요 의존성

- Compose BOM 2024.12.01
- AndroidX Webkit 1.12.1
- AndroidX SplashScreen 1.0.1
- AndroidX Browser 1.8.0 (Chrome Custom Tabs)
- Google Play In-App Update 2.1.0

## 프로젝트 구조

```
app/src/main/java/com/elseeker/android/
├── ElSeekerApplication.kt          # Application 클래스, WebView 디버그 설정
├── ElSeekerActivity.kt             # Single Activity, 스플래시, 뒤로가기 처리
├── ElSeekerViewModel.kt            # UI 상태 관리 (Loading/Ready/NoNetwork/Error)
├── network/
│   └── NetworkMonitor.kt           # ConnectivityManager 기반 네트워크 상태 모니터링
├── webview/
│   ├── WebViewSetup.kt             # WebView 설정 (쿠키, User-Agent 등)
│   ├── ElSeekerWebViewClient.kt    # URL 라우팅, 에러 처리
│   └── ElSeekerWebChromeClient.kt  # 로딩 진행률, 콘솔 로그
└── ui/
    ├── theme/
    │   ├── Theme.kt                # Material3 테마
    │   └── Color.kt                # 색상 정의
    └── screen/
        ├── MainScreen.kt           # WebView + 프로그레스 바
        ├── ErrorScreen.kt          # 에러 화면
        └── OfflineScreen.kt        # 오프라인 화면
```

## 빌드 및 실행

### 요구사항

- JDK 17+
- Android Studio Ladybug 이상 권장

### 빌드 명령

```bash
# Debug 빌드
./gradlew assembleDebug

# Release 빌드 (R8 난독화 적용)
./gradlew assembleRelease

# 연결된 디바이스/에뮬레이터에 설치
./gradlew installDebug

# Lint 검사
./gradlew lint
```

### 빌드 변형

| 변형 | BASE_URL | 디버그 | 난독화 |
|------|----------|--------|--------|
| debug | `https://elseeker.com` | O | X |
| release | `https://elseeker.com` | X | O (R8) |

## 아키텍처

```
ElSeekerActivity (Single Activity)
  └─ ElSeekerViewModel (StateFlow<UiState>)
       ├─ UiState.Loading   → SplashScreen
       ├─ UiState.Ready     → WebView + 프로그레스 바
       ├─ UiState.NoNetwork → OfflineScreen (Compose)
       └─ UiState.Error     → ErrorScreen (Compose)
```

- 모든 콘텐츠는 서버 사이드 렌더링(Thymeleaf SSR) → 네이티브 화면 불필요
- 웹의 모바일 Bottom Tab Bar를 그대로 사용 (이중 네비게이션 방지)
- DI 프레임워크/네트워킹 라이브러리/로컬 DB 없이 경량 구성

### URL 라우팅

| URL 패턴 | 처리 방식 |
|-----------|-----------|
| elseeker.com/* | WebView 내부 로드 |
| Kakao/Naver OAuth | WebView 내부 로드 |
| Google OAuth | Chrome Custom Tabs |
| YouTube | YouTube 앱 또는 시스템 브라우저 |
| 기타 외부 URL | 시스템 브라우저 |

## 로드맵

### Phase 1 — MVP (현재)
- [x] WebView 셸 + OAuth 소셜 로그인
- [x] 스플래시 스크린, 오프라인/에러 처리
- [x] 하드웨어 뒤로가기, 외부 링크 라우팅
- [x] In-App Update

### Phase 2 — Enhanced Experience
- [ ] Android App Links (딥링크)
- [ ] 오프라인 성경 캐시
- [ ] 네이티브 공유 (JS Bridge)

### Phase 3 — Native Features
- [ ] FCM 푸시 알림
- [ ] 로컬 읽기 리마인더
- [ ] 홈 위젯 (오늘의 말씀)

## 연동 프로젝트 (웹 + 백엔드)

프로젝트 루트의 `the_bible_project/` 는 `/mnt/c/workspace/java/the_bible_project` 를 가리키는 **로컬 심볼릭 링크**로, **ElSeeker 웹 + 백엔드 REST API 서버** 프로젝트입니다.

| 항목 | 내용 |
|------|------|
| 역할 | 서버 사이드 렌더링(Thymeleaf) 페이지 + REST API 제공 |
| 기술 스택 | Spring Boot 3.5, Kotlin (JVM 21), Spring Data JPA, Thymeleaf, Spring Cloud GCP |
| 앱과의 관계 | WebView가 로드하는 웹 페이지 및 네이티브 화면이 호출하는 REST API의 출처 |

> **하이브리드 → 네이티브 전환 맥락**: 현재 앱은 WebView로 Thymeleaf 페이지를 감싸지만, 전환이 진행되면서 네이티브 Compose 화면이 웹 페이지를 대체하고 백엔드 **REST API를 직접 호출**하게 됩니다. 따라서 이 연동 프로젝트는 단순 페이지 출처를 넘어, 네이티브 화면이 맞춰야 할 **API 계약(엔드포인트·DTO), OAuth/약관 동의 흐름, 기존 웹 동작**의 권위 있는 레퍼런스입니다.
>
> 심볼릭 링크는 로컬 개발 참조용(read-only)이며 `.gitignore` 처리되어 커밋되지 않습니다. 네이티브 화면 구현 시 먼저 `the_bible_project/`의 컨트롤러/DTO/API를 확인해 계약을 정확히 맞추세요.

## 문서

- [Product Specification](docs/android-app-spec.md) — 기능 요구사항, 웹 커버리지, OAuth 전략
- [Architecture Design](docs/android-app-architecture.md) — 기술 설계, 코드 구조, 보안

## 라이선스

Private — All rights reserved.
