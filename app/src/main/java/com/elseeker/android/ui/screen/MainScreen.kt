package com.elseeker.android.ui.screen

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.elseeker.android.ElSeekerViewModel
import com.elseeker.android.UiState
import com.elseeker.android.webview.ElSeekerWebChromeClient
import com.elseeker.android.webview.ElSeekerWebViewClient
import com.elseeker.android.webview.WebViewSetup

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MainScreen(
    viewModel: ElSeekerViewModel,
    onBackPressed: (canGoBack: Boolean, goBack: () -> Unit) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val isReady = uiState is UiState.Ready

    BackHandler {
        val wv = webView
        if (wv != null) {
            onBackPressed(wv.canGoBack()) { wv.goBack() }
        } else {
            onBackPressed(false) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        // WebView is always in composition once created (survives NoNetwork/Error states)
        if (uiState !is UiState.Loading) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        WebViewSetup.configure(this)

                        webViewClient = ElSeekerWebViewClient(
                            context = ctx,
                            onPageStarted = { isLoading = true },
                            onPageFinished = { url ->
                                isLoading = false
                                loadingProgress = 100
                                viewModel.onPageLoaded(url)
                            },
                            onError = { failedUrl, errorCode ->
                                viewModel.setError(failedUrl, errorCode)
                            },
                            onSslError = {
                                viewModel.setError(null, null)
                            }
                        )

                        this@apply.webChromeClient = ElSeekerWebChromeClient(
                            onProgressChanged = { progress ->
                                loadingProgress = progress
                            }
                        )

                        webView = this
                    }
                },
                update = { wv ->
                    wv.setOnTouchListener { _, _ -> !isReady }
                    if (uiState is UiState.Ready) {
                        val targetUrl = (uiState as UiState.Ready).url
                        if (wv.url != targetUrl) {
                            isLoading = true
                            loadingProgress = 0
                            wv.loadUrl(targetUrl)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isReady) 1f else 0f)
            )

            // Loading progress bar
            AnimatedVisibility(
                visible = isReady && isLoading && loadingProgress < 100,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                LinearProgressIndicator(
                    progress = { loadingProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }

        // Overlay screens on top of WebView
        when (uiState) {
            is UiState.Loading -> {
                // Splash screen handles this state
            }
            is UiState.Ready -> {
                // WebView is visible above
            }
            is UiState.NoNetwork -> {
                OfflineScreen(onRetry = { viewModel.retry() })
            }
            is UiState.Error -> {
                ErrorScreen(onRetry = { viewModel.retry() })
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }
}
