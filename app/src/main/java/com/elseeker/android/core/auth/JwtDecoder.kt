package com.elseeker.android.core.auth

import android.util.Base64
import org.json.JSONObject

/**
 * JWT payload 의 클레임만 로컬에서 읽어내는 경량 디코더(서명 검증 없음).
 *
 * 세션 복원 시 서버 호출 없이 토큰 종류를 판별하기 위한 용도다(PRD §5.4).
 * 백엔드 `JwtProvider` 기준: signup token 은 `scope=="SIGNUP"`, `typ=="signup"`.
 * 일반 access token 은 `scope` 클레임이 없다(null).
 */
object JwtDecoder {

    private const val CLAIM_SCOPE = "scope"
    private const val SCOPE_SIGNUP = "SIGNUP"

    /** signup(동의 전용) 토큰이면 true. 파싱 실패 시 false(보수적으로 일반 토큰 취급 후 /me 로 재확인). */
    fun isSignupToken(token: String?): Boolean {
        return readClaim(token, CLAIM_SCOPE) == SCOPE_SIGNUP
    }

    fun readClaim(token: String?, name: String): String? {
        val payload = decodePayload(token) ?: return null
        return payload.optString(name).takeIf { it.isNotBlank() }
    }

    private fun decodePayload(token: String?): JSONObject? {
        if (token.isNullOrBlank()) return null
        val parts = token.split(".")
        if (parts.size < 2) return null
        return try {
            val bytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }
}
