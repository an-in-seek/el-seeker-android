# ElSeeker Android App - Product Specification

## 1. Overview

ElSeeker Android 앱은 기존 웹 서비스(elseeker.com)를 WebView로 감싸 Google Play Store에서 배포하는 네이티브 앱이다. 웹의 모든 기능을 그대로 제공하면서, 푸시 알림/딥링크/오프라인 처리 등 네이티브 이점을 추가한다.

### 1.1 Goals

- 웹 서비스의 모든 기능을 앱에서 동일하게 제공
- 네이티브 앱 경험 (스플래시, 오프라인 처리, 하드웨어 뒤로가기)
- Google Play Store 배포를 통한 접근성 향상
- 향후 FCM 푸시 알림, 딥링크 공유 등 네이티브 기능 확장 기반 마련

### 1.2 Non-Goals (MVP)

- 오프라인 성경 읽기 (Phase 2)
- FCM 푸시 알림 (Phase 3)
- 네이티브 UI 요소 (bottom nav, 위젯 등)
- iOS 앱

---

## 2. Target Users

- 기존 ElSeeker 웹 사용자 중 모바일 앱 선호 사용자
- 성경 학습/읽기를 일상적으로 하는 Android 사용자
- Google Play Store 검색을 통한 신규 사용자

---

## 3. Functional Requirements

### 3.1 Core Features (MVP - Phase 1)

| ID | Feature | Description | Priority |
|----|---------|-------------|----------|
| F-01 | WebView 렌더링 | 모든 웹 페이지를 WebView에서 정상 렌더링 | Must |
| F-02 | OAuth2 소셜 로그인 | Google, Naver, Kakao 로그인이 WebView에서 동작 | Must |
| F-03 | JWT 쿠키 인증 유지 | 앱 종료 후 재실행 시 로그인 상태 유지 (Refresh Token 14일) | Must |
| F-04 | 스플래시 스크린 | Android 12+ SplashScreen API 기반 앱 시작 화면 | Must |
| F-05 | 네트워크 오류 처리 | 오프라인/서버 오류 시 네이티브 에러 화면 + 재시도 | Must |
| F-06 | 하드웨어 뒤로가기 | Android 뒤로가기 버튼으로 WebView 히스토리 탐색 | Must |
| F-07 | 외부 링크 처리 | YouTube 등 외부 링크는 시스템 브라우저/앱으로 이동 | Must |
| F-08 | In-App Update | Google Play In-App Update API로 앱 업데이트 유도 | Should |

### 3.2 Phase 2 Features

| ID | Feature | Description |
|----|---------|-------------|
| F-09 | 딥링크 / App Links | 공유 URL 클릭 시 앱에서 직접 열기 |
| F-10 | 오프라인 캐시 | 성경 본문 캐시로 오프라인 읽기 지원 |
| F-11 | 네이티브 공유 | `navigator.share()` 미지원 시 JS Bridge로 Android Share Intent 호출 |

### 3.3 Phase 3 Features

| ID | Feature | Description |
|----|---------|-------------|
| F-12 | FCM 푸시 알림 | 커뮤니티 댓글, 읽기 리마인더 등 서버 푸시 |
| F-13 | 로컬 알림 | 매일 성경 읽기 리마인더 |
| F-14 | 홈 위젯 | 오늘의 말씀 위젯 |

---

## 4. Non-Functional Requirements

| Category | Requirement |
|----------|-------------|
| Min SDK | API 26 (Android 8.0, 시장 점유율 95%+) |
| Target SDK | API 35 (Play Store 현행 요구사항) |
| 언어 | Kotlin |
| 앱 크기 | APK 10MB 이하 (웹 콘텐츠는 서버에서 로드) |
| 시작 시간 | Cold start 2초 이내 (WebView 첫 렌더링 제외) |
| 인증서 | HTTPS 필수 (프로덕션) |
| 데이터 안전 | OAuth 이메일, 프로필 정보 수집 고지 |

---

## 5. Web Features Coverage

### 5.1 Page Routes

앱에서 접근 가능한 모든 웹 페이지:

**공개 페이지:**

| Section | Routes |
|---------|--------|
| 홈 | `/` |
| 성경 | `/web/bible/translation`, `/web/bible/book`, `/web/bible/book/description`, `/web/bible/chapter`, `/web/bible/verse`, `/web/bible/search` |
| 학습 | `/web/study`, `/web/study/bible-overview-video`, `/web/study/bible-genealogy`, `/web/study/twelve-tribes`, `/web/study/twelve-disciples`, `/web/study/lords-prayer`, `/web/study/apostles-creed`, `/web/study/creation`, `/web/study/ten-commandments`, `/web/study/dictionary`, `/web/study/dictionary/{id}`, `/web/study/history`, `/web/study/history/{era}`, `/web/study/history/event/{id}` |
| 커뮤니티 | `/web/community`, `/web/community/{postId}` |
| 게임 | `/web/game`, `/web/game/ranking` |
| 기타 | `/web/legal/terms`, `/web/legal/privacy` |

