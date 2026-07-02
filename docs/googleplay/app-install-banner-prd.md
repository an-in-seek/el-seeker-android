# PRD 요약: Android 웹 방문자 대상 ElSeeker 앱 다운로드 유도 기능

## 1. 목적

Android 모바일 기기에서 브라우저로 ElSeeker 웹 서비스에 접속한 사용자에게 ElSeeker Android 앱 설치를 자연스럽게 안내한다.

사용자를 강제로 Google Play Store로 이동시키지 않고, 사용자가 직접 선택할 수 있는 앱 설치 유도 배너를 제공한다.

---

## 2. 핵심 원칙

* Android 모바일 브라우저 사용자에게만 노출한다.
* 강제 리다이렉트는 사용하지 않는다.
* 사용자가 “앱 설치하기” 버튼을 눌렀을 때만 Google Play Store로 이동한다.
* 사용자는 배너를 닫거나 “웹에서 계속 보기”를 선택할 수 있다.
* 닫기 후 일정 기간 동안 반복 노출하지 않는다.
* Google Play Store 공식 링크만 사용한다.
* APK 직접 다운로드, 외부 마켓, 비공식 설치 링크는 제공하지 않는다.

---

## 3. Google Play Store 링크

앱 다운로드 링크(설치 유입 분석용 Install Referrer 파라미터 포함):

https://play.google.com/store/apps/details?id=com.elseeker.android&referrer=utm_source%3Delseeker_web%26utm_medium%3Dinstall_banner

패키지명:

com.elseeker.android

### 링크 파라미터 정책

* `pcampaignid=web_share`는 Play "공유하기"가 자동으로 붙이는 태그로 본 배너 유입 분석에 부적합하므로 사용하지 않는다.
* 대신 `referrer`에 `utm_source=elseeker_web`, `utm_medium=install_banner`를 전달하여 Google Play Console의 Install Referrer로 배너 유입 설치를 귀속한다.
* `referrer` 값은 고정값으로 관리하며 사용자 입력값을 결합하지 않는다(15장 보안 정책 참조).

---

## 4. 제공 범위

MVP에서는 다음 기능만 제공한다.

* Android 모바일 브라우저 감지
* 하단 앱 설치 유도 배너 노출
* “앱 설치하기” 버튼 제공
* “웹에서 계속 보기” 또는 닫기 버튼 제공
* Google Play Store 공식 링크 이동
* 동일 세션 재노출 제한
* 닫기 후 7일간 재노출 제한
* 기본 이벤트 로깅

---

## 5. 제외 범위

다음 기능은 MVP에서 제외한다.

* 강제 Google Play Store 리다이렉트
* iOS 앱 다운로드 유도
* PC 브라우저 다운로드 유도
* APK 직접 다운로드
* 외부 마켓 링크
* 앱 설치 여부 정확 판별
* 앱 설치 완료 여부 추적
* 앱 실행 딥링크 처리
* Android App Links 연동

---

## 6. 노출 조건

다음 조건을 모두 만족하면 배너를 노출한다.

* Android OS로 판단되는 기기
* 모바일 브라우저 접속
* ElSeeker 웹 서비스 접속
* ElSeeker 앱 내부 WebView가 아닌 일반 브라우저 접속
* 사용자가 최근 7일 내 배너를 닫지 않은 상태

Android 태블릿은 앱 지원 범위에 따라 노출할 수 있다.
데스크톱 모드 브라우저는 판별이 어려울 수 있으므로 보수적으로 미노출 처리한다.

---

## 7. 비노출 조건

다음 경우에는 배너를 노출하지 않는다.

* PC 브라우저 접속
* iOS 기기 접속
* ElSeeker 앱 내부 WebView 접속
* 사용자가 최근 7일 내 배너를 닫은 경우
* 동일 세션에서 이미 배너가 노출된 경우
* 검색엔진 봇, 크롤러, 소셜 미리보기 봇 접속

---

## 8. 기기 감지 기준

기본 판별은 User-Agent 문자열을 사용한다.

* User-Agent에 `Android` 포함 여부 확인
* User-Agent에 `Mobile` 포함 여부 확인
* 화면 크기와 터치 지원 여부를 보조 기준으로 활용

User-Agent Client Hints가 지원되는 브라우저에서는 보조 판단 정보로 활용할 수 있다.

