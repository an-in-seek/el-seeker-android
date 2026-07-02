# PRD: ElSeeker Android 네이티브 앱

## 0. 문서 상태 및 정책 게이트

상태: **기획 단계 (구현 전)**

작성 기준일: **2026-06-30**

본 문서는 기존 ElSeeker 웹 서비스(Spring Boot 3.5 + Thymeleaf SSR)를 **Android Studio 기반 네이티브 앱**으로 출시하기 위한 제품 요구사항을 정의한다.

### 0.1 공식 정책 참조

* Google Play 개발자 정책: `https://support.google.com/googleplay/android-developer/answer/10281818`
* Play 앱 품질 가이드: `https://developer.android.com/quality`
* 데이터 보안 섹션(Play Console Data Safety): 개인정보/소셜로그인/네트워크 사용 고지 필수

### 0.2 배포 전 필수 확인

* 패키지명은 기존 합의값 `com.elseeker.android`를 사용한다 (`docs/googleplay/app-install-banner-prd.md` 참조).
* 소셜 로그인 SDK(Google/Kakao/Naver) 각 콘솔에 **Android 앱 패키지명 + 키 해시/SHA-1**을 등록해야 한다.
* 후원/외부결제 관련 기능은 `docs/support/donation-prd.md`의 정책 게이트를 그대로 따른다. 네이티브 앱에서 후원 진입점 노출 여부는 심사 리스크 검토 후 결정한다.
* Play Console **Data Safety** 양식에 수집 데이터(이메일, 닉네임, 사용 기록, 기기 식별자)와 용도를 정확히 신고한다.
* 만 14세 미만 대상 여부 및 콘텐츠 등급(IARC) 설문을 완료한다.

### 0.3 ⚠️ 이 문서를 읽는 Android 프로젝트의 Claude Code에게 (필독)

본 PRD는 백엔드 레포(ElSeeker, Spring Boot)에서 작성되었고, **Android 프로젝트는 이 백엔드 레포를 심볼릭 링크로 연결**해 둔다. 따라서:

* **백엔드 소스에 심링크로 접근 가능.** Android 프로젝트 루트의 심링크 **`the_bible_project/`**(§0.4)를 통해 백엔드 레포 전체를 읽을 수 있다. 문서 내 `src/main/kotlin/...`, `src/main/resources/.../*.js`, `*.html`, `*.css`, `docs/...` 경로는 **백엔드 레포 루트 기준**이며, Android에서는 앞에 `the_bible_project/` 를 붙여 접근한다.
* **심링크는 읽기 참조 전용.** 백엔드 소스를 수정하지 말 것 — 앱 구현은 Android 프로젝트 트리 안에서 한다. 정적 콘텐츠는 심링크에서 **데이터/텍스트만 읽어** 네이티브 리소스(`assets`/Compose)로 옮긴다. 렌더링 로직(JS/Thymeleaf)은 이식하지 않고 네이티브로 재구현(WebView 미사용, 4-A).
* **API 계약 정본 = 실행 중인 백엔드 Swagger/OpenAPI.** `{EL_SEEKER_API_BASE_URL}/swagger-ui/index.html`, `/v3/api-docs`. 심링크로 `*ApiDocument` 인터페이스/컨트롤러를 직접 읽어 교차 확인도 가능. 본 문서의 엔드포인트 목록은 요약이며, 필드 단위 계약은 Swagger를 정본으로 삼는다.
* **부록 B** = 심링크로 접근할 자산의 위치 안내(물리 복사가 아니라 참조 경로).

### 0.4 심볼릭 링크 설정

* 심링크 이름/위치: Android 프로젝트 루트의 **`the_bible_project/`** → `/mnt/c/workspace/java/the_bible_project` (백엔드 레포 루트). 본 문서의 백엔드 경로는 Android에서 `the_bible_project/` 를 prefix로 붙여 접근한다. (예: `the_bible_project/src/main/resources/static/js/study/bible-genealogy.js`)
* 심링크는 **로컬 개발 참조용(read-only)** 이며 `.gitignore` 처리되어 커밋되지 않는다. API 계약·OAuth/약관 동의 흐름·페이지 URL 확인 시 레퍼런스로만 활용하고, 백엔드 소스를 수정하지 않는다.
* **❗정합 주의 (전부 네이티브 확정, 2026-06-30):** Android 프로젝트의 README/CLAUDE.md 등에 "WebView가 웹 페이지를 로드한다 / 앱이 웹을 감싼다(하이브리드)"라고 기술돼 있다면 **구식 표현이다.** 본 PRD `§4-A`(v1 전부 네이티브, WebView 미사용)가 정본. 심링크된 웹/백엔드는 **WebView 로드 대상이 아니라**, API 계약·OAuth/동의 흐름·페이지 URL·정적 콘텐츠 데이터를 확인하는 **read-only 레퍼런스**일 뿐이다. 충돌 시 PRD를 따르고, 프로젝트 README를 네이티브 기준으로 정정한다.
* 심링크가 끊긴 환경(CI, 다른 머신 클론 등)에서는 백엔드 소스에 접근하지 못한다. 이 경우 백엔드 레포를 직접 클론/참조해 부록 B의 자산을 확인한다.

---

## 1. 목적 및 배경

### 1.1 목적

기독교 성경 학습 플랫폼 'ElSeeker'를 Android 사용자가 앱으로 설치·이용할 수 있도록, **Android Studio / Kotlin 기반 네이티브 앱**으로 출시한다.

### 1.2 배경

현재 ElSeeker는 다음 형태로 운영 중이다.

* 서버 렌더링: Spring Boot 3.5 + Thymeleaf (대부분 페이지가 SSR)
* 부분 SPA/리치 인터랙션: Three.js 우주 배경, 게임(타이핑/퀴즈/낱말퍼즐), 정적 JS 데이터 페이지(족보, 성경 개요 영상)
* REST API: 인증, 성경 읽기/메모/하이라이트/진도, 게임, 커뮤니티, 마이페이지, 사전 등 클라이언트 API가 이미 다수 존재
* 모바일 연동 사전작업: `POST /api/v1/auth/social-login` 모바일 소셜 로그인 API와 가이드(`docs/mobile/social-login-api.md`)가 이미 마련됨

즉, 백엔드 API 자산과 모바일 인증 플로우는 일부 갖춰져 있으나, 화면의 상당수는 SSR/JS에 종속되어 있다. 이 PRD는 그 현실 위에서 네이티브화 전략을 정의한다.

---

## 2. 핵심 원칙

* 웹과 앱은 **동일한 백엔드**(`{EL_SEEKER_API_BASE_URL}`)와 동일한 계정 체계를 공유한다. 앱 전용 데이터 사일로를 만들지 않는다.
* 사용자 경험은 네이티브 표준(머티리얼 디자인, 시스템 뒤로가기, 다크모드, 접근성)을 따른다.
* **v1은 전부 네이티브로 구현하고 WebView를 사용하지 않는다**(§4-A 확정). 웹은 동일 백엔드를 공유하는 별개 클라이언트이며, 앱은 REST API 또는 정적 데이터 번들로 동일 데이터를 네이티브 렌더링한다.
* 기존 웹 기능 패리티를 1차 목표로 하고, 앱 고유 가치(푸시, 오프라인, 위젯)는 후속 단계에서 추가한다.
* 게임·커뮤니티는 2차로 단계적 확장한다(§4-A.3).

---

## 3. 아키텍처 결정 (의사결정 기록)

> **정본은 §4-A.** 본 장은 결정 배경만 짧게 남긴다. v1 범위·근거·완료 기준은 §4-A를 따른다.

**최종 결정(2026-06-30): v1은 전부 네이티브, WebView 미사용.** 초기 검토한 3안 중 A를 채택했다.

| 방식 | v1 판단 |
|------|---------|
| **A. 순수 네이티브** (Compose + REST API/정적 번들) | **채택** — 범위를 네이티브화 가능한 화면(성경·학습·마이·지원)으로 한정 |
| B. 순수 WebView 래퍼 | 기각 — 단순 웹래퍼 리스크·UX 한계 |
| C. 네이티브 셸 + 선택적 WebView | v1 미사용 — WebView가 필요했던 게임·커뮤니티를 2차로 이월. 하이브리드는 2차에서 재검토 |

