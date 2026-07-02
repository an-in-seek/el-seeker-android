package com.elseeker.android.feature.auth.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 인증 + 회원 Retrofit 계약. 인증 필요 엔드포인트는 [AuthInterceptor] 가 Bearer 를 부착한다.
 * intent=link 와 일반 login 은 응답 타입이 다르므로 메서드를 분리한다(PRD §5.1.1).
 */
interface AuthApi {

    @POST("api/v1/auth/social-login")
    suspend fun socialLogin(@Body body: SocialLoginRequest): SocialLoginResponse

    /** intent="link": 성공 시 토큰이 아니라 회원 정보(AuthMeResponse)를 반환. Bearer 필요. */
    @POST("api/v1/auth/social-login")
    suspend fun linkSocialAccount(@Body body: SocialLoginRequest): AuthMeResponse

    // reissue 는 여기 정의하지 않는다 — @AuthClient 로 호출하면 401→Authenticator 재귀 위험.
    // 재발급은 no-auth 전용 [TokenRefreshService] 가 담당한다.

    @POST("api/v1/auth/consent")
    suspend fun consent(@Body body: ConsentRequest): ConsentResponse

    @POST("api/v1/auth/consent/cancel")
    suspend fun cancelConsent(): Response<Unit>

    @GET("api/v1/auth/me")
    suspend fun me(): AuthMeResponse

    @GET("api/v1/members/{memberUid}/oauth-accounts")
    suspend fun oauthAccounts(@Path("memberUid") memberUid: String): List<MemberOAuthAccountResponse>

    @DELETE("api/v1/members/{memberUid}/oauth-accounts")
    suspend fun unlinkOauthAccount(
        @Path("memberUid") memberUid: String,
        @Query("provider") provider: String,
        @Query("providerUserId") providerUserId: String,
    ): AuthMeResponse

    @PUT("api/v1/members/{memberUid}")
    suspend fun updateMember(
        @Path("memberUid") memberUid: String,
        @Body body: MemberUpdateRequest,
    ): AuthMeResponse

    @DELETE("api/v1/members/{memberUid}")
    suspend fun deleteMember(@Path("memberUid") memberUid: String): Response<Unit>
}
