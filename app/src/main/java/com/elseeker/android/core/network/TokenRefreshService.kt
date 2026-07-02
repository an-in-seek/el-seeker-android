package com.elseeker.android.core.network

import com.elseeker.android.feature.auth.data.ReissueRequest
import com.elseeker.android.feature.auth.data.ReissueResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * [TokenAuthenticator] 전용 동기 재발급 클라이언트.
 *
 * 인증 인터셉터/Authenticator 가 붙지 않은 별도 OkHttpClient 로 동작해
 * 401 → reissue → 401 무한 재귀를 원천 차단한다(permitAll 엔드포인트).
 */
interface TokenRefreshService {
    @POST("api/v1/auth/reissue")
    fun reissue(@Body body: ReissueRequest): Call<ReissueResponse>
}