> 2차에서 하이브리드 화면을 도입할 경우의 브릿지 요건(WebView 토큰 주입/쿠키 동기화, 앱 UA 식별, Custom Tabs 위임, JS Bridge)은 그 시점에 별도 설계한다. v1에는 해당 없음.

---

## 4. 범위 — 기능 매핑

기존 웹 화면을 Android v1/2차 범위로 재분류한다. 표의 "v1"은 **전부 네이티브 구현**을 의미한다. WebView 화면은 v1에 없다.

### 4.1 성경 (bible)

| 화면 | API | v1 범위 |
|------|-----|---------|
| 번역본 목록 | `GET /api/v1/bibles/translations` | 포함 |
| 책 목록 | `GET /api/v1/bibles/translations/{translationId}/books` | 포함 |
| 책 개요 | `GET /api/v1/bibles/translations/{translationId}/books/{bookOrder}` (`description` 포함) | 포함 |
| 장 목록 | `GET /api/v1/bibles/translations/{translationId}/books/{bookOrder}/chapters` | 포함 |
| 절 본문/장 이동 | `GET .../chapters/{chapterNumber}/verses`, `GET .../navigate?direction=PREV\|NEXT` | 포함. ⚠️ `direction`은 **대문자** `PREV`/`NEXT`(서버 `DirectionType` enum, 커스텀 컨버터 없음 → 소문자는 400) |
| 장 상태(메모·하이라이트·읽음) | `GET .../chapters/{chapterNumber}/state` | 포함 |
| 절 검색 | `GET /api/v1/bibles/translations/{translationId}/search`; 인기 검색어는 `GET /api/v1/bibles/search-keywords/ranking` | 포함 |
| 하이라이트 | `GET .../highlights`, `PUT/DELETE .../verses/{verseNumber}/highlight` | 포함 |
| 메모(절/장/책) | `BibleMemoApi`, `BibleChapterMemoApi`, `BibleBookMemoApi` | 포함 |
| 읽기 진도 | `POST/GET /api/v1/bible/reading/chapters/read` | 포함 |
| 내 메모 모아보기 | `BibleMyMemoApi`, `BibleMyChapterMemoApi`, `BibleMyBookMemoApi`, `BibleMyMemoCountsApi` | 포함 |

### 4.2 학습 (study) — 전부 v1 네이티브 (4-A.4-5 확정)

> **사전을 제외한** 학습 화면은 DB 의존 없는 정적 콘텐츠 → 정적 데이터를 앱 리소스로 번들해 네이티브 구현. 사전은 `DictionaryApi`/DB 기반이라 REST API로 연동한다. 둘 다 WebView 미사용.

| 화면 | v1 범위 | 비고 |
|------|---------|------|
| 사전 목록/상세/참조/검색 | 포함 | `DictionaryApi`, `DictionarySearchKeywordApi` 존재 |
| 성경 개요 영상 | 포함 | `bible-overview-video.js`의 66권 YouTube 데이터 → 네이티브 그리드/검색/외부 YouTube |
| 족보(genealogy) | 포함 | `bible-genealogy.js`의 마태 61명·누가 78명 → 세로 타임라인 |
| 십계명/사도신경/주기도문/창조/성주간/12제자/12지파/공동체 성경읽기/성경 주석 | 포함 | 정적 콘텐츠 9종 → 번들 텍스트/이미지/링크 |
| 성경 역사(연대/사건/상세) | 포함 | `HistoryDummyData` → JSON 번들 |
| 성경 논문/지도/강의 | 제외 | 웹에서도 "준비중" 카드. v1 빈 진입점 금지 |

### 4.3 게임 (game)

| 화면 | API | v1 범위 |
|------|-----|---------|
| 게임 허브/랭킹 | `GameRankingApi` | 제외 → 2차 |
| 성경 타이핑 | `BibleTypingSessionApi`, `BibleTypingVerseProgressApi`, `BibleTypingLookupApi` | 제외 → 2차 |
| 성경 퀴즈 / OX 퀴즈 | `BibleQuizApi`, `OxQuizApi` | 제외 → 2차 |
| 낱말 퍼즐 | `WordPuzzleApi` | 제외 → 2차 |
| 제비뽑기(casting-lots) | API 없음(웹 정적/클라이언트 로직) | 제외 → 2차 |

### 4.4 커뮤니티 / 마이페이지 / 지원

| 화면 | API | v1 범위 |
|------|-----|---------|
| 커뮤니티 목록/상세/작성/댓글/신고 | `CommunityApi` | 제외 → 2차 |
| 마이페이지/프로필 수정/연동 계정/탈퇴 | `GET /api/v1/auth/me`, `MemberApi`, `POST /api/v1/auth/social-login`(`intent=link`) | 포함 |
| 내 메모 모아보기 | `BibleMyMemoApi`, `BibleMyChapterMemoApi`, `BibleMyBookMemoApi`, `BibleMyMemoCountsApi` | 포함 |
| 공개 문의/내 문의 | `ContactApi`, `InquiryApi` | 포함 |
| 후원 안내 | - | 제외. 정책 게이트 통과 전 앱 내 진입점 비노출 |

### 4.5 공통

* 홈(메뉴 그리드 + 3D 우주 섹션): 메뉴는 N, 우주 배경 섹션은 **네이티브 경량 비주얼로 대체**(4-A.4-1). v1 WebView 미사용.
* 통합 검색: v1은 성경 절 검색 + 사전 검색만(N). 전 콘텐츠 통합 검색은 2차(4-A.4-4).

---

## 4-A. 1차 출시 패리티 기준선 (확정)

> 결정일: 2026-06-30. 미해결 질문 #1 해소. 본 절이 v1 범위의 **단일 기준(source of truth)** 이다.

### 4-A.1 결정 요약

| 항목 | 결정 | 근거 |
|------|------|------|
| 범위 톤 | **성경 + 학습 중심** (게임·커뮤니티만 2차) | 성경 읽기 앵커 + 학습 콘텐츠로 학습 가치 확보 |
| 학습 | **v1 포함** (2026-06-30 디렉티브) | 사전(API/DB)을 제외하면 정적 콘텐츠라 백엔드 신설 없이 네이티브 이식 가능 |
| 게임 | **2차 제외** | 인터랙션 재구현 비용 큼 |
| 커뮤니티 | **2차 제외** | UGC 신고/차단 등 운영 부담을 단계적으로 |
| 렌더링 | **v1 전부 네이티브 (WebView 미사용)** | 성경·마이·지원은 REST API 보유, 학습은 정적 데이터 번들 → 모두 네이티브 가능 |

이 조합은 정합적이다. 게임·커뮤니티만 2차로 미루며, 이 둘이 바로 WebView가 필요했을 화면들이다. v1에 남는 화면은 (1) 기존 클라이언트 API가 있거나(성경/사전/마이/지원) (2) DB 의존 없는 **정적 콘텐츠**(학습 본문·족보·개요·역사 — `HistoryDummyData` 포함)여서, 둘 다 네이티브로 구현 가능하다.

### 4-A.2 v1 포함 화면 (IN)

