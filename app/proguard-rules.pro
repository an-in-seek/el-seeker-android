# ElSeeker ProGuard Rules

# Keep WebView JavaScript interface methods
-keepclassmembers class com.elseeker.android.bridge.ElSeekerJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep BuildConfig
-keep class com.elseeker.android.BuildConfig { *; }
