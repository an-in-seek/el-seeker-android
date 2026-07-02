package com.elseeker.android.core.network

import com.elseeker.android.core.auth.AuthTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 저장된 access token 이 있으면 Bearer 를 추가한다. 앱 식별 헤더는 [ClientHeadersInterceptor] 담당.
 *
 * - 공개(public) 엔드포인트에 Bearer 가 붙어도 서버는 무시하므로 안전하다.
 * - reissue 자체에는 Bearer 를 붙이지 않는다(별도 no-auth 클라이언트가 호출 — [TokenRefreshService]).
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: AuthTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        val token = tokenStore.accessToken
        if (!token.isNullOrBlank() && original.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