| 영역 | 화면 | 백엔드 API | 비고 |
|------|------|-----------|------|
| 인증 | 소셜 로그인(Google/Kakao/Naver) | `POST /api/v1/auth/social-login` | 네이티브 SDK. 응답 `consentRequired`에 따라 정식 토큰/동의 전용 토큰 분기 |
| 인증 | 약관 동의(Consent) | `POST /api/v1/auth/consent`, `POST /api/v1/auth/consent/cancel` | 신규 가입자는 signup token으로 동의 제출 후 정식 토큰 수령 |
| 인증 | 내 세션 확인/토큰 갱신 | `GET /api/v1/auth/me`, `POST /api/v1/auth/reissue` | Bearer 기반 |
| 성경 | 번역본/책/장/절 목록·본문 | `BibleApi` | 앵커 기능. 번역본 노출은 4-A.9 데이터 게이트 적용 |
| 성경 | 책 개요 | `GET /api/v1/bibles/translations/{translationId}/books/{bookOrder}` | `description` 필드 사용 |
| 성경 | 장 상태 | `BibleChapterViewApi` (`.../state`) | 메모·하이라이트·읽음 상태 통합 조회 |
| 성경 | 절 검색 | `BibleApi.searchBible` (`/api/v1/bibles/translations/{translationId}/search`) | 인기검색어는 `BibleSearchKeywordApi`(별개) |
| 성경 | 하이라이트 | `BibleHighlightApi` | 절 단위 색상 저장/삭제 |
| 성경 | 메모(절/장/책) | `BibleMemoApi`, `BibleChapterMemoApi`, `BibleBookMemoApi` | |
| 성경 | 읽기 진도 | `BibleReadingProgressApi` | `/api/v1/bible/reading` 단수형 base 주의 |
| 마이 | 마이페이지/프로필 수정 | `GET /api/v1/auth/me`, `PUT /api/v1/members/{memberUid}` | |
| 마이 | 소셜 계정 연동 관리 | `GET/DELETE /api/v1/members/{memberUid}/oauth-accounts`, `POST /api/v1/auth/social-login`(`intent=link`) | 연동 추가는 provider 토큰을 서버가 검증 |
| 마이 | 내 메모 모아보기 | `BibleMyMemoApi`, `BibleMyChapterMemoApi`, `BibleMyBookMemoApi`, `BibleMyMemoCountsApi` | 책/장/절 탭 및 카운트 |
| 마이 | 회원 탈퇴 | `DELETE /api/v1/members/{memberUid}` | Play 계정삭제 정책 충족 목적 포함 |
| 학습 | 성경 사전(목록/상세/참조/검색) | `DictionaryApi`, `DictionarySearchKeywordApi` | API 완비, 성경 읽기 보완 |
| 학습 | 학습 홈 + 정적 콘텐츠(십계명·사도신경·주기도문·창조·성주간·12제자·12지파·공동체성경읽기·주석) | - (정적) | 텍스트/이미지 → 네이티브 화면 + 번들 데이터 |
| 학습 | 성경 개요 영상 | - (정적 JS) | 영상 목록 네이티브화(썸네일 그리드+검색). 재생은 **YouTube 앱/Custom Tabs/Intent로 외부 위임**(자체 호스팅 아님 → ExoPlayer 인앱 재생 아님). 이식 공수 주의 |
| 학습 | 족보(genealogy) | - (정적 JS) | 세로 타임라인 네이티브 재구현(분기 트리 아님 — 4-A.7) — v1 최대 공수 항목 |
| 학습 | 성경 역사(연대/사건/상세) | - (`HistoryDummyData`, 코드 하드코딩) | 더미데이터 JSON 번들로 네이티브화 |
| 지원 | 1:1 문의(작성/내역) | `ContactApi`, `InquiryApi` | |
| 공통 | 홈/하단탭 네비게이션 | - | 4-A.4 참조 |
| 공통 | 스플래시/오류·오프라인/딥링크/뒤로가기 | - | 네이티브 기본기 |

### 4-A.3 v1 제외 → 2차 이월 (OUT)

* 게임 전체(타이핑/퀴즈/OX/낱말퍼즐/제비뽑기/랭킹)
* 커뮤니티 전체(목록/상세/작성)
* 학습 홈의 준비중 콘텐츠(성경 논문/지도/강의)
* 후원 안내(정책 게이트 별도)
* 푸시/오프라인 캐시/위젯 등 앱 고유 가치(6장 ◇)

> 학습(study)은 v1 포함으로 변경됨(2026-06-30 디렉티브). 단 정적 콘텐츠 이식이라 백엔드 작업은 없으나 화면 수가 많아 **v1 공수가 늘어난다**(특히 족보·개요영상). 일정 압박 시 족보/개요영상만 후순위 스프린트로 분리하는 것을 권장(범위는 v1 유지, 내부 순서만 조정).

### 4-A.4 미결 항목 처리 (확정, 2026-06-30)

전부 네이티브 결정에 따른 v1 처리 방식을 아래와 같이 확정한다.

1. **홈 3D 우주 섹션 → 확정: 네이티브 경량 비주얼로 대체.** Three.js를 WebView로 못 쓰므로 v1은 정적 이미지 또는 Compose 그라데이션/라이트 파티클로 브랜딩만 유지한다. Three.js 수준의 완전 재현은 2차(필요 시 OpenGL/Compose 고도화).
2. **홈 히어로 배너 → 확정: v1 앱 정적 리소스 구성, 2차 서버화.** 배너 운영이 필요해지는 시점에 서버 API로 전환한다.
3. **사전(Dictionary) → 확정: v1 포함.** `DictionaryApi`/검색 API가 완비되어 네이티브화가 저렴하고 성경 읽기를 직접 보완한다. (4-A.2 IN 표 반영)
4. **통합 검색 → 확정: 2차 이월.** v1은 성경 절 검색 + 사전 검색만 제공. 전 콘텐츠 통합 검색(게임/커뮤니티 포함)은 2차.
5. **학습(study) 전체 → 확정: v1 포함.** 사전을 제외한 학습 화면은 DB 의존 없는 정적 콘텐츠(템플릿 본문 + 정적 JS 배열 + `HistoryDummyData`)이므로 **백엔드 API 신설 없이** 텍스트/데이터를 앱 리소스(JSON/문자열)로 이식한다. 사전은 기존 `DictionaryApi`로 연동한다. WebView 미사용 원칙 유지. 족보 세로 타임라인과 개요 영상은 이식 공수가 크므로 v1 내 스프린트 순서를 후순위로 둘 수 있다.

### 4-A.5 v1 완료(Done) 기준

* 위 IN 화면 전부 네이티브로 동작하고 동일 계정으로 웹과 데이터가 일치한다.
* 소셜 3종 로그인·신규 가입 동의·로그아웃·토큰 갱신·회원탈퇴가 정상 동작한다.
* signup token으로 일반 API를 호출하면 서버가 `403 CONSENT_REQUIRED`를 반환하므로, 앱은 해당 응답을 동의 화면으로 라우팅한다.
* WebView를 사용하지 않는다(외부 링크는 Custom Tabs/외부 브라우저 위임 예외).
* 오프라인/네트워크 오류 시 빈손 크래시 없이 안내·재시도가 동작한다.
* Play 내부테스트 트랙 업로드 및 Data Safety/개인정보처리방침 충족.

### 4-A.6 백엔드 영향

* v1 화면은 기존 API로 커버되며 **신규 도메인 API는 원칙적으로 불필요**. 학습 콘텐츠는 사전(`DictionaryApi`/DB)을 제외하면 정적(템플릿/정적 JS/`HistoryDummyData`)이라 백엔드 추가 없이 앱 번들로 이식한다(단, 콘텐츠 원본을 앱 리소스로 추출하는 작업은 필요).
* **앱용 토큰 갱신 확인됨**: `POST /api/v1/auth/reissue` (바디 기반, permitAll) 이미 구현 → v1 차단 요인 아님(5.2 참조).
* **회원탈퇴/프로필 API의 Bearer 호출 가능 확인됨**: `MemberApi`는 `@AuthenticationPrincipal JwtPrincipal` 기반이며, `SecurityConfig`에서 `/api/v1/members/**` 인증 경로로 처리한다.
* **프로필 조회 필드 확인됨**: `GET /api/v1/auth/me`는 `memberUid`, `email`, `role`, `nickname`, `profileImageUrl`, `provider`, `status`, `createdAt`을 반환한다. 다중 OAuth 계정 상세는 `GET /api/v1/members/{memberUid}/oauth-accounts`를 별도로 호출한다.

### 4-A.7 족보·개요영상 이식 공수 및 스프린트 분리안

학습 화면 중 공수가 큰 두 항목(족보·개요영상)을 별도 산정하고, v1 범위는 유지하되 내부 스프린트를 분리해 MVP 본류를 막지 않도록 한다.

#### 소스 분석 요약

