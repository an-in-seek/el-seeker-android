package com.elseeker.android

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.auth.ApiException
import com.elseeker.android.auth.AuthApi
import com.elseeker.android.auth.CookieHelper
import com.elseeker.android.auth.TokenManager
import com.elseeker.android.network.NetworkMonitor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Loading : UiState
    data class Ready(val url: String) : UiState
    data object NoNetwork : UiState
    data class Error(val failedUrl: String?, val errorCode: Int?) : UiState
}

sealed interface LoginEvent {
    /** 로그인 성공 — WebView를 재로드해 인증 상태 반영. */
    data object Success : LoginEvent
    /** 연동 성공 — 토큰 변경 없이 연동 상태만 갱신. */
    data object LinkSuccess : LoginEvent
    data class Error(val message: String) : LoginEvent
}

class ElSeekerViewModel(application: Application) : AndroidViewModel(application) {

    private val networkMonitor = NetworkMonitor(application, viewModelScope)
    private val tokenManager = TokenManager(application)
    private var currentUrl: String = BuildConfig.BASE_URL

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val isConnected: StateFlow<Boolean> = networkMonitor.isConnected

    private val _loginEvent = Channel<LoginEvent>(Channel.BUFFERED)
    val loginEvent = _loginEvent.receiveAsFlow()

    private var pendingUrl: String? = null

    init {
        // 앱 시작 시 저장된 토큰이 있으면 WebView 쿠키에 설정
        restoreAuthCookies()

        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _uiState.update { current ->
                    when (current) {
                        is UiState.Loading -> {
                            if (connected) UiState.Ready(currentUrl) else UiState.NoNetwork
                        }
                        is UiState.Ready -> {
                            if (!connected) {
                                pendingUrl = currentUrl
                                UiState.NoNetwork
                            } else {
                                current
                            }
                        }
                        is UiState.NoNetwork -> {
                            if (connected) {
                                val url = pendingUrl ?: currentUrl
                                UiState.Ready(url)
                            } else {
                                current
                            }
                        }
                        is UiState.Error -> {
                            if (connected) {
                                val url = current.failedUrl ?: pendingUrl ?: currentUrl
                                UiState.Ready(url)
                            } else {
                                current
                            }
                        }
                    }
                }
            }
        }
    }

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    fun handleSocialLogin(provider: String, socialToken: String, isLink: Boolean) {
        // 원자적 compare-and-set으로 동시/중복 진입 차단.
        if (!_isLoggingIn.compareAndSet(expect = false, update = true)) return
        Log.i(TAG, "Social ${if (isLink) "link" else "login"} API call - provider: $provider, tokenLength: ${socialToken.length}")
        viewModelScope.launch {
            try {
                if (isLink) linkAccount(provider, socialToken) else login(provider, socialToken)
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    private suspend fun login(provider: String, socialToken: String) {
        AuthApi.socialLogin(provider, socialToken).fold(
            onSuccess = { tokenResponse ->
                Log.i(TAG, "Social login success - accessToken length: ${tokenResponse.accessToken.length}")
                tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                CookieHelper.setAuthCookies(tokenResponse.accessToken, tokenResponse.refreshToken)
                _loginEvent.send(LoginEvent.Success)
            },
            onFailure = { error ->
                Log.e(TAG, "Social login failed: ${error.message}", error)
                _loginEvent.send(LoginEvent.Error(error.message ?: "로그인에 실패했습니다."))
            }
        )
    }

    private suspend fun linkAccount(provider: String, socialToken: String) {
        val accessToken = tokenManager.getAccessToken()
        if (accessToken == null) {
            // 비로그인 상태에서의 연동 시도 → 재로그인 유도
            _loginEvent.send(LoginEvent.Error("로그인이 필요합니다. 다시 로그인해 주세요."))
            return
        }

        var result = AuthApi.linkSocialAccount(provider, socialToken, accessToken)

        // accessToken 만료(401) → reissue 후 1회 재시도
        if ((result.exceptionOrNull() as? ApiException)?.status == 401) {
            Log.i(TAG, "Link got 401 - attempting token reissue")
            val refreshed = reissueAccessToken()
            if (refreshed == null) {
                _loginEvent.send(LoginEvent.Error("세션이 만료되었습니다. 다시 로그인해 주세요."))
                return
            }
            result = AuthApi.linkSocialAccount(provider, socialToken, refreshed)
        }

        result.fold(
            onSuccess = {
                Log.i(TAG, "Social link success - provider: $provider")
                _loginEvent.send(LoginEvent.LinkSuccess)
            },
            onFailure = { error ->
                Log.e(TAG, "Social link failed: ${error.message}", error)
                _loginEvent.send(LoginEvent.Error(linkErrorMessage(error)))
            }
        )
    }

    /** 저장된 refreshToken으로 토큰 재발급. 성공 시 신규 accessToken 반환, 실패 시 null. */
    private suspend fun reissueAccessToken(): String? {
        val refreshToken = tokenManager.getRefreshToken() ?: return null
        return AuthApi.reissue(refreshToken).fold(
            onSuccess = { tokenResponse ->
                tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                CookieHelper.setAuthCookies(tokenResponse.accessToken, tokenResponse.refreshToken)
                tokenResponse.accessToken
            },
            onFailure = {
                Log.e(TAG, "Token reissue failed: ${it.message}")
                null
            }
        )
    }

    private fun linkErrorMessage(error: Throwable): String {
        val api = error as? ApiException
        return when {
            api?.code == "OAUTH_ACCOUNT_ALREADY_LINKED" -> "이미 다른 계정에 연결된 소셜 계정입니다."
            api?.status == 401 -> "세션이 만료되었습니다. 다시 로그인해 주세요."
            else -> error.message ?: "연동에 실패했습니다."
        }
    }

    companion object {
        private const val TAG = "ElSeekerAuth"
    }

    fun setError(failedUrl: String?, errorCode: Int?) {
        pendingUrl = failedUrl ?: currentUrl
        _uiState.update { UiState.Error(failedUrl, errorCode) }
    }

    fun setNoNetwork() {
        pendingUrl = currentUrl
        _uiState.update { UiState.NoNetwork }
    }

    fun retry() {
        if (!isConnected.value) {
            _uiState.update { UiState.NoNetwork }
            return
        }
        val url = pendingUrl ?: currentUrl
        pendingUrl = null
        _uiState.update { UiState.Ready(url) }
    }

    fun onPageLoaded(url: String?) {
        if (!url.isNullOrBlank()) {
            currentUrl = url
        }
        pendingUrl = null
    }

    private fun restoreAuthCookies() {
        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()
        if (accessToken != null && refreshToken != null) {
            CookieHelper.setAuthCookies(accessToken, refreshToken)
        }
    }
}
