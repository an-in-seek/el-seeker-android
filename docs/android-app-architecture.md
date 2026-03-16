# ElSeeker Android App - Architecture Design

## 1. Technology Stack

```
Language:        Kotlin
Min SDK:         26 (Android 8.0)
Target SDK:      35
UI Framework:    Jetpack Compose (native shell) + WebView (content)
Build System:    Gradle Kotlin DSL
Architecture:    Single Activity + ViewModel

Dependencies:
  androidx.webkit:webkit:1.12.x          -- Enhanced WebView APIs
  androidx.activity:activity-compose:1.9.x
  androidx.lifecycle:lifecycle-viewmodel-compose
  androidx.core:core-splashscreen:1.0.x
  com.google.android.play:app-update:2.1.x
  com.google.firebase:firebase-messaging:24.x   (Phase 3)
  androidx.browser:browser:1.8.x                 (Chrome Custom Tabs, Google OAuth)
```

---

## 2. App Architecture

### 2.1 Overview

```
ElSeekerActivity (Single Activity)
  |
  +-- ElSeekerViewModel
  |     +-- uiState: StateFlow<UiState>
  |     +-- connectivityState: StateFlow<Boolean>
  |     +-- pendingUrl: String?
  |
  +-- Compose UI
        +-- SplashScreen (Android 12+)
        +-- MainScreen
        |     +-- WebView (전체 웹 콘텐츠)
        |     +-- ErrorOverlay (네트워크 오류)
        +-- OfflineScreen (네트워크 없음)
```

### 2.2 Why Single Activity + WebView

- ElSeeker의 모든 페이지가 Thymeleaf SSR로 완성됨 -> 네이티브 화면 불필요
- 웹의 Bottom Tab Bar (`section-nav.html`)가 이미 모바일 최적화됨
- 네이티브 Bottom Nav 추가 시 웹 section-nav와 이중 표시 문제 발생
- WebView 내 페이지 전환이 SPA처럼 동작 (SSR이지만 전체 리로드)

### 2.3 UiState

```kotlin
sealed interface UiState {
    data object Loading : UiState
    data class Ready(val url: String) : UiState
    data object NoNetwork : UiState
    data class Error(val failedUrl: String?, val errorCode: Int?) : UiState
}
```

---

## 3. WebView Configuration

### 3.1 WebSettings

```kotlin
webView.settings.apply {
    // Required
    javaScriptEnabled = true                        // auth-check.js, 모든 인터랙션
    domStorageEnabled = true                        // sessionStorage (OAuth 플래그)
    databaseEnabled = true                          // localStorage (번역본 선택 등)

    // Display
    useWideViewPort = true
    loadWithOverview = true
    setSupportZoom(false)                           // 웹이 이미 반응형

    // Security
    allowFileAccess = false                         // 로컬 파일 차단
    allowContentAccess = false                      // content:// URI 차단
    mixedContentMode = MIXED_CONTENT_NEVER_ALLOW    // HTTPS 강제

    // Cache
    cacheMode = WebSettings.LOAD_DEFAULT            // 서버 Cache-Control 정책 따름

    // User-Agent
    userAgentString = "$userAgentString ElSeeker-Android/${BuildConfig.VERSION_NAME}"
}
```

### 3.2 Cookie Configuration

```kotlin
val cookieManager = CookieManager.getInstance()
cookieManager.setAcceptCookie(true)
cookieManager.setAcceptThirdPartyCookies(webView, true)

// 쿠키는 앱 종료 후에도 CookieManager에 자동 보존
// Access Token: 1시간 TTL (JwtRefreshFilter가 서버 사이드 자동 갱신)
// Refresh Token: 14일 TTL
```

### 3.3 WebView Debugging (Debug builds only)