| 항목 | 소스 | 데이터 | 핵심 인터랙션 | 구조적 난도 |
|------|------|--------|---------------|-------------|
| 개요영상 | `bible-overview-video.js`(265L)/css(189L) | 66권 `{bookOrder, bookName, youtubeUrl}` | 검색 필터, 카드 탭→외부 YouTube, 딥링크 스포트라이트 | **낮음** — OT/NT 그리드 + 검색 |
| 족보 | `bible-genealogy.js`(395L)/css(212L) | 마태 61명·누가 78명(섹션별) | 탭(마태/누가), 섹션 접기/펼치기, 스크롤투탑 | **중간** — 단, **선형 타임라인**(분기 트리 아님 → 레이아웃 엔진 불필요) |

> 핵심: 족보의 `parentId`/`generation`은 데이터에만 존재하고 화면은 단일 혈통 **세로 타임라인**이다. 그래프/트리 레이아웃을 구현할 필요가 없어 공수가 크게 줄어든다. 과설계 금지.

#### 공수 산정 (Compose 1인, ideal engineer-day 기준 / M0 테마·네비 완비 가정 / 데이터는 정적 번들)

**개요영상 — 합계 ~2.5~3.5d**

| 작업 | 추정 |
|------|------|
| 데이터 번들(66권 JSON+모델) + videoId 추출/썸네일 URL | 0.4d |
| `LazyVerticalGrid` OT/NT 2섹션 + 카드(Coil 썸네일·재생아이콘·준비중 배지) | 1.0d |
| 검색 필터 바 + 빈 상태 | 0.5d |
| 카드 탭→YouTube(Custom Tabs/Intent) + `bookOrder` 딥링크 진입 | 0.4d |
| 다크모드/반응형/QA | 0.4d |
| (선택) 딥링크 스포트라이트 효과 | +0.5d |

**족보 — 합계 ~4.5~5.5d**

| 작업 | 추정 |
|------|------|
| 데이터 번들(마태·누가 ~139레코드, 섹션 구조) + 모델 | 0.5d |
| 탭(마태/누가) + 상태 전환 | 0.25d |
| 세로 타임라인 레이아웃 + 커넥터 데코 | 1.5d |
| 노드 컴포저블(이름/주석/하이라이트·기점·종점 변형) | 0.75d |
| 섹션 접기/펼치기("N명 더 보기"/접기) + 애니메이션 | 0.75d |
| 스크롤투탑 FAB + 스크롤 거동 | 0.25d |
| 다크모드/접근성(라벨)/QA | 0.75d |

**두 항목 합계: ~7~9 dev-day (1인 기준 약 1.5~2주, 리뷰/QA 포함).**

#### 스프린트 분리안

* **S1 (학습 코어)** — 사전 + 정적 텍스트 콘텐츠(십계명·사도신경·주기도문·창조·성주간·12제자·12지파·공동체성경읽기·주석) + 성경역사. 저난도·고가치. **MVP 본류와 함께 진행.**
* **S2 (학습 리치, 분리)** — **족보 + 개요영상**. 공수 큰 두 항목만 묶어 M1 후반 별도 트랙으로. 다음 규칙을 적용:
  1. **범위는 v1 유지** — 잘라내지 않는다. 단 내부 순서상 마지막에 배치.
  2. **피처 플래그 + 데이터 전용 번들** — 백엔드 무관하므로 본류 릴리스와 독립적으로 토글/배포 가능.
  3. **일정 게이트** — v1 출시일이 압박되면 S2를 `v1.0.x` 패스트팔로우로 분리 출시한다. 그동안 학습 메뉴의 해당 항목은 "준비중" 처리(빈 진입점 금지).
  4. **착수 우선순위** — 둘 중 **개요영상 먼저**(저난도·빠른 가치) → 족보. 개요영상에서 그리드/검색/딥링크 패턴을 먼저 확립해 재사용.
* **권장** — S2는 개요영상(2.5~3.5d) → 족보(4.5~5.5d) 순. 스포트라이트 효과는 1차에서 생략 가능(폴리시 후속).

### 4-A.8 v1 전체 일정 합산

**산정 전제**: 중급 Android/Compose 개발자 **1인**, ideal engineer-day. 디자인 핸드오프 가용, 정적 데이터 추출 포함, 백엔드 변경 없음(불필요), **앱스토어 심사 대기 시간(외부, 1~7일)과 별개**.

| # | 영역 | 작업 묶음 | dev-day |
|---|------|-----------|---------|
| 1 | 기반(M0) | 프로젝트/빌드 variant/서명·CI | 1.5 |
| | | 테마(Material3·다크)·공통 컴포넌트 | 2.5 |
| | | 네트워크 레이어(Retrofit/에러매퍼/헤더) | 1.5 |
| | | 인증 기반(토큰 저장·reissue Authenticator·세션) | 1.5 |
| | | 소셜 로그인 3종(Google/Kakao/Naver) + API 연동 | 3.0 |
| | | 약관 동의 플로우 + pending 라우팅 | 1.0 |
| | | 로그아웃/세션 폐기 | 0.5 |
| | **소계** | | **11.5** |
| 2 | 공통/홈 | 앱 셸·하단탭·네비·시스템 뒤로가기 | 1.5 |
| | | 홈(메뉴·히어로 정적·경량 우주 비주얼) | 2.0 |
| | | 딥링크(App Links) | 1.0 |
| | | 오류·오프라인 공통 처리 | 1.0 |
| | | 스플래시·아이콘·브랜딩 | 0.5 |
| | **소계** | | **6.0** |
| 3 | 성경(앵커) | 번역본/책/장 목록 | 1.75 |
| | | 절 목록·본문 뷰어(선택·폰트·스크롤) | 2.5 |
| | | 절 검색(+검색어 랭킹) | 1.5 |
| | | 하이라이트(선택→색·API·표시) | 2.0 |
| | | 메모(절/장/책 CRUD·입력 시트) | 2.5 |
| | | 읽기 진도(기록·표시) | 1.5 |
| | **소계** | | **11.75** |
| 4 | 학습 | 학습 홈 | 0.5 |
| | | 사전(목록/검색/상세/참조·API) | 2.5 |
| | | 정적 콘텐츠 9종(공통 렌더러 + 데이터 추출) | 4.0 |
| | | 성경역사(`HistoryDummyData`→JSON 번들) | 2.0 |
| | | **S2** 개요영상(4-A.7) | 3.0 |
| | | **S2** 족보(4-A.7) | 5.0 |
| | **소계** | | **17.0** |
| 5 | 마이 | 마이페이지·내 메모 모아보기·회원탈퇴 | 4.0 |
| 6 | 지원 | 1:1 문의(작성·내역·API) | 2.0 |
| 7 | 출시/QA | Play 셋업·Data Safety·등급, 배포 트랙 | 2.5 |
| | | 통합 QA(기기 매트릭스·회귀) | 3.0 |
| | | 크래시/분석 연동 | 1.5 |
| | **소계** | | **7.0** |
| | **원합계** | | **≈ 59.25** |
| | 리스크 버퍼(+18%) | | **≈ 10.5** |
| | **총합(버퍼 포함)** | | **≈ 70 dev-day** |

#### 캘린더 환산

| 구성 | 기간(버퍼 포함) | 비고 |
|------|------------------|------|
| **1인** | 약 **13~15주(≈ 3~3.5개월)** | 단일 개발자 직렬 진행 |
| **2인 병렬** | 약 **7~8주(≈ 2개월)** | 조율 오버헤드·직렬 의존 반영(효율 ~85%) |

#### 임계 경로 / 분리 포인트

* **선행(차단) 작업**: 기반(M0)의 네트워크·인증·테마가 모든 기능 화면을 막는 임계 경로. 최우선 착수.
* **앵커**: 성경 읽기(영역 3)는 핵심 가치 → M0 직후 두 번째 우선.
* **분리 가능(S2)**: 족보+개요영상 = **8 dev-day**. 일정 압박 시 빼면 **코어 v1 ≈ 51 dev-day(버퍼 포함 ≈ 60)** 로 단축, S2는 `v1.0.x` 패스트팔로우(4-A.7 규칙).
* **2인 분담 예시**: Dev A = 기반→성경→마이, Dev B = (기반 일부 후) 학습 코어→지원→S2. 출시/QA는 막바지 공동.