**인증 필요 페이지:**

| Section | Routes |
|---------|--------|
| 게임 (하위) | `/web/game/bible-quiz`, `/web/game/bible-typing`, `/web/game/bible-ox-quiz`, `/web/game/bible-casting-lots`, `/web/game/bible-word-puzzle` 등 |
| 커뮤니티 작성 | `/web/community/write` |
| 마이페이지 | `/web/member/mypage`, `/web/member/withdraw` |

### 5.2 Navigation Patterns

웹에서 이미 구현된 모바일 네비게이션을 그대로 사용:

- **Bottom Tab Bar** (< 600px): 성경, 학습, 홈, 게임, 커뮤니티 5개 탭
- **Navigation Rail** (600-991px): 좌측 세로 네비게이션
- **Top Nav**: 뒤로가기, 홈, 번역본 선택, 검색, 계정 메뉴
- **스크롤 Auto-hide**: 스크롤 시 상단/하단 네비 자동 숨김

### 5.3 Touch Interactions

| Feature | 현재 Web 구현 | WebView 호환성 |
|---------|---------------|----------------|
| 성경 절 복사/공유 | `navigator.share()` + clipboard fallback | `navigator.share()` WebView 미지원 가능 -> Phase 2에서 JS Bridge 대응 |
| 게임 (타이핑/퀴즈/퍼즐) | 터치 이벤트 기반 | 호환 |
| 스크롤 기반 auto-hide | scroll 이벤트 + transform | 호환 |
| 터치 피드백 | `scale(0.95)` + `opacity` | 호환 |

---

## 6. OAuth2 Authentication Strategy

### 6.1 Current Web Flow

```
사용자 -> /oauth2/authorization/{provider}
       -> OAuth 제공자 로그인 페이지
       -> /login/oauth2/code/{provider} (서버 콜백)
       -> OAuth2LoginSuccessHandler: JWT 쿠키 설정
       -> / (홈 리다이렉트)
```

### 6.2 WebView App Flow (MVP)

**Kakao, Naver**: WebView 내부에서 OAuth 플로우 전체 처리
- 서버 코드 변경 없음
- 쿠키가 동일 CookieManager에 즉시 반영

**Google**: Embedded WebView OAuth 차단 정책 대응 필요
- Option A: Chrome Custom Tabs로 분리 (서버에 auth code exchange 필요)
- Option B: `OAuth2LoginSuccessHandler`의 모바일 분기 구현 (TODO 주석 참조)
- Option C: MVP에서는 Google 로그인을 Custom Tabs로 열고, 성공 후 딥링크로 복귀

### 6.3 Cookie Management

| Cookie | TTL | WebView 동작 |
|--------|-----|--------------|
| Access Token | 1시간 | CookieManager 자동 관리, `JwtRefreshFilter`가 서버 사이드 자동 갱신 |
| Refresh Token | 14일 | CookieManager 자동 관리, 앱 종료 후에도 유지 |

---

## 7. URL Routing Policy

WebView의 `shouldOverrideUrlLoading()`에서 URL을 분류하여 처리:

| URL Pattern | Action | Reason |
|-------------|--------|--------|
| `{BASE_URL}/*` | WebView 내부 로드 | ElSeeker 내부 페이지 |
| `kauth.kakao.com`, `accounts.kakao.com` | WebView 내부 로드 | Kakao OAuth |
| `nid.naver.com` | WebView 내부 로드 | Naver OAuth |
| `accounts.google.com` | Chrome Custom Tabs | Google embedded WebView 차단 정책 |
| `youtube.com`, `youtu.be` | YouTube 앱 Intent | 66개 성경 개요 영상 |
| 기타 외부 URL | 시스템 브라우저 | 알 수 없는 외부 링크 |

---

## 8. Error Handling

### 8.1 Error Scenarios

| Scenario | Detection | UX |
|----------|-----------|-----|
| 네트워크 없음 | `ConnectivityManager` | Compose 오프라인 화면 + 재시도 버튼 |
| 서버 연결 실패 | `onReceivedError` (ERROR_CONNECT, ERROR_TIMEOUT) | Compose 에러 화면 + 재시도 (실패 URL 저장) |
| DNS 실패 | `onReceivedError` (ERROR_HOST_LOOKUP) | Compose 에러 화면 |
| HTTP 5xx | `onReceivedHttpError` | 서버의 에러 페이지를 WebView에서 표시 |
| HTTP 401 | `JwtRefreshFilter` 자동 처리 | 실패 시 로그인 페이지로 리다이렉트 |
| SSL 오류 | `onReceivedSslError` | 항상 cancel, 보안 경고 화면 표시 |

### 8.2 Recovery Strategy

```
네트워크 오류 발생
  -> 네이티브 에러 화면 표시 (Compose)
  -> 실패한 URL 저장
  -> 네트워크 복구 감지 (ConnectivityManager callback)
  -> "다시 시도" 버튼 또는 자동 재로드
  -> 저장된 URL로 WebView.loadUrl() 재시도
```