```kotlin
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

---

## 4. URL Routing (shouldOverrideUrlLoading)

```kotlin
override fun shouldOverrideUrlLoading(
    view: WebView, request: WebResourceRequest
): Boolean {
    val url = request.url.toString()
    val host = request.url.host ?: ""

    // 1. 내부 페이지 -> WebView
    if (url.startsWith(BASE_URL)) return false

    // 2. OAuth 프로바이더 -> 분기 처리
    return when {
        // Kakao OAuth -> WebView 내부
        host.contains("kakao.com") -> false

        // Naver OAuth -> WebView 내부
        host.contains("nid.naver.com") -> false

        // Google OAuth -> Chrome Custom Tabs (embedded WebView 차단 정책)
        host.contains("accounts.google.com") -> {
            CustomTabsIntent.Builder().build()
                .launchUrl(context, request.url)
            true
        }

        // YouTube -> YouTube 앱 또는 시스템 브라우저
        host.contains("youtube.com") || host.contains("youtu.be") -> {
            startActivity(Intent(Intent.ACTION_VIEW, request.url))
            true
        }

        // 기타 외부 링크 -> 시스템 브라우저
        else -> {
            startActivity(Intent(Intent.ACTION_VIEW, request.url))
            true
        }
    }
}
```

---

## 5. OAuth2 Authentication

### 5.1 Flow Diagram

```
[Kakao/Naver - WebView 내부 처리]

User taps login button
  -> WebView loads /oauth2/authorization/kakao
  -> WebView navigates to kauth.kakao.com (login page)
  -> User completes login
  -> Redirect to /login/oauth2/code/kakao (server callback)
  -> OAuth2LoginSuccessHandler sets JWT cookies
  -> Redirect to / (home)
  -> CookieManager has Access + Refresh tokens
  -> Done (no native code involved)

[Google - Chrome Custom Tabs]

User taps Google login button
  -> WebView loads /oauth2/authorization/google
  -> shouldOverrideUrlLoading detects accounts.google.com
  -> Opens Chrome Custom Tabs
  -> User completes login in Custom Tab
  -> Redirect to /login/oauth2/code/google (server callback)
  -> Server sets JWT cookies + redirects to /
  -> Custom Tab loads / (홈)
  -> Problem: Cookie is in Chrome, not WebView

  Solution (Phase 2):
  -> Server detects mobile client (User-Agent)
  -> Returns elseeker://auth/callback?code=ONE_TIME_CODE
  -> App receives Intent, exchanges code via POST /api/v1/auth/exchange
  -> App receives tokens, sets cookies in CookieManager
```

### 5.2 Google OAuth Workaround (MVP)

MVP에서는 Google 로그인도 WebView 내부에서 시도하되, 차단될 경우 사용자에게 안내:

```kotlin
// Google OAuth 페이지 로드 실패 감지
override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
    if (request.url.toString().contains("accounts.google.com")) {
        // "Google 로그인은 Kakao 또는 Naver를 이용해주세요" 안내
        // 또는 Chrome Custom Tabs로 재시도
    }
}
```

### 5.3 Server-Side Mobile Detection

서버에서 WebView 앱 요청을 식별하기 위한 User-Agent 규약:

```
기존 WebView UA + " ElSeeker-Android/1.0"
```

서버 활용:
- `OAuth2LoginSuccessHandler`: 모바일 클라이언트 분기 (향후 auth code exchange)
- 로깅/분석: 앱 vs 웹 트래픽 구분
- 응답 최적화: 앱 전용 헤더/동작 (향후)

---

## 6. Hardware Back Button

```kotlin
// Activity에서 onBackPressed 대신 OnBackPressedCallback 사용
onBackPressedDispatcher.addCallback(this) {
    when {
        // WebView 히스토리가 있으면 뒤로 이동
        webView.canGoBack() -> webView.goBack()

        // 히스토리 없으면 앱 종료 확인
        else -> {
            // 2초 내 두 번 누르면 종료
            if (backPressedOnce) {
                finish()
            } else {
                backPressedOnce = true
                Toast.makeText(context, "뒤로가기를 한 번 더 누르면 종료됩니다", LENGTH_SHORT).show()
                handler.postDelayed({ backPressedOnce = false }, 2000)
            }
        }
    }
}
```

---

## 7. Error Handling

### 7.1 Network Layer

```kotlin
class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService<ConnectivityManager>()

    val isConnected: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }
        connectivityManager?.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager?.unregisterNetworkCallback(callback) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(), isCurrentlyConnected())
}
```

### 7.2 WebView Error Layer

```kotlin
override fun onReceivedError(
    view: WebView, request: WebResourceRequest, error: WebResourceError
) {
    if (!request.isForMainFrame) return  // 서브리소스 오류는 무시

    val errorType = when (error.errorCode) {
        ERROR_HOST_LOOKUP, ERROR_CONNECT -> ErrorType.NETWORK
        ERROR_TIMEOUT -> ErrorType.TIMEOUT
        ERROR_UNKNOWN -> ErrorType.UNKNOWN
        else -> ErrorType.OTHER
    }
    viewModel.setError(request.url.toString(), errorType)
}
```

### 7.3 Error Recovery

```kotlin
// ViewModel
fun retry() {
    val url = pendingUrl ?: BASE_URL
    _uiState.value = UiState.Ready(url)
}