#### 변동 요인(추정 밖)

* 디자인을 웹과 픽셀 일치까지 요구하거나 정적 콘텐츠 9종이 리치 레이아웃(표·이미지 다수)이면 영역 4가 +2~4d.
* 소셜 3종 콘솔 등록/키 해시 이슈에서 지연 가능.
* 푸시·오프라인·위젯은 v1 제외(2차)이므로 본 합산에 미포함.

### 4-A.9 성경 번역본 데이터 게이트

현재 코드 기준 `BibleReader.getTranslations()`는 `KRV`, `NKRV`, `KJV`를 반환한다. 다만 웹 컨트롤러는 비관리자에게 `NKRV`를 숨기고 있으며, 로컬 seed 기준 본문 데이터 완성도도 번역본별로 다르다.

| 번역본 | API 목록 노출 | seed 상태(레포 기준) | 앱 v1 처리 |
|--------|---------------|----------------------|------------|
| KRV | 노출 | 66권 본문 seed 존재 | 기본 노출 |
| NKRV | API에는 포함, 웹 비관리자 숨김 | 일부 책만 seed 존재 | 기본 비노출. 백엔드 visibility 정책 확정 전까지 앱도 숨김 |
| KJV | API에는 포함 | 책 메타데이터 seed만 확인, 본문 seed 미확인 | 장/절 본문 smoke test 통과 전 비노출 |

앱은 번역본 목록을 그대로 렌더링하지 말고, v1에서는 **본문 조회가 가능한 번역본만 노출**한다. 이상적인 후속 개선은 백엔드에 `visibleToClient`/`contentReady` 같은 공개 플래그를 추가하는 것이지만, v1에서 백엔드 변경 없이 가려면 앱이 허용 목록(`KRV`) 또는 런타임 smoke test(`books`→`chapters`→`verses`)로 보호한다.

---

## 5. 인증 (Auth)

### 5.1 방식

`docs/mobile/social-login-api.md`의 모바일 플로우를 참고하되, 필드 단위 계약은 실행 중인 Swagger/OpenAPI와 본 절을 따른다.

1. 앱에서 네이티브 소셜 SDK로 토큰 획득
   * Google: ID Token (Credential Manager / Google Sign-In)
   * Kakao: Access Token (Kakao SDK)
   * Naver: Access Token (Naver SDK)
2. `POST /api/v1/auth/social-login { provider, token, intent? }` 호출
3. 응답의 `consentRequired`로 분기
   * `false`: `accessToken`, `refreshToken`이 정식 토큰 → 저장 후 서비스 진입
   * `true`: `accessToken`은 동의 전용 signup token, `refreshToken`은 `null` → 동의 화면으로 이동
4. 정식 토큰 발급 후 API 호출은 `Authorization: Bearer {accessToken}`

#### 5.1.1 `POST /api/v1/auth/social-login` 응답 계약

```json
{
  "consentRequired": false,
  "accessToken": "...",
  "refreshToken": "..."
}
```

신규 가입자는 다음처럼 온다.

```json
{
  "consentRequired": true,
  "accessToken": "<signup-token>",
  "refreshToken": null
}
```

`intent=link`는 현재 로그인 사용자의 소셜 계정 추가 연동 용도다. 서버 게이트는 **인증된 사용자(principal 존재)** 를 요구하며 웹은 쿠키로도 통과하지만, **앱은 `Authorization: Bearer {정식 accessToken}`** 으로 호출한다. 성공 응답은 토큰이 아니라 `AuthMeResponse` 계열 회원 정보다. 앱은 일반 로그인과 계정 연동 API 클라이언트를 분리해 응답 타입을 혼동하지 않는다.

### 5.2 토큰 저장 및 갱신

