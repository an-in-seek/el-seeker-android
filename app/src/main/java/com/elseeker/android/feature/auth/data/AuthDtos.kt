package com.elseeker.android.feature.auth.data

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------ *
 * 인증/회원 DTO — 백엔드 계약 정본(Swagger/소스) 기준.
 * Instant 필드는 ISO-8601 문자열로 받는다(커스텀 시리얼라이저 회피).
 * ------------------------------------------------------------------ */

/** provider 와이어 값(소문자). 백엔드 OAuthProvider.registrationId 와 일치. */
object SocialProvider {
    const val GOOGLE = "google"
    const val KAKAO = "kakao"
    const val NAVER = "naver"
}

object SocialIntent {
    const val LOGIN = "login"
    const val LINK = "link"
}

@Serializable
data class SocialLoginRequest(
    val provider: String,
    val token: String,
    val intent: String? = null,
)

/** intent=login 응답. consentRequired=true 면 refreshToken 은 null(=signup token). */
@Serializable
data class SocialLoginResponse(
    val consentRequired: Boolean = false,
    val accessToken: String,
    val refreshToken: String? = null,
)

@Serializable
data class ReissueRequest(
    val refreshToken: String,
)

/** 재발급 응답. 서버는 refresh 를 회전하지 않으므로 입력값과 동일 refreshToken 을 돌려준다. */
@Serializable
data class ReissueResponse(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class ConsentRequest(
    val agreeTerms: Boolean,
    val agreePrivacy: Boolean,
    val ageOver14: Boolean,
)

/** 동의 응답. Bearer(모바일) + 신규 활성화 시에만 access/refresh 가 채워진다. */
@Serializable
data class ConsentResponse(
    val redirectTo: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

@Serializable
data class AuthMeResponse(
    val memberUid: String,
    val email: String,
    val role: String,
    val nickname: String = "",
    val profileImageUrl: String? = null,
    val provider: String = "",
    val status: String,
    val createdAt: String? = null,
) {
    val isActive: Boolean get() = status == STATUS_ACTIVE
    val isPendingConsent: Boolean get() = status == STATUS_PENDING_CONSENT

    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_PENDING_CONSENT = "PENDING_CONSENT"
    }
}

@Serializable
data class MemberOAuthAccountResponse(
    val provider: String,
    val providerUserId: String,
    val email: String? = null,
    val nickname: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class MemberUpdateRequest(
    val nickname: String,
    val profileImageUrl: String? = null,
)