// Compose ErrorScreen
@Composable
fun ErrorScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("네트워크 연결을 확인해주세요")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("다시 시도")
        }
    }
}
```

---

## 8. Splash Screen

```kotlin
class ElSeekerActivity : ComponentActivity() {
    private val viewModel: ElSeekerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        // 네트워크 확인 완료까지 스플래시 유지
        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value is UiState.Loading
        }

        super.onCreate(savedInstanceState)
        // ...
    }
}
```

시작 흐름:
```
Splash 표시 -> 네트워크 확인 (200ms)
  -> 있음: UiState.Ready(BASE_URL) -> WebView 로드 -> Splash 해제
  -> 없음: UiState.NoNetwork -> OfflineScreen 표시 -> Splash 해제
```

---

## 9. Deep Link / App Links (Phase 2)

### 9.1 AndroidManifest.xml

```xml
<activity android:name=".ElSeekerActivity"
          android:exported="true"
          android:launchMode="singleTask">

    <!-- App Links (HTTPS) -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https"
              android:host="elseeker.com"
              android:pathPrefix="/web/" />
        <data android:scheme="https"
              android:host="elseeker.com"
              android:path="/" />
    </intent-filter>

    <!-- Custom Scheme (OAuth callback) -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="elseeker"
              android:host="auth"
              android:path="/callback" />
    </intent-filter>
</activity>
```

### 9.2 Server: assetlinks.json

```json
// https://elseeker.com/.well-known/assetlinks.json
[{
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
        "namespace": "android_app",
        "package_name": "com.elseeker.android",
        "sha256_cert_fingerprints": ["XX:XX:..."]
    }
}]
```

### 9.3 Deep Link Handling

```kotlin
private fun handleIntent(intent: Intent) {
    val uri = intent.data ?: return

    when (uri.scheme) {
        "https" -> {
            // App Link: 경로를 WebView에 로드
            val path = uri.path ?: "/"
            val query = uri.query?.let { "?$it" } ?: ""
            webView.loadUrl("$BASE_URL$path$query")
        }
        "elseeker" -> {
            // Custom scheme: OAuth callback 등
            if (uri.host == "auth" && uri.path == "/callback") {
                val code = uri.getQueryParameter("code")
                if (code != null) exchangeAuthCode(code)
            }
        }
    }
}
```

---

## 10. FCM Push Notification (Phase 3)

### 10.1 App Side

```kotlin
class ElSeekerMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // 서버에 FCM 토큰 전송
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: return
        val body = message.data["body"] ?: return
        val deepLink = message.data["deepLink"] ?: "/"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }
}
```

### 10.2 Server Side (New)

```
POST /api/v1/members/fcm-token
  Body: { "token": "fcm-token-string", "platform": "ANDROID" }
  Auth: JWT required

Server stores token in member_device table
Server uses Firebase Admin SDK to send notifications
```

### 10.3 Notification Triggers

| Event | Notification | Deep Link |
|-------|-------------|-----------|
| 커뮤니티 댓글 | "내 글에 새 댓글이 달렸습니다" | `/web/community/{postId}` |
| 게임 랭킹 변동 | "축하합니다! 랭킹이 올랐습니다" | `/web/game/ranking` |
| 읽기 리마인더 | "오늘의 성경 읽기를 시작하세요" | `/web/bible/translation` |

---

## 11. Native Share Bridge (Phase 2)

`navigator.share()` API가 WebView에서 미지원될 경우 JS Bridge로 대체:

### 11.1 Android Side

```kotlin
class ElSeekerJsBridge(private val activity: Activity) {