* 토큰은 **EncryptedSharedPreferences / Android Keystore**에 저장(평문 저장 금지).
* **앱용 갱신 엔드포인트는 이미 존재한다 → `POST /api/v1/auth/reissue` (permitAll, 바디 기반).** (미해결 질문 #3 해소, 2026-06-30 코드 확인)
  * 요청: `{ "refreshToken": "..." }` / 응답: `{ "accessToken": "...", "refreshToken": "..." }`
  * 웹용 `POST /api/v1/auth/refresh`는 **쿠키 전용**이라 앱에서 사용하지 않는다.
  * Access 만료 시 OkHttp `Authenticator`에서 `/reissue` 호출 → 새 access로 원요청 재시도.
  * **Refresh 토큰 회전 없음**: 응답 `refreshToken`은 입력값과 동일(서버가 회전하지 않음). 저장값 갱신 로직은 단순하나, refresh가 만료까지 장수명이라는 점을 보안상 인지한다.
  * **실패 분기 처리**: refresh 무효/회원부재 → `401`; 동의 미완료 회원 → `401`(`consent`) → 소셜 재로그인 후 동의 화면으로 라우팅.
* 로그아웃/회원탈퇴 시 로컬 토큰 및 소셜 SDK 세션을 모두 폐기한다.
* (2차) WebView 화면 도입 시 네이티브 토큰을 안전하게 전달(§3 말미 2차 브릿지 요건 참조). v1은 WebView 미사용.

> ✅ 문서 정합성: `docs/mobile/social-login-api.md` §5를 `/reissue`(바디 기반) 기준으로 갱신 완료(2026-06-30). 회전 없음·동의 게이팅·OkHttp Authenticator 권장 구현 포함.

### 5.3 동의(Consent)

* 기존 `ConsentApi`/약관 동의 플로우를 앱 최초 로그인 시 처리한다. 미동의 사용자는 동의 화면으로 라우팅.
* `POST /api/v1/auth/consent` 요청 바디는 `{ "agreeTerms": true, "agreePrivacy": true, "ageOver14": true }`이며 세 항목 모두 필수다.
* 모바일은 signup token을 `Authorization: Bearer {signupToken}`로 보내야 한다. 서버는 **이번 호출로 동의가 신규 활성화된 경우에만**(`activated=true`) Bearer 요청 응답 body에 정식 `accessToken`/`refreshToken`을 내려준다. 이미 동의 완료된 회원의 멱등 재호출은 토큰 없이 `redirectTo`만 반환한다(`ConsentApi.submit`). 따라서:
  * **정상 경로**: 신규 가입 1회 동의 호출에서 정식 `accessToken`/`refreshToken`을 수령해 저장한다.
  * **이미 정식 토큰 보유** 상태에서 멱등 호출이면 응답에 토큰이 없어도 기존 토큰을 그대로 쓴다.
  * **signup token만 가진 상태에서 토큰을 못 받은 경우**(예: 직전 응답 유실): refresh token이 없어 `/reissue`가 **불가능**하므로, **소셜 재로그인**으로 `consentRequired=false` 정식 토큰을 다시 발급받는다.
* 동의 취소는 `POST /api/v1/auth/consent/cancel`로 처리하고, 앱은 로컬 signup token 및 소셜 SDK 세션을 폐기한다.
* signup token으로 일반 API를 호출하면 `403` + `code=CONSENT_REQUIRED`가 온다. 앱 전역 에러 매퍼는 이를 세션 만료가 아니라 동의 필요 상태로 처리한다.

### 5.4 콜드 스타트 세션 복원 (⚠️ /me 200만으로 메인 진입 금지)

서버 `ConsentGateFilter`는 미동의(PENDING_CONSENT) 회원의 signup token에도 **`GET /api/v1/auth/me`를 허용**한다. 즉 **signup token으로도 `/me`가 `200`을 반환**하므로, 앱이 `/me` 200만 보고 메인으로 보내면 미동의 사용자가 서비스에 진입한다. 세션 복원 규칙을 다음과 같이 명시한다.

1. 저장된 토큰의 **JWT `scope` 클레임이 `SIGNUP`이면**(= signup token) 즉시 **동의 화면**으로 라우팅한다(서버 호출 없이 로컬 판별 가능).
2. 일반 access token으로 `GET /api/v1/auth/me` 호출 후 **응답 `status` 필드로 분기**:
   * `status == "ACTIVE"` → 메인 진입.
   * `status == "PENDING_CONSENT"` → 동의 화면으로 라우팅(메인 금지).
   * `401`(만료) → `/reissue` 시도 → 실패 시 로그인 화면.
3. `AuthMeResponse`에는 `memberUid/email/role/nickname/profileImageUrl/provider/**status**/createdAt`이 포함된다. `status`는 `MemberStatus` enum(`ACTIVE`/`PENDING_CONSENT`)의 이름이다.

> 요지: **메인 진입 게이트 = `scope!=SIGNUP` AND `/me.status==ACTIVE`.** 둘 중 하나라도 어긋나면 동의 플로우로 보낸다.

---

## 6. 앱 고유(네이티브) 기능

1차 출시 필수(★)와 후속(◇)으로 구분한다.

* ★ 시스템 뒤로가기 / 딥링크 라우팅 (Android App Links: `https://{도메인}/...` → 해당 화면)
* ★ 다크모드 / 시스템 폰트 크기 / 접근성 대응
* ★ 네트워크 오류·오프라인 상태 UI, 재시도
* ★ 스플래시(Android 12 SplashScreen API) + 앱 아이콘/브랜딩
* ◇ 푸시 알림(FCM): 새 콘텐츠, 커뮤니티 답글, 학습 리마인더 — 별도 서버 API/동의 필요
* ◇ 오프라인 성경 읽기 캐시(Room)
* ◇ 홈 화면 위젯(오늘의 말씀), 공유 인텐트
* ◇ 인앱 리뷰(Play In-App Review), 인앱 업데이트

---

## 7. 기술 스택 (제안)

| 영역 | 선택 | 비고 |
|------|------|------|
| 언어 | Kotlin | 백엔드와 동일 언어 |
| 최소 SDK | API 26 (Android 8.0) 권장, 정책에 따라 조정 | |
| 타겟 SDK | 최신 Play 요구 버전 | 매년 갱신 |
| UI | Jetpack Compose + Material 3 | |
| 아키텍처 | MVVM + Clean(아니면 UDF), 단방향 데이터 흐름 | 백엔드 헥사고날과 별개 |
| DI | Hilt | |
| 네트워크 | Retrofit + OkHttp + Kotlinx Serialization/Moshi | Bearer 인터셉터 |
| 비동기 | Coroutines + Flow | |
| 이미지 | Coil | |
| 로컬 저장 | DataStore / EncryptedSharedPreferences, Room(후속) | |
| WebView | (v1 미사용) | 2차 하이브리드 화면 도입 시에만 AndroidX WebKit |
| 분석 | 기존 `analytics` 모듈/이벤트 API 연동 + (선택) Firebase | `AppInstallBannerEventApi` 패턴 참고 |
| 푸시 | Firebase Cloud Messaging(후속) | |

> 백엔드는 **v1에서 변경 없음**(앱용 토큰 갱신 `/api/v1/auth/reissue` 이미 존재 — §5.2). 2차 기능(통합검색, 푸시 토큰 등록, 정적 콘텐츠 동적화 등)에서 신규 API 요구가 생기면 백엔드 작업 항목으로 별도 트래킹한다.

---

## 8. API 연동 규약

* Base URL: 백엔드 기본값 `http://localhost:8080`은 **Android에서 그대로 쓰면 안 된다** — 기기/에뮬레이터 안의 localhost는 백엔드 PC에 닿지 않는다. 빌드 variant별로 분리:
  * `debug`(에뮬레이터): **`http://10.0.2.2:8080`** (에뮬레이터 → 호스트 PC 루프백 별칭)
  * `debug`(실기기): 백엔드 PC의 **LAN IP**(예: `http://192.168.x.x:8080`), 같은 네트워크 + `usesCleartextTraffic` 허용 필요
  * `release`: **HTTPS 운영 도메인**(`https://...`)
* 빌드 variant: `debug`(개발 서버), `release`(운영 서버)로 BASE_URL 분리.
* 에러 포맷이 **두 종류**다 — 앱 에러 매퍼는 `code`가 **있을 수도 없을 수도** 있다고 가정하고 **HTTP status를 1차 신호로** 삼아야 한다:
  * **도메인 예외(`ServiceError` → `GlobalExceptionHandler`)**: `{ "status", "code", "message" }` — `code = ErrorType.name` 포함. 이 경우 `code` 기반 분기(다국어/문구 변경에 안전).
  * **Spring Security 예외**: 인증 실패 `401`은 `sendError(401)`(컨테이너 기본 에러 바디 → `code` 없음), 접근 거부 `403`도 `code` 없이 응답한다. 이 경로는 **`code`가 없으므로 HTTP status로 처리**한다.
* 매핑 권장: `code` 존재 시 `code` 우선, 없으면 status fallback — 예) `401` → 토큰 갱신/재로그인, `403`(code 없음) → 권한 없음 안내.
* ⚠️ **`CONSENT_REQUIRED`는 status까지 함께 봐야 한다**(같은 code가 두 status로 나온다):
  * **`403` + `CONSENT_REQUIRED`** (`ConsentGateFilter`): signup token으로 일반 API 접근 → **동의 화면으로 라우팅**.
  * **`400` + `CONSENT_REQUIRED`** (`ErrorType` 정의=400): 동의 제출 시 3항목(`agreeTerms/agreePrivacy/ageOver14`) 미충족 → **동의 화면의 입력 검증 오류**(라우팅이 아니라 "모두 동의 필요" 안내).
  * 즉 `code==CONSENT_REQUIRED && status==403` → 동의 플로우 진입, `&& status==400` → 동의 폼 검증 실패.
* 주요 `code` 예: `CONSENT_REQUIRED`, `SOCIAL_LOGIN_INVALID_TOKEN`, `AUTHENTICATION_REQUIRED` 등. 정확한 코드 집합은 백엔드 `ErrorType` enum / Swagger 확인.
* (선택) 백엔드에서 보안/validation 예외까지 `ErrorResponse`로 통일하면 앱이 전부 `code` 기반으로 처리 가능 — 2차 개선 후보.
* 모든 요청에 앱 식별 헤더(`X-Client: android`, `X-App-Version`) 부착 권장 → 서버 로깅/분기.
* 인증 필요 API는 `Authorization: Bearer {accessToken}`만 사용한다. 웹 쿠키 기반 `/api/v1/auth/refresh`는 앱에서 사용 금지.
* 공개 API라도 개인화가 가능한 API(예: `GET /api/v1/game/ranking`)는 Bearer가 있으면 내 랭킹을 함께 반환할 수 있다. v1 게임 제외지만 2차 구현 시 이 패턴을 따른다.
* 네트워크 보안: `cleartextTraffic` 운영 비활성, 인증서 핀닝(선택), TLS only.

---

## 9. 단계별 로드맵 (마일스톤)

### M0 — 기반 (1차 스프린트)
* Android Studio 프로젝트 생성, `com.elseeker.android`, 빌드 variant, CI(서명/번들).
* 디자인 토큰·테마(Material 3, 다크모드), 공통 컴포넌트.
* 네트워크/인증 기반(Retrofit, 토큰 저장, Refresh 인터셉터), 소셜 로그인 3종.

### M1 — 1차 패리티 기준선 (MVP, 전부 네이티브) — 4-A절 확정 범위
* 홈/네비게이션(하단 탭) — 우주 섹션은 네이티브 경량 비주얼 또는 생략(4-A.4).
* 성경 읽기·책 개요·검색·하이라이트·메모·진도(N). 번역본 노출은 4-A.9 데이터 게이트를 따른다.
* 학습 전체(사전 + 정적 콘텐츠 + 족보 + 개요영상 + 성경역사) — 정적 데이터 번들로 네이티브화(N). 족보/개요영상은 별도 트랙 S2로 분리(공수·분리안 4-A.7 참조).
* 마이페이지/프로필 수정/소셜 계정 연동 관리/내 메모/회원탈퇴, 1:1 문의(N).
* 소셜 로그인 3종·신규 가입 동의·로그아웃·토큰 갱신.
* 딥링크/뒤로가기/오류·오프라인 처리/스플래시.
* **WebView 미사용** (외부 링크만 Custom Tabs 위임).