### WebView 판별 (일반 브라우저와 구분)

ElSeeker 하이브리드 앱 내부 WebView 접속은 배너 노출 대상에서 제외하므로, 다음 기준으로 일반 브라우저와 구분한다.

* 1차: 하이브리드 앱이 주입하는 공통 WebView marker(`window.ElSeekerWebView`) 존재 여부로 판별한다. marker가 존재하면 앱 내부 WebView로 간주하고 배너를 노출하지 않는다.
* 2차(호환): 기존 네이티브 브릿지 객체(`window.AppBridge`) 존재 여부로 판별한다. 브릿지가 존재하면 앱 내부 WebView로 간주하고 배너를 노출하지 않는다.
* 3차(보조): User-Agent의 Android WebView 토큰(`; wv)`) 포함 여부를 확인한다.
* 본 WebView 감지 기준은 `donation-prd.md`의 공통 marker 규약(`window.ElSeekerWebView`)과 동일한 메커니즘을 사용하여 두 기능이 일관된 환경 판별을 하도록 한다.

### 봇 판별

검색엔진 봇, 크롤러, 소셜 미리보기 봇은 배너 노출 대상에서 제외한다. 봇 판별은 기존 분석 인프라(`site_visit_event`의 `is_bot` 판정 로직)를 재사용한다.

단, 기기 판별은 100% 정확하지 않으므로 앱 설치 배너 노출 여부 판단 용도로만 사용한다.

---

## 9. UX 정책

기본 노출 방식은 화면 하단 고정 배너로 한다.

배너에는 다음 요소를 포함한다.

* ElSeeker 앱 아이콘
* 제목
* 짧은 설명
* 앱 설치하기 버튼
* 웹에서 계속 보기 버튼
* 닫기 버튼

모달은 사용자 경험을 방해할 수 있으므로 MVP에서는 사용하지 않는다.

---

## 10. 권장 문구

### 제목

ElSeeker 앱으로 더 편하게 이용해보세요

### 설명

Android 앱에서 더 안정적으로 성경 학습 서비스를 이용할 수 있습니다.

### 주요 버튼

앱 설치하기

### 보조 버튼

웹에서 계속 보기

---

## 11. 사용자 흐름

1. 사용자가 Android 모바일 브라우저로 ElSeeker 웹 서비스에 접속한다.
2. 웹 서비스는 Android 모바일 브라우저 여부를 판단한다.
3. 노출 조건을 만족하면 하단 앱 설치 유도 배너를 표시한다.
4. 사용자가 “앱 설치하기”를 누르면 Google Play Store 링크로 이동한다.
5. 사용자가 “웹에서 계속 보기” 또는 닫기 버튼을 누르면 배너를 닫는다.
6. 닫은 사용자는 7일 동안 동일 배너를 다시 보지 않는다.

---

## 12. 반복 노출 제한

재노출 제한은 두 가지 저장소를 분리하여 사용한다.

* 7일 재노출 제한: localStorage에 닫은 시각을 저장한다.
  * 저장 키 예시: `elseeker_app_install_banner_dismissed_at`
* 동일 세션 재노출 제한: sessionStorage에 노출 여부 플래그를 저장한다(탭/세션 종료 시 초기화).
  * 저장 키 예시: `elseeker_app_install_banner_shown_in_session`

정책:

* 동일 세션 내 재노출 금지(sessionStorage 기준)
* 닫기 후 7일간 재노출 금지(localStorage 기준)

### 저장소 구현

* 원시 `localStorage`/`sessionStorage`를 직접 호출하지 않고, 공용 유틸 `js/storage-util.js`의 `LocalStore`/`SessionStore`를 사용한다(localStorage 차단 환경 폴백이 이미 처리됨).
* 저장 키는 `storage-util.js`의 `STORAGE_KEYS`(Object.freeze)에 등록하여 관리한다.

Cookie는 서버 렌더링 단계에서 노출 여부를 판단해야 하는 경우에만 사용한다.

---

## 13. 이벤트 로깅

기본 수집 이벤트:

* app_install_banner_impression
* app_install_banner_click
* app_install_banner_dismiss

기본 수집하지 않는 이벤트:

* app_install_banner_not_shown

미노출 이벤트는 로그가 과도하게 쌓일 수 있으므로 기본 수집하지 않는다.
필요 시 디버깅 또는 실험 기간에만 샘플링 방식으로 수집한다.

