package com.elseeker.android.core.network

/**
 * 서버 에러 응답 또는 네트워크/파싱 실패를 담는 단일 예외 타입.
 *
 * 호출자는 사람이 읽는 [message] 가 아니라 [status]/[code] 로 분기한다.
 * - [status] : HTTP 상태 코드. 네트워크 실패는 [STATUS_NETWORK], 응답 파싱 실패는 [STATUS_PARSE].
 * - [code]   : 백엔드 `ErrorType.name`. Security 예외 등에서는 null 일 수 있다.
 */
class ApiException(
    val status: Int,
    val code: String?,
    override val message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    val isNetwork: Boolean get() = status == STATUS_NETWORK
    val isUnauthorized: Boolean get() = status == 401

    /** signup token 으로 일반 API 접근 → 동의 화면으로 라우팅해야 하는 상태(PRD §8). */
    val isConsentRequired: Boolean
        get() = code == CODE_CONSENT_REQUIRED && status == 403

    companion object {
        const val STATUS_NETWORK = -1
        const val STATUS_PARSE = -2

        const val CODE_CONSENT_REQUIRED = "CONSENT_REQUIRED"
        const val CODE_SOCIAL_LOGIN_INVALID_TOKEN = "SOCIAL_LOGIN_INVALID_TOKEN"
        const val CODE_AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED"
        const val CODE_OAUTH_ACCOUNT_ALREADY_LINKED = "OAUTH_ACCOUNT_ALREADY_LINKED"
        const val CODE_NICKNAME_ALREADY_EXISTS = "NICKNAME_ALREADY_EXISTS"
        const val CODE_INVALID_NICKNAME_FORMAT = "INVALID_NICKNAME_FORMAT"
    }
}