### M2 — 게임·커뮤니티 네이티브화
* 게임: 퀴즈/OX/랭킹 우선 → 타이핑/낱말퍼즐(N).
* 커뮤니티: 목록/상세 → 작성·신고/차단(N).
* (학습은 v1에서 완료. 정적 번들 콘텐츠를 운영상 동적화할 필요가 생기면 별도 API화 검토.)

### M3 — 앱 고유 가치
* FCM 푸시(+서버 등록 API), 오프라인 캐시, 위젯, 인앱 리뷰/업데이트.

### M4 — 출시·운영
* Play Console 등록, Data Safety/등급 설문, 내부테스트→비공개테스트→프로덕션 단계 배포.
* 크래시/분석 모니터링(Crashlytics 등), `app-install-banner-prd.md` 웹 배너와 설치 귀속 연동.

---

## 10. 비기능 요구사항

* 성능: 콜드 스타트 2.5s 이내(중급 기기), 주요 화면 전환 16ms 프레임 예산 준수.
* 안정성: 크래시 프리 세션 99.5%+ 목표.
* 접근성: TalkBack, 동적 글자크기, 대비 AA.
* 보안: 토큰 암호화 저장, 루팅/디버그 빌드 분리, 민감정보 로그 금지.
* 국제화: 1차 한국어, 문자열 리소스화로 확장 대비.
* 호환: 최소 SDK ~ 최신, 폴더블/태블릿 레이아웃 깨짐 방지.

---

## 11. 스토어 정책 / 출시 게이트

* 소셜 로그인 각 콘솔에 패키지명/키 해시(SHA-1, SHA-256) 등록(디버그·릴리스 키 모두).
* Play Console: 앱 서명(Play App Signing), Data Safety, 콘텐츠 등급, 개인정보처리방침 URL(기존 `legal` 페이지 활용), 타겟 SDK 충족.
* "단순 웹래퍼" 리스크 없음: v1은 전부 네이티브(WebView 미사용)이므로 심사상 자연 충족. 네이티브 UX·딥링크로 가치 명시.
* 후원/외부결제 진입점은 `donation-prd.md` 정책 게이트 통과 전 비노출.
* 개인정보·약관 동의(Consent) 플로우 앱 내 구현.

---

## 12. 제외 범위 (1차)

* iOS 앱(별도 PRD).
* 백엔드 도메인 로직 변경(필요한 신규 API만 추가).
* 관리자(admin) 화면 — 앱 대상 아님.
* 결제/구독/인앱결제.
* 완전 오프라인 모드(읽기 캐시는 후속).

---

## 13. 리스크 및 대응

| 리스크 | 영향 | 대응 |
|--------|------|------|
| 정적 콘텐츠 원본을 앱 리소스로 추출하는 수작업 | 학습 영역 일정 증가 | 데이터만 JSON 번들로 추출(렌더링 로직 제외), 4-A.7 스프린트 분리 |
| 족보·개요영상 이식 공수 | v1 일정 압박 | S2로 분리, 필요 시 `v1.0.x` 패스트팔로우(4-A.7) |
| `social-login` 응답 분기 누락 | 신규 가입자가 refresh 없이 일반 화면으로 진입 | `consentRequired=true`면 signup token만 저장하고 동의 화면으로 강제 라우팅 |
| signup token을 일반 access token처럼 사용 | `403 CONSENT_REQUIRED` 반복 | 전역 에러 매퍼에서 동의 필요 상태로 처리 |
| 번역본 목록에 미완성 seed 노출 | 장/절 화면 404/500 또는 빈 화면 | 4-A.9 데이터 게이트 적용, v1 기본 KRV만 노출 |
| 소셜 SDK 키/해시 누락 | 로그인 실패 | M0 체크리스트화, 디버그·릴리스 분리 |
| 성경 API 경로 불일치(`/bibles` vs `/bible/reading`) | 연동 오류 | API 클라이언트에 경로 상수화 + Swagger 대조(부록 B.1) |

---

## 14. 미해결 질문 (Open Questions)

1. ~~1차 출시 패리티 기준선~~ — **해소(2026-06-30, 4-A절):** 슬림 코어, 게임·커뮤니티 2차 제외, v1 전부 네이티브.
2. ~~v1 WebView 인증 전략~~ — **v1 무관(WebView 미사용).** 단, 2차 하이브리드 화면 도입 시 쿠키 동기화 vs 헤더 인젝션 재검토 필요.
3. ~~Refresh 토큰 갱신 전용 API(앱용) 존재 여부~~ — **해소(2026-06-30):** `POST /api/v1/auth/reissue`(바디 기반) 구현 확인. 회전 없음·동의 게이팅 유의(5.2). `social-login-api.md` §5 갱신 완료.
4. ~~홈 3D 우주 섹션·히어로 배너 v1 처리~~ — **해소(2026-06-30, 4-A.4):** 우주=경량 네이티브 비주얼 대체, 히어로=앱 정적 리소스(2차 서버화).
5. ~~사전(Dictionary) v1 포함 여부~~ — **해소(2026-06-30, 4-A.4):** v1 포함(API 완비).
6. 푸시 도입 시점과 서버측 토큰 등록/발송 API 신설 범위. (2차)
7. ~~정적 학습 콘텐츠 API화 우선순위~~ — **재정의(2026-06-30):** 학습 v1 포함 확정, 단 정적 데이터 번들 방식이라 API화는 불필요(운영상 동적화 필요 시에만 후속 검토). 잔여 작업: 콘텐츠 원본을 앱 리소스로 추출.
8. 후원 진입점을 앱에서 노출할지 여부(정책 판단 필요).
9. 분석: 기존 `analytics` 이벤트 체계를 그대로 쓸지, Firebase를 병행할지.
10. 번역본 공개 정책: v1은 4-A.9에 따라 KRV 중심으로 보호하고, 후속으로 백엔드 `visibleToClient`/`contentReady` 플래그를 둘지 결정.

---

## 부록 A. 참고 문서 (백엔드 레포 기준 경로)

> 아래는 **백엔드 ElSeeker 레포** 내 경로다. Android에서는 심링크 `the_bible_project/` 를 prefix로 붙여 read-only로 참조한다(§0.4).

* `docs/mobile/social-login-api.md` — 모바일 소셜 로그인 API 참고 문서(필드 계약 정본은 Swagger/OpenAPI와 본 PRD 5장)
* `docs/googleplay/app-install-banner-prd.md` — 웹→앱 설치 유도 배너, 패키지명/Referrer 정책
* `docs/support/donation-prd.md` — 후원/외부결제 정책 게이트
* `CLAUDE.md` — 백엔드 아키텍처, 인증/필터 체인, 환경변수

---

## 부록 B. 이식 자산 & 외부 의존 체크리스트 (Android 프로젝트 작업 전 준비물)

Android 프로젝트는 백엔드 레포를 **심볼릭 링크로 연결**한다(§0.3/0.4). 따라서 아래 자산은 **물리 복사 없이 심링크 경로(`the_bible_project/...`)로 참조**한다.

### B.1 API 계약 (호출 대상 — 런타임 의존)

