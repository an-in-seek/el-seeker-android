package com.elseeker.android.feature.auth.domain

import com.elseeker.android.core.auth.AuthState
import com.elseeker.android.core.auth.SessionManager
import com.elseeker.android.core.network.ApiException
import com.elseeker.android.core.network.orThrow
import com.elseeker.android.core.network.safeApiCall
import com.elseeker.android.feature.auth.data.AuthApi
import com.elseeker.android.feature.auth.data.AuthMeResponse
import com.elseeker.android.feature.auth.data.ConsentRequest
import com.elseeker.android.feature.auth.data.MemberUpdateRequest
import com.elseeker.android.feature.auth.data.SocialIntent
import com.elseeker.android.feature.auth.data.SocialLoginRequest
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** 소셜 로그인 결과. UI 라우팅 분기에 사용. */
sealed interface LoginResult {
    /** 정식 토큰 수령(동의 완료 사용자). 메인 진입. */
    data object Authenticated : LoginResult

    /** 신규 가입자: signup token 만 수령. 동의 화면으로. */
    data object NeedsConsent : LoginResult

    data class Error(val exception: ApiException) : LoginResult
}

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
    private val json: Json,
) {

    /**
     * 콜드 스타트 세션 복원(PRD §5.4).
     * 메인 진입 게이트 = scope!=SIGNUP AND /me.status==ACTIVE.
     */
    suspend fun restoreSession(): AuthState {
        if (!sessionManager.hasSession()) {
            sessionManager.setState(AuthState.Unauthenticated)
            return AuthState.Unauthenticated
        }
        // 1) signup 토큰이면 서버 호출 없이 즉시 동의 화면으로.
        if (sessionManager.isSignupSession) {
            sessionManager.setState(AuthState.NeedsConsent)
            return AuthState.NeedsConsent
        }
        // 2) 일반 토큰이면 /me 의 status 로 분기. 401(만료+재발급 실패)은 미인증.
        return try {
            val me = safeApiCall(json) { authApi.me() }
            val state = if (me.isActive) AuthState.Authenticated else AuthState.NeedsConsent
            sessionManager.setState(state)
            state
        } catch (e: ApiException) {
            if (e.isConsentRequired) {
                // signup 토큰이 로컬 파싱 실패로 일반 토큰처럼 취급된 경우 등:
                // /me 가 403 CONSENT_REQUIRED 를 주면 동의 화면으로(인터셉터가 설정한 상태 보존).
                sessionManager.setState(AuthState.NeedsConsent)
                AuthState.NeedsConsent
            } else if (e.isUnauthorized) {
                sessionManager.invalidate()
                AuthState.Unauthenticated
            } else {
                // 네트워크/일시적 오류: 토큰을 폐기하지 않고 Offline 로 두어 재시도하게 한다.
                // (유효 사용자를 오프라인 상태에서 로그인 화면으로 내쫓지 않음 — PRD §5.4)
                sessionManager.setState(AuthState.Offline)
                AuthState.Offline
            }
        }
    }

    suspend fun socialLogin(provider: String, token: String): LoginResult {
        return try {
            val response = safeApiCall(json) {
                authApi.socialLogin(SocialLoginRequest(provider, token, SocialIntent.LOGIN))
            }
            if (response.consentRequired || response.refreshToken == null) {
                sessionManager.onNeedsConsent(response.accessToken)
                LoginResult.NeedsConsent
            } else {
                sessionManager.onAuthenticated(response.accessToken, response.refreshToken)
                LoginResult.Authenticated
            }
        } catch (e: ApiException) {
            LoginResult.Error(e)
        }
    }

    /** 기존 로그인 사용자에 소셜 계정 추가 연동(Bearer). 성공 시 회원 정보 반환. */
    suspend fun linkSocialAccount(provider: String, token: String): Result<AuthMeResponse> =
        runCatching {
            safeApiCall(json) {
                authApi.linkSocialAccount(SocialLoginRequest(provider, token, SocialIntent.LINK))
            }
        }

    /**
     * 약관 동의 제출(PRD §5.3). 신규 활성화 시 정식 토큰을 본문으로 받아 저장한다.
     * 멱등 재호출(이미 활성)이면 토큰이 없어도 기존 토큰을 유지하고 인증 상태로 본다.
     */
    suspend fun submitConsent(
        agreeTerms: Boolean,
        agreePrivacy: Boolean,
        ageOver14: Boolean,
    ): Result<Unit> = runCatching {
        val response = safeApiCall(json) {
            authApi.consent(ConsentRequest(agreeTerms, agreePrivacy, ageOver14))
        }
        if (response.accessToken != null) {
            sessionManager.onAuthenticated(response.accessToken, response.refreshToken)
        } else if (sessionManager.isSignupSession) {
            // signup 토큰만 가진 상태에서 정식 토큰을 못 받음(직전 응답 유실 등).
            // refresh 가 없어 /reissue 불가 → 소셜 재로그인으로만 복구 가능(PRD §5.3).
            sessionManager.invalidate()
            throw ApiException(
                status = ApiException.STATUS_PARSE,
                code = null,
                message = "동의 처리가 완료되지 않았습니다. 다시 로그인해 주세요.",
            )
        } else {
            // 이미 정식 토큰 보유(멱등 재호출) → 기존 토큰 유지.
            sessionManager.setState(AuthState.Authenticated)
        }
    }

    /** 동의 취소: 서버의 PENDING_CONSENT 회원 삭제 + 로컬 세션 폐기. */
    suspend fun cancelConsent(): Result<Unit> = runCatching {
        safeApiCall(json) { authApi.cancelConsent().orThrow() }
        sessionManager.invalidate()
    }

    suspend fun me(): Result<AuthMeResponse> =
        runCatching { safeApiCall(json) { authApi.me() } }

    suspend fun oauthAccounts(memberUid: String) =
        runCatching { safeApiCall(json) { authApi.oauthAccounts(memberUid) } }

    suspend fun unlinkOauthAccount(memberUid: String, provider: String, providerUserId: String) =
        runCatching {
            safeApiCall(json) { authApi.unlinkOauthAccount(memberUid, provider, providerUserId) }
        }

    suspend fun updateProfile(memberUid: String, nickname: String, profileImageUrl: String?) =
        runCatching {
            safeApiCall(json) {
                authApi.updateMember(memberUid, MemberUpdateRequest(nickname, profileImageUrl))
            }
        }

    suspend fun deleteMember(memberUid: String): Result<Unit> = runCatching {
        safeApiCall(json) { authApi.deleteMember(memberUid).orThrow() }
        sessionManager.invalidate()
    }

    /** 로그아웃: 로컬 토큰/상태 폐기. 소셜 SDK 세션 정리는 UI(Activity) 에서 수행. */
    fun logout() = sessionManager.invalidate()
}