---

## 9. Release Roadmap

### Phase 1: MVP (2-3주)

- [ ] Android 프로젝트 셋업 (Kotlin, Compose, Gradle)
- [ ] Single Activity + WebView 기본 구현
- [ ] OAuth2 로그인 처리 (Kakao/Naver WebView 내부, Google Custom Tabs)
- [ ] 스플래시 스크린 (Android 12+ SplashScreen API)
- [ ] 네트워크 오류/오프라인 처리
- [ ] 하드웨어 뒤로가기 처리
- [ ] 외부 링크 라우팅 (YouTube -> 앱, 기타 -> 브라우저)
- [ ] In-App Update 기본 구현
- [ ] 내부 테스트 (QA)

### Phase 2: Enhanced Experience (2주)

- [ ] Android App Links (딥링크) + `assetlinks.json` 서버 배포
- [ ] 네이티브 공유 JS Bridge (`@JavascriptInterface`)
- [ ] 성경 본문 오프라인 캐시 (WebView cache + 서버 Cache-Control)
- [ ] Google Play Store 배포 준비 (메타데이터, 스크린샷, 개인정보처리방침)
- [ ] Play Store 첫 배포

### Phase 3: Native Features (2주)

- [ ] FCM 푸시 알림 (서버 Firebase Admin SDK + 앱 FCM)
- [ ] 서버: FCM 토큰 저장 API (`POST /api/v1/members/fcm-token`)
- [ ] 서버: 알림 발송 서비스 (커뮤니티 댓글, 랭킹 변동)
- [ ] 로컬 알림 (읽기 리마인더)

### Phase 4: Future (선택)

- [ ] 홈 위젯 (오늘의 말씀)
- [ ] Wear OS 대응
- [ ] 다크 모드 네이티브 셸 연동

---

## 10. Google Play Store Compliance

### 10.1 Minimum Functionality Policy

Google Play는 단순 WebView 래퍼를 거부할 수 있다. 다음 네이티브 차별화 기능으로 정책 준수:

1. **스플래시 스크린** - 웹 브라우저에 없는 네이티브 앱 경험
2. **오프라인 에러 처리** - 네이티브 에러 화면 + 자동 복구
3. **FCM 푸시 알림** (Phase 3) - 웹에서 제공하지 않는 기능
4. **딥링크** (Phase 2) - 공유 URL로 앱 직접 열기
5. **In-App Update** - 자동 업데이트 유도

### 10.2 Store Metadata

| Field | Value |
|-------|-------|
| App Name | ElSeeker - 성경 플랫폼 |
| Category | Books & Reference |
| Content Rating | Everyone |
| Privacy Policy | `https://elseeker.com/web/legal/privacy` |
| Data Safety | OAuth 이메일/프로필 수집, JWT 쿠키 사용 |

### 10.3 Required Server Changes

| Item | Description | Phase |
|------|-------------|-------|
| `/.well-known/assetlinks.json` | App Links 인증 파일 | Phase 2 |
| `POST /api/v1/members/fcm-token` | FCM 토큰 저장 API | Phase 3 |
| Firebase Admin SDK | 서버 사이드 푸시 발송 | Phase 3 |
| `User-Agent` 식별 | WebView 앱 요청 구분 (`ElSeeker-Android/x.x`) | Phase 1 |

---

## 11. Known Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Google embedded WebView OAuth 차단 | Google 로그인 불가 | Chrome Custom Tabs로 분리 또는 서버 auth code exchange 구현 |
| `navigator.share()` WebView 미지원 | 성경 절/커뮤니티 공유 불가 | Phase 2에서 `@JavascriptInterface` 브릿지 구현 |
| WebView `sessionStorage` OAuth 리다이렉트 후 초기화 | OAuth 복귀 URL 유실 | 서버 사이드 `returnUrl` 파라미터로 대체 (이미 구현됨) |
| Play Store WebView 래퍼 거부 | 배포 불가 | FCM, 딥링크 등 네이티브 기능 추가로 차별화 |
| `env(safe-area-inset-bottom)` Android 미지원 | Bottom tab 레이아웃 깨짐 | Android WindowInsets로 padding 주입 또는 CSS fallback |
| OAuth 중 앱 killed (메모리 부족) | WebView 상태 유실 | `onSaveInstanceState` WebView 상태 저장 |

---

## 12. Open Questions

- [ ] Google OAuth 처리 전략 최종 확정 (Custom Tabs vs 서버 auth code exchange)
- [ ] 앱 아이콘/브랜딩 디자인
- [ ] Play Store 스크린샷/프로모션 그래픽 제작
- [ ] 성경 본문 오프라인 캐시 범위 (전체 KRV 66권 vs 최근 읽은 권만)
- [ ] FCM 알림 카테고리 및 사용자 설정 범위
