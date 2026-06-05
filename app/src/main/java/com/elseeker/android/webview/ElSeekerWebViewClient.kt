package com.elseeker.android.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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
    private val onSocialLoginRequested: (provider: String, isLink: Boolean) -> Unit = { _, _ -> }
) : WebViewClient() {

    companion object {
        private const val TAG = "ElSeekerAuth"
    }

    private val baseUri: Uri = Uri.parse(BuildConfig.BASE_URL)

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val targetUri = request.url
        val host = targetUri.host.orEmpty()

        Log.i(TAG, "shouldOverrideUrlLoading: $targetUri (internal: ${isInternalUri(targetUri)})")

        if (isInternalUri(targetUri)) {
            val path = targetUri.path.orEmpty()
            Log.i(TAG, "Internal URL path: $path")
            if (path.startsWith("/oauth2/authorization/")) {
                val provider = path.substringAfterLast("/")
                // 마이페이지 연동 버튼은 ?link=true 를 실어 보낸다 (웹과 동일 신호).
                val isLink = targetUri.getQueryParameter("link") == "true"
                Log.i(TAG, "OAuth intercept -> provider: $provider, link: $isLink")
                if (provider in listOf("google", "kakao", "naver")) {
                    onSocialLoginRequested(provider, isLink)
                    return true
                }
            }
            return false
        }

        return when {
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
        if (!request.isForMainFrame) return
        onError(request.url.toString(), error.errorCode)
    }

    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        handler.cancel()
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
                } catch (_: Exception) { /* intent URI 처리 불가 */ }
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
