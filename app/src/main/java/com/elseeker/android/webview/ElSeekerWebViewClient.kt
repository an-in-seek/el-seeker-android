package com.elseeker.android.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.SslErrorHandler
import android.net.http.SslError
import com.elseeker.android.BuildConfig

class ElSeekerWebViewClient(
    private val context: Context,
    private val onPageStarted: () -> Unit,
    private val onPageFinished: (String?) -> Unit,
    private val onError: (failedUrl: String?, errorCode: Int?) -> Unit,
    private val onSslError: () -> Unit,
    private val onOAuthStartedInCustomTab: () -> Unit = {}
) : WebViewClient() {

    private val baseUri: Uri = Uri.parse(BuildConfig.BASE_URL)

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val targetUri = request.url
        val host = targetUri.host.orEmpty()

        // Internal pages must match the configured origin exactly.
        if (isInternalUri(targetUri)) return false

        return when {
            // Kakao OAuth -> WebView (허용됨, WebView 쿠키 유지)
            isDomainOrSubdomain(host, "kakao.com") -> false

            // Naver OAuth -> WebView (허용됨, WebView 쿠키 유지)
            isDomainOrSubdomain(host, "nid.naver.com") -> false

            // Google OAuth -> Custom Tabs (Google 정책상 WebView 차단 대비)
            isDomainOrSubdomain(host, "accounts.google.com") -> {
                onOAuthStartedInCustomTab()
                openInCustomTab(targetUri)
            }

            // YouTube -> 앱 Intent (PiP, 백그라운드 재생 지원)
            isDomainOrSubdomain(host, "youtube.com") || isDomainOrSubdomain(host, "youtu.be") -> {
                openExternalApp(targetUri)
            }

            // 외부 링크 -> Custom Tabs (앱 내 브라우징 UX)
            else -> openInCustomTab(targetUri)
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)

        // Toggle third-party cookies for OAuth domains only
        view?.let { wv ->
            val host = url?.let { Uri.parse(it).host }.orEmpty()
            val isOAuthDomain = isDomainOrSubdomain(host, "accounts.google.com")
                    || isDomainOrSubdomain(host, "nid.naver.com")
                    || isDomainOrSubdomain(host, "kakao.com")
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv, isOAuthDomain)
        }

        onPageStarted()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished(url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        // Ignore sub-resource errors
        if (!request.isForMainFrame) return

        onError(request.url.toString(), error.errorCode)
    }

    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        handler.cancel() // Never proceed on SSL errors
        onSslError()
    }

    private fun openInCustomTab(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        when (scheme) {
            "http", "https" -> CustomTabsHelper.launch(context, uri)
            "intent" -> {
                try {
                    val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    context.startActivity(intent)
                } catch (_: Exception) { /* 처리 불가 intent URI 무시 */ }
            }
            else -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: ActivityNotFoundException) { /* 처리 앱 없음 */ }
            }
        }
        return true
    }

    private fun openExternalApp(uri: Uri): Boolean {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            // 앱이 없으면 Custom Tabs로 fallback
            CustomTabsHelper.launch(context, uri)
        }
        return true
    }

    private fun isInternalUri(uri: Uri): Boolean {
        val sameScheme = uri.scheme.equals(baseUri.scheme, ignoreCase = true)
        val sameHost = uri.host.equals(baseUri.host, ignoreCase = true)
        val samePort = uri.port == baseUri.port
        return sameScheme && sameHost && samePort
    }

    private fun isDomainOrSubdomain(host: String, domain: String): Boolean {
        return host.equals(domain, ignoreCase = true) ||
                host.endsWith(".$domain", ignoreCase = true)
    }
}
