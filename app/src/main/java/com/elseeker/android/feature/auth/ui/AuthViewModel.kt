package com.elseeker.android.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.auth.AuthState
import com.elseeker.android.core.auth.SessionManager
import com.elseeker.android.core.network.ApiException
import com.elseeker.android.feature.auth.domain.AuthRepository
import com.elseeker.android.feature.auth.domain.LoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 로그인/동의 화면이 1회성으로 소비하는 이벤트(토스트·다이얼로그). */
sealed interface AuthUiEvent {
    data class Message(val text: String) : AuthUiEvent

    /** 사용자가 명시적으로 로그아웃함 → Activity 가 소셜 SDK 세션도 정리(계정 전환 가능). */
    data object SocialLogout : AuthUiEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    val authState: StateFlow<AuthState> = sessionManager.authState

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _events = Channel<AuthUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Activity 가 소셜 SDK 로 토큰을 받아 호출. 결과 라우팅은 authState 변화로 자동 처리. */
    fun onSocialToken(provider: String, token: String) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                when (val result = authRepository.socialLogin(provider, token)) {
                    LoginResult.Authenticated,
                    LoginResult.NeedsConsent -> Unit // authState 가 갱신되어 네비게이션이 반응
                    is LoginResult.Error -> emit(result.exception)
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun submitConsent(agreeTerms: Boolean, agreePrivacy: Boolean, ageOver14: Boolean) {
        if (_busy.value) return
        if (!(agreeTerms && agreePrivacy && ageOver14)) {
            send(AuthUiEvent.Message("필수 약관에 모두 동의해 주세요."))
            return
        }
        _busy.value = true
        viewModelScope.launch {
            authRepository.submitConsent(agreeTerms, agreePrivacy, ageOver14)
                .onFailure { e ->
                    val api = e as? ApiException
                    val text = when {
                        // 서버 폼 검증(400 CONSENT_REQUIRED): 필수 약관 미동의 — 폼 오류로 안내.
                        api?.isConsentRequired == true -> api.message.ifBlank { "필수 약관에 모두 동의해 주세요." }
                        else -> api?.message ?: "동의 처리에 실패했습니다."
                    }
                    send(AuthUiEvent.Message(text))
                }
            _busy.value = false
        }
    }

    fun cancelConsent() {
        viewModelScope.launch {
            authRepository.cancelConsent()
                .onSuccess { send(AuthUiEvent.SocialLogout) }
                .onFailure { send(AuthUiEvent.Message((it as? ApiException)?.message ?: "취소 처리에 실패했습니다.")) }
        }
    }

    /** 로컬 세션 폐기 + 소셜 SDK 세션 정리 요청(이벤트). */
    fun logout() {
        authRepository.logout()
        send(AuthUiEvent.SocialLogout)
    }

    private fun emit(e: ApiException) {
        val text = when {
            e.code == ApiException.CODE_SOCIAL_LOGIN_INVALID_TOKEN -> "소셜 인증에 실패했습니다. 다시 시도해 주세요."
            e.isNetwork -> "네트워크 연결을 확인해 주세요."
            else -> e.message
        }
        send(AuthUiEvent.Message(text))
    }

    private fun send(event: AuthUiEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
