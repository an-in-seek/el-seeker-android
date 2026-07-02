# ElSeeker - 성경 플랫폼 Android 앱

[ElSeeker](https://elseeker.com) 백엔드(REST API)를 사용하는 **전부 네이티브** Android 앱입니다(Jetpack Compose).
WebView 없이 모든 화면을 네이티브로 렌더링하며, 웹과 동일한 백엔드·계정 체계를 공유합니다. (전환 배경: `docs/android-native-app-prd.md`)

> ⚠️ **빌드 환경**: Android SDK가 Windows 측에 있어 **WSL에서는 빌드 불가**합니다. Windows의 Android Studio 또는 `gradlew` 로 빌드하세요.

## 주요 기능 (v1 MVP 진행 현황)

- **네이티브 인증** — Google(Credential Manager)·Kakao·Naver SDK 로그인 + 약관 동의 + 세션 복원(JWT scope/`/me` status 게이트)
- **성경 읽기(앵커)** — 번역본→책→장→절 본문 네이티브 뷰어, 이전/다음 장 이동(PRD §4-A.9 번역본 게이트: KRV)
- **홈** — 오늘의 말씀(daily verse)
- **학습** — 성경 사전 검색/목록(정적 콘텐츠는 후속)
- **마이** — 프로필 조회, 로그아웃
- **스플래시** — Android 12+ SplashScreen API
- **오류/오프라인 처리** — 요청 단위 로딩/오류 상태 + 재시도
- **In-App Update** — Google Play In-App Update API

> 게임·커뮤니티·정적 학습 콘텐츠(족보·개요영상 등)는 2차/후속 범위입니다. 토큰 갱신은 OkHttp Authenticator + `/api/v1/auth/reissue`(회전 없음)로 처리합니다.

## 기술 스택

| 항목 | 기술 |
|------|------|
| 패키지 이름 | `com.elseeker.android` |
| 언어 | Kotlin 2.0.21 |
| Min SDK | 26 (Android 8.0) |
| Target / Compile SDK | 35 |
| UI | Jetpack Compose (Material3) — WebView 미사용 |
| 빌드 | Gradle Kotlin DSL, Version Catalog |
| 아키텍처 | Single Activity + Compose Navigation + MVVM + Hilt |

### 주요 의존성

- Compose BOM 2024.12.01, Navigation Compose 2.8.5
- Hilt 2.52 (DI)
- Retrofit 2.11.0 + OkHttp 4.12.0 + Kotlinx Serialization 1.7.3
- Coil 2.7.0 (이미지), DataStore / Security-Crypto (토큰)
- AndroidX SplashScreen 1.0.1, Browser 1.8.0 (Custom Tabs)
- 소셜 SDK: Google Credential Manager, Kakao v2-user, Naver OAuth
- Google Play In-App Update 2.1.0

## 프로젝트 구조

```
app/src/main/java/com/elseeker/android/
├── MainActivity.kt                 # 단일 Activity, 소셜 SDK 호출, 스플래시, 인앱 업데이트
├── ElSeekerApplication.kt          # @HiltAndroidApp, Kakao/Naver SDK 초기화
├── app/                            # AppViewModel(세션 복원), ElSeekerApp(인증 라우팅),
│   └── navigation/                 #   MainScaffold(하단탭+NavHost), Destinations
├── core/
│   ├── network/                    # AuthInterceptor, TokenAuthenticator, ApiException, SafeApiCall
│   ├── auth/                       # AuthTokenStore(암호화), SessionManager, JwtDecoder
│   ├── di/NetworkModule.kt         # Hilt: Json/OkHttp/Retrofit/API
│   └── ui/                         # UiResource, ResourceContent (로딩/오류 표준)
├── feature/
│   ├── auth/                       # data·domain(AuthRepository)·ui(Login/Consent/AuthViewModel)
│   ├── bible/                      # data(BibleApi)·domain(BibleRepository)·ui(Books/Reader)
│   ├── study/                      # data(DictionaryApi/Repository)·ui(사전)
│   ├── home/                       # 오늘의 말씀
│   └── my/                         # 프로필/로그아웃
└── ui/theme/                       # Material3 테마(라이트/다크)
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
MainActivity (@AndroidEntryPoint)
  ├─ AppViewModel → 콜드 스타트 세션 복원 → AuthState
  └─ ElSeekerApp(authState)
       ├─ Unknown         → SplashScreen 유지
       ├─ Unauthenticated → LoginScreen (Google/Kakao/Naver)
       ├─ NeedsConsent    → ConsentScreen (약관 3항목 필수)
       └─ Authenticated   → MainScaffold (하단탭 + NavHost)
                              ├─ 홈   (오늘의 말씀)
                              ├─ 성경 (책 목록 → 본문 뷰어)
                              ├─ 학습 (사전)
                              └─ 마이 (프로필/로그아웃)
```

- 전부 네이티브 렌더링(WebView 미사용). REST API 또는 정적 번들로 데이터 공급
- 세션 게이트(PRD §5.4): JWT `scope==SIGNUP` 이면 동의 화면, `/me.status==ACTIVE` 만 메인
- 토큰: EncryptedSharedPreferences 저장, OkHttp Authenticator 가 `/reissue`(회전 없음)로 갱신

### 외부 위임 (WebView 예외)

| 대상 | 처리 방식 |
|------|-----------|
| 외부 링크(YouTube 등)·약관 페이지 | Chrome Custom Tabs / 시스템 브라우저 |

## 로드맵

### M0/M1 — 기반 + 1차 패리티 (현재 진행)
- [x] 네이티브 기반: Hilt DI, Retrofit/OkHttp 네트워크, 토큰 저장·재발급, 에러 매퍼
- [x] 소셜 로그인 3종 + 약관 동의 + 세션 복원(403 CONSENT_REQUIRED 전역 라우팅, 로그아웃 시 SDK 세션 정리)
- [x] 앱 셸: 하단탭, 스플래시(다크 대응), 오프라인 상태·재시도, 뒤로가기 2회 종료
- [x] 성경 읽기(번역본→책→장→절 본문 + 장 선택 picker + 이전/다음 장), 홈(오늘의 말씀), 학습(사전), 마이(프로필)
- [x] 회원 탈퇴(Play 계정삭제 정책), App Links 딥링크(앱측 — 서버 `assetlinks.json` 필요)
- [ ] 성경 검색·하이라이트·메모·읽기진도 **UI** (API 계층 완료, 화면 후속)
- [ ] 책 개요, 사전 상세/참조, 프로필 수정·계정 연동, 내 메모, 1:1 문의
- [ ] 학습 정적 콘텐츠(족보·개요영상·정적 9종·성경역사)

### M2 — 게임 · 커뮤니티 네이티브화
### M3 — 앱 고유 가치 (FCM 푸시 · 오프라인 캐시 · 위젯)
### M4 — 출시/운영 (Play Console, Data Safety, 단계 배포)

> **App Links 활성화 조건**: 서버가 `https://elseeker.com/.well-known/assetlinks.json` 에 릴리스 서명 SHA-256 지문을 호스팅해야 자동 검증(autoVerify)이 동작합니다. 앱 매니페스트·라우팅은 `/bible`, `/study` 경로에 대해 구현 완료.

## 연동 프로젝트 (웹 + 백엔드)

프로젝트 루트의 `the_bible_project/` 는 `/mnt/c/workspace/java/the_bible_project` 를 가리키는 **로컬 심볼릭 링크**로, **ElSeeker 웹 + 백엔드 REST API 서버** 프로젝트입니다.

| 항목 | 내용 |
|------|------|
| 역할 | 서버 사이드 렌더링(Thymeleaf) 페이지 + REST API 제공 |
| 기술 스택 | Spring Boot 3.5, Kotlin (JVM 21), Spring Data JPA, Thymeleaf, Spring Cloud GCP |
| 앱과의 관계 | 네이티브 Compose 화면이 직접 호출하는 REST API의 출처 (WebView 미사용) |

> **네이티브 전환 완료(기반)**: 앱은 WebView 없이 백엔드 **REST API를 직접 호출**하는 전부-네이티브 구조입니다. 따라서 이 연동 프로젝트는 네이티브 화면이 맞춰야 할 **API 계약(엔드포인트·DTO), OAuth/약관 동의 흐름, 정적 콘텐츠 데이터**의 권위 있는 read-only 레퍼런스입니다.
>
> 심볼릭 링크는 로컬 개발 참조용(read-only)이며 `.gitignore` 처리되어 커밋되지 않습니다. 네이티브 화면 구현 시 먼저 `the_bible_project/`의 컨트롤러/DTO/API를 확인해 계약을 정확히 맞추세요.

## 문서

- [Product Specification](docs/android-app-spec.md) — 기능 요구사항, 웹 커버리지, OAuth 전략
- [Architecture Design](docs/android-app-architecture.md) — 기술 설계, 코드 구조, 보안

## 라이선스

Private — All rights reserved.