### 수집 인프라

배너 이벤트는 별도 체계를 신설하기보다 기존 분석 인프라(`site_visit_event`, 설계: `docs/common/site-visitor-count-design.md`)에 얹는 것을 우선 검토한다. 봇 제외(`is_bot`)와 방문자 식별(`visitor_id`) 로직을 재사용할 수 있다.

### 익명 집계 원칙

배너 이벤트는 완전 익명으로 집계한다. 로그인 사용자라도 `member_uid`(회원 식별자)는 기록하지 않으며, 익명 방문자 식별자(`visitor_id`)만 사용한다. 후원자/사용자 식별 목적의 회원 정보 결합은 하지 않는다(14장 미수집 정보 참조).

---

## 14. 수집하지 않는 정보

다음 정보는 수집하지 않는다.

* 개인 식별 정보
* 전화번호
* 이메일
* 정확한 기기 고유 식별자
* Google 계정 정보
* 앱 설치 완료 여부
* Google Play Store 내 사용자 행동

---

## 15. 보안 정책

* Google Play Store 공식 링크만 사용한다.
* 다운로드 URL은 고정값으로 관리한다.
* 사용자 입력값으로 redirect URL을 전달받지 않는다.
* APK 직접 다운로드 링크를 제공하지 않는다.
* 비공식 앱 배포 링크를 제공하지 않는다.
* 단축 URL 사용은 지양한다.

---

## 16. 접근성 및 UX 요구사항

* 배너가 본문 콘텐츠를 과도하게 가리지 않아야 한다.
* 닫기 버튼은 명확해야 한다.
* 버튼 터치 영역은 충분해야 한다.
* 화면 회전 시 레이아웃이 깨지지 않아야 한다.
* 다크모드에서도 문구와 버튼이 잘 보여야 한다.
* 배너 때문에 웹 서비스 이용이 막히면 안 된다.

---

## 17. 성능 요구사항

* 배너 로직은 초기 페이지 렌더링을 지연시키지 않는다.
* 외부 SDK 의존성을 추가하지 않는다.
* 스크립트는 가볍게 구현한다.
* 앱 아이콘 이미지는 최적화된 리소스를 사용한다.
* 배너 노출로 인한 레이아웃 흔들림을 최소화한다.

---

## 18. QA 체크리스트

* Android Chrome에서 배너가 노출되는가?
* Android 삼성 인터넷에서 배너가 노출되는가?
* Android Firefox에서 배너가 노출되는가?
* PC 브라우저에서는 배너가 노출되지 않는가?
* iPhone Safari에서는 배너가 노출되지 않는가?
* 앱 내부 WebView에서는 배너가 노출되지 않는가?
* 앱 설치하기 클릭 시 Google Play Store로 이동하는가?
* 닫기 후 동일 세션에서 재노출되지 않는가?
* 닫기 후 7일 동안 재노출되지 않는가?
* 개인정보가 이벤트에 포함되지 않는가?

---

## 19. 성공 지표

* Android 모바일 방문자 대비 배너 노출률
* 배너 노출 대비 앱 설치하기 클릭률
* 배너 닫기율
* Google Play Store 이동률
* 배너 노출 후 웹 이탈률
* Android 앱 신규 설치 수 변화

### 측정 출처 구분

* 노출률·클릭률·닫기율·이동률·웹 이탈률은 앱 내 이벤트 로깅(13장)으로 측정한다.
* "Android 앱 신규 설치 수 변화"는 앱 자체 데이터로 추적하지 않으며(5장·14장 제외 범위), Google Play Console + Install Referrer(3장의 `utm_medium=install_banner`)를 통해 외부에서 귀속·측정한다.

---

## 20. 최종 결론

본 기능은 Android 모바일 웹 방문자에게 ElSeeker Android 앱 설치를 자연스럽게 안내하기 위한 기능이다.

MVP는 다음 구조로 구현한다.

Android 모바일 브라우저 감지

* 하단 앱 설치 유도 배너
* Google Play Store 공식 링크 이동
* 웹에서 계속 보기 제공
* 닫기 후 7일간 반복 노출 제한
* 개인정보 없는 최소 이벤트 로깅

강제 리다이렉트는 사용하지 않는다.
