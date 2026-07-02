package com.elseeker.android.core.di

import com.elseeker.android.BuildConfig
import com.elseeker.android.core.network.AuthInterceptor
import com.elseeker.android.core.network.ClientHeadersInterceptor
import com.elseeker.android.core.network.ConsentGateInterceptor
import com.elseeker.android.core.network.TokenAuthenticator
import com.elseeker.android.core.network.TokenRefreshService
import com.elseeker.android.feature.auth.data.AuthApi
import com.elseeker.android.feature.bible.data.BibleApi
import com.elseeker.android.feature.bible.data.BibleMyMemoApi
import com.elseeker.android.feature.study.data.DictionaryApi
import com.elseeker.android.feature.support.data.InquiryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** 인증(Bearer + Authenticator) 클라이언트와 재발급 전용 no-auth 클라이언트를 구분한다. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class AuthClient
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class NoAuthClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    /** 토큰이 응답 본문에 실리는 인증 엔드포인트 — debug 에서도 본문을 로깅하지 않는다. */
    private val sensitiveLogPaths = listOf(
        "/api/v1/auth/social-login",
        "/api/v1/auth/reissue",
        "/api/v1/auth/consent",
    )

    /**
     * debug: 본문 로깅하되 Authorization 헤더는 마스킹, 인증 엔드포인트는 헤더까지만.
     * release: 로깅 없음. (민감정보 로그 금지 — PRD §10)
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): Interceptor {
        if (!BuildConfig.DEBUG) return Interceptor { chain -> chain.proceed(chain.request()) }
        val bodyLogger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }
        val headersLogger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
        }
        return Interceptor { chain ->
            val path = chain.request().url.encodedPath
            val sensitive = sensitiveLogPaths.any { path.startsWith(it) }
            if (sensitive) headersLogger.intercept(chain) else bodyLogger.intercept(chain)
        }
    }

    @Provides
    @Singleton
    @NoAuthClient
    fun provideNoAuthClient(
        clientHeadersInterceptor: ClientHeadersInterceptor,
        logging: Interceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(clientHeadersInterceptor)
            .addInterceptor(logging)
            .build()

    @Provides
    @Singleton
    @AuthClient
    fun provideAuthClient(
        clientHeadersInterceptor: ClientHeadersInterceptor,
        authInterceptor: AuthInterceptor,
        consentGateInterceptor: ConsentGateInterceptor,
        authenticator: TokenAuthenticator,
        logging: Interceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(clientHeadersInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(consentGateInterceptor)
            .authenticator(authenticator)
            .addInterceptor(logging)
            .build()

    @Provides
    @Singleton
    @NoAuthClient
    fun provideNoAuthRetrofit(
        @NoAuthClient client: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(client, json)

    @Provides
    @Singleton
    @AuthClient
    fun provideAuthRetrofit(
        @AuthClient client: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(client, json)

    private fun buildRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(BuildConfig.BASE_URL))
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    private fun normalizeBaseUrl(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    @Provides
    @Singleton
    fun provideTokenRefreshService(@NoAuthClient retrofit: Retrofit): TokenRefreshService =
        retrofit.create()

    @Provides
    @Singleton
    fun provideAuthApi(@AuthClient retrofit: Retrofit): AuthApi = retrofit.create()

    @Provides
    @Singleton
    fun provideBibleApi(@AuthClient retrofit: Retrofit): BibleApi = retrofit.create()

    @Provides
    @Singleton
    fun provideDictionaryApi(@AuthClient retrofit: Retrofit): DictionaryApi = retrofit.create()

    @Provides
    @Singleton
    fun provideInquiryApi(@AuthClient retrofit: Retrofit): InquiryApi = retrofit.create()

    @Provides
    @Singleton
    fun provideBibleMyMemoApi(@AuthClient retrofit: Retrofit): BibleMyMemoApi = retrofit.create()
}
