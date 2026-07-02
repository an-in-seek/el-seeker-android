package com.elseeker.android.core.network

import android.util.Log
import com.elseeker.android.core.auth.SessionManager
import com.elseeker.android.feature.auth.data.ReissueRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 401 발생 시 refresh token 으로 access 를 재발급하고 원요청을 1회 재시도한다(PRD §5.2).
 *
 * 무한 루프/중복 재발급 방지:
 * - [responseCount] 로 재시도 횟수를 제한(이미 한 번 갱신해 붙였으면 포기).
 * - 동시 401 은 [lock] 으로 직렬화하고, 그 사이 다른 스레드가 토큰을 갱신했으면 그 값으로 재시도.
 * - signup 세션(refresh 없음)이나 재발급 실패 시 세션을 무효화하고 null 반환(로그인 화면으로).
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val refreshServiceProvider: Provider<TokenRefreshService>,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()

        synchronized(lock) {
            val current = sessionManager.accessToken ?: return null

            // 다른 스레드가 이미 토큰을 갱신했다면 새 토큰으로 즉시 재시도.
            if (failedToken != null && failedToken != current) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $current")
                    .build()
            }

            val refresh = sessionManager.refreshToken
            if (refresh.isNullOrBlank()) {
                // signup 세션 등 refresh 부재 → 재발급 불가.
                sessionManager.invalidate()
                return null
            }

            return when (val result = runReissue(refresh)) {
                is ReissueResult.Success -> response.request.newBuilder()
                    .header("Authorization", "Bearer ${result.accessToken}")
                    .build()
                // 서버가 refresh 를 명시적으로 거부(하드 401/403) → 세션 폐기(PRD §5.2).
                ReissueResult.Rejected -> {
                    sessionManager.invalidate()
                    null
                }
                // 네트워크/일시 오류 → 토큰 보존 + IOException 전파.
                // null 반환이면 호출부가 401 을 보고 세션을 폐기하므로(restoreSession 등),
                // 네트워크 오류로 실패시켜 재시도 경로(§5.4 Offline·UiResource 재시도)로 보낸다.
                ReissueResult.Unavailable ->
                    throw IOException("Token reissue temporarily unavailable")
            }
        }
    }

    private sealed interface ReissueResult {
        data class Success(val accessToken: String) : ReissueResult

        /** 서버가 refresh 무효 판정(401/403) — 재로그인 필요. */
        data object Rejected : ReissueResult

        /** 전송 실패·서버 일시 오류 — refresh 가 무효라는 증거가 아님. */
        data object Unavailable : ReissueResult
    }

    /** 동기 재발급. 성공 시 토큰 저장. 백엔드는 refresh 무효 시 401 을 준다. */
    private fun runReissue(refreshToken: String): ReissueResult {
        return try {
            val result = refreshServiceProvider.get()
                .reissue(ReissueRequest(refreshToken))
                .execute()
            val body = result.body()
            when {
                result.isSuccessful && body != null -> {
                    sessionManager.onAuthenticated(body.accessToken, body.refreshToken)
                    ReissueResult.Success(body.accessToken)
                }
                result.code() == 401 || result.code() == 403 -> {
                    Log.w(TAG, "Reissue rejected: HTTP ${result.code()}")
                    ReissueResult.Rejected
                }
                else -> {
                    Log.w(TAG, "Reissue failed: HTTP ${result.code()}")
                    ReissueResult.Unavailable
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Reissue network error: ${e.message}")
            ReissueResult.Unavailable
        } catch (e: Exception) {
            Log.e(TAG, "Reissue error: ${e.message}")
            ReissueResult.Unavailable
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    companion object {
        private const val TAG = "ElSeekerAuth"
    }
}
