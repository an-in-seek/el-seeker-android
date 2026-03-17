package com.elseeker.android

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    data object Success : LoginEvent
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

    fun handleSocialLogin(provider: String, socialToken: String) {
        if (_isLoggingIn.value) return
        _isLoggingIn.value = true
        Log.d(TAG, "Social login API call - provider: $provider, tokenLength: ${socialToken.length}")
        viewModelScope.launch {
            val result = AuthApi.socialLogin(provider, socialToken)
            _isLoggingIn.value = false
            result.fold(
                onSuccess = { tokenResponse ->
                    Log.d(TAG, "Social login API success - accessToken length: ${tokenResponse.accessToken.length}")
                    tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                    CookieHelper.setAuthCookies(tokenResponse.accessToken, tokenResponse.refreshToken)
                    _loginEvent.send(LoginEvent.Success)
                },
                onFailure = { error ->
                    Log.e(TAG, "Social login API failed: ${error.message}", error)
                    _loginEvent.send(LoginEvent.Error(error.message ?: "로그인에 실패했습니다."))
                }
            )
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
