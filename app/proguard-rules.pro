# ElSeeker ProGuard Rules

# Keep WebView JavaScript interface methods
-keepclassmembers class com.elseeker.android.bridge.ElSeekerJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep BuildConfig
-keep class com.elseeker.android.BuildConfig { *; }

# Kakao SDK
-keep class com.kakao.sdk.** { *; }
-dontwarn com.kakao.sdk.**

# Naver SDK
-keep class com.navercorp.nid.** { *; }
-dontwarn com.navercorp.nid.**

# Google Play Services Auth
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
