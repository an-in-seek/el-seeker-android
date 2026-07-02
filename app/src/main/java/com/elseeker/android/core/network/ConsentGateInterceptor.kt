package com.elseeker.android.core.network

import com.elseeker.android.core.auth.AuthState
import com.elseeker.android.core.auth.SessionManager
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 `ConsentGateFilter` 가 signup 토큰의 일반 API 접근을 막을 때 내려주는
 * `403 + code=CONSENT_REQUIRED` 를 전역에서 감지해 앱을 동의 화면으로 라우팅한다(PRD §5.4/§8).
 *
 * 세션 도중 토큰이 signup 범위로 바뀌거나 동의 미완료 상태가 드러나는 경우를 한 곳에서 처리한다.
 * 응답 본문은 [Response.peekBody] 로 복사해 읽으므로 원본 스트림은 그대로 소비자에게 전달된다.
 */
@Singleton
class ConsentGateInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val json: Json,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 403 && isConsentRequired(response)) {
            sessionManager.setState(AuthState.NeedsConsent)
        }
        return response
    }

    private fun isConsentRequired(response: Response): Boolean {
        val body = try {
            response.peekBody(MAX_PEEK_BYTES).string()
        } catch (_: Exception) {
            return false
        }
        if (body.isBlank()) return false
        val code = try {
            json.decodeFromString(ErrorResponse.serializer(), body).code
        } catch (_: Exception) {
            null
        }
        return code == ApiException.CODE_CONSENT_REQUIRED
    }

    private companion object {
        const val MAX_PEEK_BYTES = 1024L * 1024 // 에러 본문은 작으므로 1MB 상한이면 충분
    }
}
