# ElSeeker ProGuard Rules

# Keep WebView JavaScript interface methods
-keepclassmembers class com.elseeker.android.bridge.ElSeekerJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep BuildConfig
-keep class com.elseeker.android.BuildConfig { *; }

# Google Play Services Auth (consumer rules 보완)
-keep class com.google.android.gms.auth.api.signin.** { *; }

# release 빌드에서 인증 흐름 등의 Log.v/d/i 호출 제거 (Log.w/e는 유지).
# 토큰 값은 애초에 로깅하지 않지만, 부수적 정보 노출을 release에서 차단한다.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# OkHttp (play-app-update / gms-auth 등을 통해 간접 포함)이 참조하는
# 선택적 TLS 프로바이더 클래스들. Android 런타임에는 존재하지 않으므로
# R8 경고를 무시한다 (런타임 동작에는 영향 없음).
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