* **정본 = 백엔드 Swagger/OpenAPI**: `{EL_SEEKER_API_BASE_URL}/swagger-ui/index.html`, 스키마 JSON `{...}/v3/api-docs`.
* v1에서 호출하는 엔드포인트(2026-06-30 코드 확인 — 필드 계약은 Swagger 정본). **`(public)`=비로그인 허용, `(auth)`=Bearer 필수**(`SecurityConfig` 기준). 인증 필수 엔드포인트를 토큰 없이 호출하면 `401`이므로, 앱은 호출 전 인증 상태를 확인한다:
  * 인증: `POST /api/v1/auth/social-login` (public), `POST /api/v1/auth/reissue` (public), `GET /api/v1/auth/me` (auth — signup token도 허용, §5.4), `POST /api/v1/auth/consent`·`/consent/cancel` (auth — signup token 허용)
  * 성경 기본 **(public)**: `GET /api/v1/bibles/translations`, `.../books`, `.../books/{bookOrder}`, `.../chapters`
  * 성경 본문/이동 **(public)**: `.../chapters/{chapterNumber}/verses`, `GET .../navigate?direction=PREV|NEXT` (⚠️ 대문자 enum, 소문자 400), `GET /api/v1/bibles/daily?translationType=KRV` (기본값 `KRV`)
  * 장 상태 **(auth)**: `GET .../chapters/{chapterNumber}/state` (메모·하이라이트·읽음·장 메모 통합)
  * 절 검색 **(public)**: `GET /api/v1/bibles/translations/{translationId}/search?keyword=&bookOrder=&page=&size=&track=` · 인기 검색어 랭킹 **(public)**: `GET /api/v1/bibles/search-keywords/ranking?limit=`
  * 절 하이라이트 **(auth)**: `GET .../highlights`, `PUT/DELETE .../verses/{verseNumber}/highlight`
  * 절 메모 **(auth)**: `GET .../memos`, `PUT/DELETE .../verses/{verseNumber}/memo`
  * 장/책 메모 **(auth)**: `GET/PUT/DELETE .../chapter-memo`, `GET/PUT/DELETE /api/v1/bibles/translations/{translationId}/books/{bookOrder}/book-memo`
  * 성경 읽기 진도 **(auth)**: `POST /api/v1/bible/reading/chapters/read`, `GET /api/v1/bible/reading/chapters/read?translationId=&bookOrder=` ⚠️ **단수형 `bible` base** (위 `bibles` 와 다름)
  * 내 메모(절) **(auth)**: `GET /api/v1/bibles/my-memos`(전체), `.../my-memos/translations`(번역본별), `.../my-memos/books`(책별) — ⚠️ base/`/translations`/`/books` 3개 모두 존재(탭 필터). 누락 주의
  * 내 메모(장) **(auth)**: `GET /api/v1/bibles/my-chapter-memos` + `/translations` + `/books` (동일 패턴)
  * 내 메모(책) **(auth)**: `GET /api/v1/bibles/my-book-memos` + `/translations` + `/books` (동일 패턴)
  * 메모 카운트 **(auth)**: `GET /api/v1/bibles/my-memo-counts`
  * 학습 사전 **(public)**: `GET /api/v1/study/dictionaries?keyword=&page=&size=&track=`, `.../{id}`, `.../{id}/references`, `.../search-keywords/ranking?limit=`
  * 마이 **(auth)**: `PUT /api/v1/members/{memberUid}`, `DELETE /api/v1/members/{memberUid}`, `GET/DELETE /api/v1/members/{memberUid}/oauth-accounts`, `POST /api/v1/members/{memberUid}/oauth-accounts/initialize-profile`
  * 소셜 계정 추가 연동 **(auth — 인증된 사용자; 앱은 Bearer)**: `POST /api/v1/auth/social-login` + body `{ provider, token, intent: "link" }`. 성공 응답은 `AuthMeResponse`
  * 지원: `POST /api/v1/qna/contacts` **(public)** — 공개 문의 작성. 그 외 `/api/v1/qna/**`는 **(auth)** — 내 문의 `POST/GET /api/v1/qna/inquiries`, `GET/PUT/DELETE /api/v1/qna/inquiries/{id}`
* 인증 헤더: `Authorization: Bearer {accessToken}`. 갱신은 `/reissue`(바디 기반, 회전 없음 — 5.2 참조).

### B.2 정적 콘텐츠 원본 (네이티브 이식 source-of-truth — 심링크로 참조)

아래 경로는 모두 **백엔드 레포 루트 기준**이다. Android에서는 `the_bible_project/` 를 앞에 붙여 읽고, **데이터/텍스트만** 추출해 네이티브 리소스(JSON/Compose)로 옮긴다.

| 화면 | 참조할 백엔드 경로(심링크 기준) | 용도 |
|------|--------------------------------|------|
| 족보 | `src/main/resources/static/js/study/bible-genealogy.js` (데이터 배열) + 동명 css/`templates/study/bible-genealogy.html` | 마태/누가 ~139레코드, 타임라인/접기 UI 사양 |
| 개요영상 | `src/main/resources/static/js/study/bible-overview-video.js` (66권 데이터) + 동명 css/html | 영상 목록·검색·딥링크 사양 |
| 성경역사 | `src/main/kotlin/com/elseeker/study/adapter/input/web/client/response/HistoryViewResponse.kt`(`HistoryDummyData` 정의) + `HistoryWebController.kt` + `templates/study/history*.html` | 연대/사건/상세 더미데이터 |
| 학습 정적 9종 | `templates/study/` 의 ten-commandments·apostles-creed·lords-prayer·creation·holy-week·twelve-disciples·twelve-tribes·public-reading-of-scripture·bible-commentary `.html` (+ 동명 `static/js/study/*.js`, `static/css/study/*.css`) | 본문 텍스트/데이터/레이아웃 |
| 홈 히어로 이미지 | `src/main/resources/static/images/thebible1.png`, `thebible2.png` | v1 앱 정적 히어로 리소스 후보 |
| 홈 비주얼 | (참고) `static/js/home/universe-bg.js`, `static/css/home.css` | 경량 네이티브 비주얼 대체 시 디자인 참고(4-A.4-1) |
| 브랜딩/프로필 | `src/main/resources/static/images/elseeker.png`, `elseeker_login.png`, `elseeker_og.png`, `playstore-icon.png`, `user.png` | 앱 아이콘/스플래시/기본 프로필 참고. 최종 Play 아이콘은 Android 프로젝트에서 별도 생성 |
| 소셜/학습 아이콘 | `src/main/resources/static/images/btn_google.svg`, `btn_kakao.svg`, `btn_naver.svg`, `images/icon/*.svg` | 로그인 버튼·학습 메뉴 아이콘 참고. Android 벡터/Compose 리소스로 재작성 가능 |

> 추출 팁: JS의 `const X = [...]` 데이터 배열과 Kotlin `HistoryDummyData` 는 JSON으로 옮겨 `assets/` 또는 Compose 리소스로 번들. 각 학습 정적 페이지는 자체 `*.js` 도 있으니 데이터가 거기 있는지 확인. 렌더링 로직은 이식하지 말고 **데이터만** 가져와 네이티브로 재구현한다.

### B.3 성경 데이터 준비 상태 확인

* API 목록(`GET /api/v1/bibles/translations`)은 계약상 `KRV/NKRV/KJV`를 반환할 수 있으나, 앱 노출은 4-A.9 데이터 게이트를 따른다.
* `src/main/resources/data/krv/`는 66권 본문 seed가 존재한다.
* `src/main/resources/data/nkrv/`는 현재 일부 책 seed만 확인된다.
* `src/main/resources/data/kjv/`는 현재 `bible_kjv_book.sql`만 확인되며 장/절 본문 seed는 확인되지 않는다.
* 앱 개발 시작 시 실제 실행 서버의 `/translations`→`/books`→`/chapters`→`/verses` smoke test로 최종 노출 번역본을 다시 확정한다.

### B.4 동반 문서 (심링크로 참조)

* `docs/mobile/social-login-api.md` — 인증 구현 참고(Provider별 SDK·키 해시·reissue·시퀀스). 필드 계약은 Swagger/OpenAPI와 본 PRD 5장을 우선한다.
* `docs/support/donation-prd.md`, `docs/googleplay/app-install-banner-prd.md` — 정책/패키지명 참고.

### B.5 외부 콘솔/시크릿 (사용자 준비)

* Google Cloud(서버 웹 Client ID — `requestIdToken`에 사용), Kakao/Naver 개발자 콘솔에 **Android 패키지명 + 키 해시(SHA-1/256, 디버그·릴리스)** 등록.
* `{EL_SEEKER_API_BASE_URL}` 개발/운영 값.
* Play Console 접근, 앱 서명 키.
