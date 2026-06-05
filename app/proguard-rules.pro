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
