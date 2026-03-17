# ElSeeker ProGuard Rules

# Keep WebView JavaScript interface methods
-keepclassmembers class com.elseeker.android.bridge.ElSeekerJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep BuildConfig
-keep class com.elseeker.android.BuildConfig { *; }

# Google Play Services Auth (consumer rules 보완)
-keep class com.google.android.gms.auth.api.signin.** { *; }
