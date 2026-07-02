package com.elseeker.android.core.network

import com.elseeker.android.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 식별 헤더(X-Client / X-App-Version) 부착.
 *
 * auth/no-auth 클라이언트 **모두**에 적용된다 — `/reissue` 등 no-auth 요청도
 * 서버 로그·차단 정책에서 앱 트래픽으로 식별돼야 하기 때문(PRD §8).
 */
@Singleton
class ClientHeadersInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder()
                .header("X-Client", "android")
                .header("X-App-Version", BuildConfig.VERSION_NAME)
                .build(),
        )
}
