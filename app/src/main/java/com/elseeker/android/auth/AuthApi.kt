package com.elseeker.android.auth

import com.elseeker.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)

/** intent="link" 성공 시 서버가 내려주는 본문 (토큰 없음). */
data class AuthMeResponse(
    val memberUid: String?,
    val email: String?,
    val nickname: String?,
    val oauthAccounts: List<String>
)

/**
 * 서버 에러 응답({status, code, message}) 또는 응답 처리 실패를 담는 예외.
 * 호출자는 한글 [message]가 아니라 [status]/[code]로 분기한다.
 * 네트워크/파싱 원인은 [cause]에 보존한다.
 */
class ApiException(
    val status: Int,
    val code: String?,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

object AuthApi {

    private const val SOCIAL_LOGIN_PATH = "/api/v1/auth/social-login"
    private const val REISSUE_PATH = "/api/v1/auth/reissue"
    private const val INVALID_RESPONSE = "INVALID_RESPONSE"

    /** intent="login" — 기존 로그인. 응답: { accessToken, refreshToken }. */
    suspend fun socialLogin(provider: String, token: String): Result<TokenResponse> {
        val body = JSONObject().apply {
            put("provider", provider)
            put("token", token)
            put("intent", "login")
        }
        return postJson(SOCIAL_LOGIN_PATH, body, bearer = null)
            .mapCatching { it.toTokenResponse() }
    }

    /**
     * intent="link" — 기존 사용자[accessToken]에 소셜 계정 연동.
     * 성공 시 토큰을 내려주지 않고 [AuthMeResponse]를 반환한다.
     * 충돌 시 409 OAUTH_ACCOUNT_ALREADY_LINKED, 인증 실패 시 401.
     */
    suspend fun linkSocialAccount(
        provider: String,
        token: String,
        accessToken: String
    ): Result<AuthMeResponse> {
        val body = JSONObject().apply {
            put("provider", provider)
            put("token", token)
            put("intent", "link")
        }
        return postJson(SOCIAL_LOGIN_PATH, body, bearer = accessToken)
            .mapCatching { it.toAuthMeResponse() }
    }

    /** 바디 기반 토큰 재발급. refreshToken 만료/무효 시 401. */
    suspend fun reissue(refreshToken: String): Result<TokenResponse> {
        val body = JSONObject().apply {
            put("refreshToken", refreshToken)
        }
        return postJson(REISSUE_PATH, body, bearer = null)
            .mapCatching { it.toTokenResponse() }
    }

    private suspend fun postJson(
        path: String,
        body: JSONObject,
        bearer: String?
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("${BuildConfig.BASE_URL}$path")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                bearer?.let { setRequestProperty("Authorization", "Bearer $it") }
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
            }

            conn.outputStream.bufferedWriter().use { it.write(body.toString()) }

            val status = conn.responseCode
            if (status in 200..299) {
                val text = conn.inputStream.bufferedReader().readText()
                // 성공 본문 파싱 실패는 '네트워크 오류'가 아니라 '유효하지 않은 응답'으로 구분한다.
                val json = try {
                    if (text.isBlank()) JSONObject() else JSONObject(text)
                } catch (e: Exception) {
                    throw ApiException(status, INVALID_RESPONSE, "서버 응답 형식이 올바르지 않습니다.", e)
                }
                Result.success(json)
            } else {
                val errorText = conn.errorStream?.bufferedReader()?.readText()
                Result.failure(parseError(status, errorText))
            }
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ApiException(-1, null, "네트워크 오류가 발생했습니다.", e))
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseError(status: Int, body: String?): ApiException {
        if (body.isNullOrBlank()) {
            return ApiException(status, null, "요청에 실패했습니다. ($status)")
        }
        return try {
            val json = JSONObject(body)
            val code = json.optString("code").ifBlank { null }
            val message = json.optString("message").ifBlank { "요청에 실패했습니다. ($status)" }
            ApiException(status, code, message)
        } catch (_: Exception) {
            ApiException(status, null, body)
        }
    }

    private fun JSONObject.toTokenResponse(): TokenResponse {
        // 빈/누락 본문도 영문 JSONException 대신 일관된 한글 메시지로 노출한다.
        if (!has("accessToken") || !has("refreshToken")) {
            throw ApiException(-2, INVALID_RESPONSE, "서버 응답에 토큰이 없습니다.")
        }
        return TokenResponse(
            accessToken = getString("accessToken"),
            refreshToken = getString("refreshToken")
        )
    }

    private fun JSONObject.toAuthMeResponse(): AuthMeResponse {
        val accounts = optJSONArray("oauthAccounts") ?: JSONArray()
        val providers = (0 until accounts.length()).mapNotNull { i ->
            when (val item = accounts.opt(i)) {
                is JSONObject -> item.optString("provider").ifBlank { null }
                is String -> item.ifBlank { null }
                else -> null
            }
        }
        return AuthMeResponse(
            memberUid = optString("memberUid").ifBlank { null },
            email = optString("email").ifBlank { null },
            nickname = optString("nickname").ifBlank { null },
            oauthAccounts = providers
        )
    }
}
