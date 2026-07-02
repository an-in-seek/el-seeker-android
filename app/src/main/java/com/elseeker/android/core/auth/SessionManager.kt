package com.elseeker.android.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 전역 인증 상태. 네비게이션 게이트의 단일 기준(PRD §5.4). */
enum class AuthState {
    /** 콜드 스타트 직후, 아직 복원 판정 전. */
    Unknown,

    /** 로그인 + 동의 완료(/me.status==ACTIVE). 메인 진입 가능. */
    Authenticated,

    /** signup token 보유 또는 status==PENDING_CONSENT. 동의 화면으로 라우팅. */
    NeedsConsent,

    /** 토큰 없음/만료. 로그인 화면으로 라우팅. */
    Unauthenticated,

    /** 토큰은 있으나 복원 중 네트워크 오류. 토큰 유지 + 오프라인 화면에서 재시도(로그아웃 아님). */
    Offline,
}

/**
 * 인메모리 인증 상태 보관 + 토큰 영속화 위임.
 *
 * 토큰 저장/삭제는 항상 이곳을 통해 일어나도록 해 [authState] 가 항상 저장소와 일치하게 만든다.
 * [TokenAuthenticator] 는 refresh 실패(하드 401) 시 [invalidate] 를 호출한다.
 */
@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: AuthTokenStore,
) {
    private val _authState = MutableStateFlow(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val accessToken: String? get() = tokenStore.accessToken
    val refreshToken: String? get() = tokenStore.refreshToken
    val isSignupSession: Boolean get() = tokenStore.isSignupSession

    fun hasSession(): Boolean = tokenStore.hasSession

    /** 정식 로그인/재발급/동의 활성화로 정식 토큰을 수령했을 때. */
    fun onAuthenticated(accessToken: String, refreshToken: String?) {
        tokenStore.save(accessToken, refreshToken)
        _authState.value = AuthState.Authenticated
    }

    /** 신규 가입자: signup token 만 수령(refresh 없음) → 동의 필요. 이전 refresh 는 폐기. */
    fun onNeedsConsent(signupToken: String) {
        tokenStore.saveSignup(signupToken)
        _authState.value = AuthState.NeedsConsent
    }

    /** 이미 저장된 토큰을 기준으로 상태만 갱신(세션 복원 판정 결과 반영). */
    fun setState(state: AuthState) {
        _authState.value = state
    }

    /** 로그아웃/탈퇴/하드 401 → 토큰 폐기 후 미인증. */
    fun invalidate() {
        tokenStore.clear()
        _authState.value = AuthState.Unauthenticated
    }
}
