package com.elseeker.android.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 토큰의 영구 저장소. Android Keystore 로 암호화된
 * [EncryptedSharedPreferences] 에 access/refresh 토큰을 평문 없이 보관한다(PRD §5.2).
 *
 * 인터셉터/Authenticator 가 네트워크 스레드에서 동기적으로 읽으므로 getter 는 blocking 이다.
 */
@Singleton
class AuthTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    val accessToken: String? get() = prefs.getString(KEY_ACCESS, null)
    val refreshToken: String? get() = prefs.getString(KEY_REFRESH, null)

    val hasSession: Boolean get() = !accessToken.isNullOrBlank()

    /** signup(동의 전용) 토큰만 보유한 상태인지. true 면 동의 화면으로 라우팅(PRD §5.4). */
    val isSignupSession: Boolean get() = JwtDecoder.isSignupToken(accessToken)

    /**
     * 토큰 저장. signup 토큰은 refresh 가 없으므로([refreshToken] null 허용),
     * 기존 refresh 를 덮어쓰지 않도록 null 일 때는 access 만 갱신한다.
     */
    fun save(accessToken: String, refreshToken: String?) {
        prefs.edit().apply {
            putString(KEY_ACCESS, accessToken)
            if (refreshToken != null) putString(KEY_REFRESH, refreshToken)
            apply()
        }
    }

    /**
     * signup(동의 전용) 토큰 저장. refresh 를 **삭제**해 이전 계정의 refresh 가 남지 않게 한다.
     * (남아 있으면 401 시 Authenticator 가 이전 계정으로 재발급 — 계정 섞임)
     */
    fun saveSignup(accessToken: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS, accessToken)
            remove(KEY_REFRESH)
            apply()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "elseeker_auth"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