    @JavascriptInterface
    fun share(title: String, text: String, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$text\n$url")
        }
        activity.startActivity(Intent.createChooser(intent, "공유하기"))
    }
}

// WebView에 브릿지 등록
webView.addJavascriptInterface(ElSeekerJsBridge(this), "ElSeekerApp")
```

### 11.2 Web Side Detection

```javascript
// verse-list.js, community-detail.js 등에서
if (navigator.share) {
    navigator.share({ title, text, url });
} else if (window.ElSeekerApp?.share) {
    window.ElSeekerApp.share(title, text, url);
} else {
    // clipboard fallback
    navigator.clipboard.writeText(text);
}
```

---

## 12. Security

### 12.1 WebView Security Checklist

| Item | Setting | Reason |
|------|---------|--------|
| JavaScript | Enabled | 필수 (인증, 인터랙션) |
| File Access | Disabled | 로컬 파일 접근 차단 |
| Content Access | Disabled | content:// URI 차단 |
| Mixed Content | Never Allow | HTTPS 강제 |
| JS Interface | 최소화 (share만) | XSS 공격 면적 최소화 |
| SSL Error | Always cancel | handler.proceed() 절대 금지 |
| Debug | BuildConfig.DEBUG only | Release에서 비활성화 |

### 12.2 SSL Error Handling

```kotlin
override fun onReceivedSslError(
    view: WebView, handler: SslErrorHandler, error: SslError
) {
    handler.cancel()  // 절대 proceed() 하지 않음
    viewModel.setSslError()
}
```

### 12.3 URL Whitelist

WebView에서 로드 허용하는 도메인:

```
elseeker.com (프로덕션)
localhost:8080 (개발)
kauth.kakao.com, accounts.kakao.com
nid.naver.com
accounts.google.com (Custom Tabs로 분리)
img.youtube.com (썸네일만)
```

---

## 13. Project Structure

```
app/
  src/main/
    java/com/elseeker/android/
      ElSeekerApplication.kt
      ElSeekerActivity.kt
      ElSeekerViewModel.kt
      webview/
        ElSeekerWebViewClient.kt
        ElSeekerWebChromeClient.kt
        WebViewSetup.kt
      network/
        NetworkMonitor.kt
      bridge/
        ElSeekerJsBridge.kt          (Phase 2)
      fcm/
        ElSeekerMessagingService.kt   (Phase 3)
      ui/
        theme/
          Theme.kt
          Color.kt
        screen/
          MainScreen.kt
          ErrorScreen.kt
          OfflineScreen.kt
    res/
      drawable/
        ic_launcher.xml
        ic_notification.xml
      values/
        strings.xml
        colors.xml
        themes.xml
      mipmap/
        ic_launcher/
  build.gradle.kts
```

---

## 14. Build Variants

```kotlin
// build.gradle.kts
android {
    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
            isDebuggable = true
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://elseeker.com\"")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

| Variant | BASE_URL | Debug | Minify |
|---------|----------|-------|--------|
| debug | `http://10.0.2.2:8080` (에뮬레이터 localhost) | true | false |
| release | `https://elseeker.com` | false | true |

---

## 15. Key Decisions Log

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Architecture | Single Activity + WebView | 웹이 이미 완성됨, 네이티브 화면 불필요 |
| Bottom Nav | 웹 section-nav 사용 | 네이티브 추가 시 이중 표시 문제 |
| Kakao/Naver OAuth | WebView 내부 | 서버 변경 없이 동작, 쿠키 자동 관리 |
| Google OAuth | Chrome Custom Tabs | Google embedded WebView 차단 정책 |
| Offline | 에러 화면 (MVP), 캐시 (Phase 2) | MVP 복잡도 최소화 |
| Push | Phase 3 | Play Store 승인 후 추가 |
| Min SDK | 26 (8.0) | 95%+ 기기 커버, WebView ES6 module 호환 |
