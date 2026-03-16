package com.elseeker.android.webview

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.elseeker.android.BuildConfig

object WebViewSetup {

    fun configure(webView: WebView) {
        configureCookies(webView)
        configureSettings(webView)
    }

    private fun configureSettings(webView: WebView) {
        webView.settings.apply {
            // Required
            javaScriptEnabled = true
            domStorageEnabled = true

            // Display
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)

            // Security
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

            // Cache
            cacheMode = WebSettings.LOAD_DEFAULT

            // User-Agent
            userAgentString = "$userAgentString ElSeeker-Android/${BuildConfig.VERSION_NAME}"
        }
    }

    private fun configureCookies(webView: WebView) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, false)
    }
}
