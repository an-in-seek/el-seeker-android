# ElSeeker Android App - Android 15/16 대응

SDK 35(Android 15) 타겟팅 이후 Google Play Console이 표시한 **권장 조치 3건**에 대한 분석과 대응 기록이다. 세 건 모두 출시를 막는 오류가 아니라 **권장(non-blocking) 사항**이며, 아래 변경은 다음 릴리즈 빌드부터 반영된다.

> **요약**
> | # | 항목 | 상태 | 비고 |
> |---|------|------|------|
> | ① | Edge-to-edge (더 넓은 화면) | ✅ 이미 처리됨 | 코드 변경 불필요 |
> | ② | 지원 중단 API (바 색상 / cutout) | ⚠️ 부분 수정 | 우리 측 해결, SDK 내부 잔여 |
> | ③ | 대형 화면 크기/방향 제한 | ✅ 수정 | Naver SDK 액티비티 오버라이드 |

---

## ① Edge-to-edge (더 넓은 화면) — 이미 처리됨

**경고**: "Android 15부터 SDK 35 타겟 앱은 기본적으로 더 넓은 화면을 표시한다. 인셋을 처리해야 한다."

**진단**: 본 앱은 이미 올바르게 처리하고 있어 **코드 변경이 불필요**하다.

- `ElSeekerActivity.onCreate()` 에서 **`enableEdgeToEdge()`** 호출 — 투명 시스템 바 + `SystemBarStyle.auto`(OS 다크/라이트 테마에 맞춰 바 아이콘 명암 자동 전환)
- `MainScreen` 최상위 `Box` 가 **`systemBarsPadding()`** 으로 인셋을 적용하며, 그 앞에 `background()` 로 바 뒤 영역까지 칠한다
- WebView·Error·Offline 모든 UiState가 이 패딩된 컨테이너 안에서 렌더링되어 시스템 바를 침범하지 않는다

> 이 경고는 SDK 35 앱 전반에 표시되는 일반 안내이며, 본 앱 구현은 권장 방식을 따른다.

---

## ② 지원 중단 API (deprecated for edge-to-edge)

**경고**: 다음 API/파라미터가 Android 15에서 지원 중단됨.
- `android.view.Window.setStatusBarColor`
- `android.view.Window.setNavigationBarColor`
- `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`

**진단**: 두 출처로 나뉜다.

| 출처 | 내용 | 직접 수정 |
|------|------|-----------|
| **우리 테마** | `themes.xml` 의 `android:statusBarColor` / `android:navigationBarColor` 가 deprecated `Window.set*Color` 로 매핑됨 | ✅ 가능 |
| **로그인 SDK 내부** | 경고 스택의 `com.airbnb.lottie.model.layer.ImageLayer.getBitmap`, 난독화된 `c.x.b` / `c.v.s` 는 Naver/Kakao 로그인 SDK가 번들한 코드 (Lottie는 우리 직접 의존성 아님) | ❌ 불가 |

**대응 (우리 측)**: edge-to-edge 가 이미 `enableEdgeToEdge(SystemBarStyle.auto)` 로 바 색상/명암을 처리하므로, 중복이자 지원 중단된 테마 속성을 제거했다.

```diff
  <!-- app/src/main/res/values/themes.xml -->
- <style name="Theme.ElSeeker" parent="android:Theme.Material.Light.NoActionBar">
-     <item name="android:statusBarColor">@color/primary</item>
-     <item name="android:navigationBarColor">@color/white</item>
- </style>
+ <style name="Theme.ElSeeker" parent="android:Theme.Material.Light.NoActionBar" />
```

```diff
  <!-- app/src/main/res/values-night/themes.xml -->
- <style name="Theme.ElSeeker" parent="android:Theme.Material.NoActionBar">
-     <item name="android:statusBarColor">@color/surface_dark</item>
-     <item name="android:navigationBarColor">@color/surface_dark</item>
- </style>
+ <style name="Theme.ElSeeker" parent="android:Theme.Material.NoActionBar" />
```

**잔여 (SDK 측)**: Naver/Kakao SDK 내부의 deprecated 호출과 `SHORT_EDGES` 사용은 라이브러리 코드라 수정할 수 없다. **해당 SDK 버전 업 시 자연 해소**된다. (현재: `naver-sdk 5.10.0`, `kakao-sdk 2.20.6`) — 비차단 경고.

---

## ③ 대형 화면 크기/방향 제한

**경고**: "Android 16부터 폴더블·태블릿 등 대형 화면에서 크기/방향 제한이 무시된다." 감지된 제한:

```xml
<activity android:name="com.navercorp.nid.oauth.activity.NidOAuthCustomTabActivity"
          android:screenOrientation="BEHIND" />
```

**진단**: **Naver 로그인 SDK**가 자체 매니페스트에 선언한 액티비티의 방향 고정(`BEHIND`). 우리 소스가 아니라 병합된 라이브러리 매니페스트에서 비롯된다.

**대응**: 매니페스트 병합으로 해당 액티비티의 방향 제한을 `unspecified` 로 덮어썼다.

```xml
<!-- app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    ...
    <activity
        android:name="com.navercorp.nid.oauth.activity.NidOAuthCustomTabActivity"
        android:screenOrientation="unspecified"
        tools:node="merge"
        tools:replace="android:screenOrientation" />
    ...
</manifest>
```

- `tools:node="merge"` — 라이브러리의 기존 액티비티 선언에 속성만 덮어쓴다(중복 선언 아님)
- `tools:replace="android:screenOrientation"` — 충돌하는 `screenOrientation` 속성을 우리 값으로 교체

> ⚠️ **테스트 권장**: 라이브러리 액티비티 속성을 덮어썼으므로, 재빌드 후 **Naver 로그인 흐름**을 1회 실행해 회전·표시에 이상이 없는지 확인한다.

---

## 변경 파일 요약

| 파일 | 변경 |
|------|------|
| `app/src/main/res/values/themes.xml` | deprecated `statusBarColor` / `navigationBarColor` 제거 |
| `app/src/main/res/values-night/themes.xml` | deprecated `statusBarColor` / `navigationBarColor` 제거 |
| `app/src/main/AndroidManifest.xml` | `xmlns:tools` 추가, Naver 액티비티 방향 제한 오버라이드 |

## 후속 작업

1. Windows에서 **재빌드** 후 새 AAB 업로드 → ①③ 및 ②의 우리 측 경고 해소
2. **Naver 로그인 동작 테스트** (③ 오버라이드 영향 확인)
3. ②의 SDK 내부 잔여 경고는 **Naver/Kakao SDK 업데이트** 시 해소 — 추후 버전 점검 항목

## 관련 문서

- [release-signing-guide.md](release-signing-guide.md) — 서명/배포, 네이티브 디버그 기호
- [android-app-architecture.md](android-app-architecture.md) — 아키텍처, edge-to-edge·테마 설계
