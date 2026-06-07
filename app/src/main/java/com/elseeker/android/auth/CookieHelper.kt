package com.elseeker.android.auth

import android.webkit.CookieManager
import com.elseeker.android.BuildConfig

object CookieHelper {

    private val baseUrl = BuildConfig.BASE_URL

    fun setAuthCookies(accessToken: String, refreshToken: String) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setCookie(baseUrl, "ACCESS_TOKEN=$accessToken; Path=/; Secure; HttpOnly; SameSite=Lax")
        cookieManager.setCookie(baseUrl, "REFRESH_TOKEN=$refreshToken; Path=/; Secure; HttpOnly; SameSite=Lax")
        cookieManager.flush()
    }

    /**
     * WebView 쿠키 저장소에서 현재 인증 토큰을 읽어온다.
     * 약관 동의 등으로 서버가 Set-Cookie로 토큰을 회전시키면 그 최신 값이 여기 담긴다.
     * (Android CookieManager.getCookie 는 HttpOnly 쿠키도 반환한다.)
     * 둘 다 존재할 때만 (access, refresh) 반환, 아니면 null.
     */
    fun readAuthTokens(): Pair<String, String>? {
        val cookies = CookieManager.getInstance().getCookie(baseUrl) ?: return null
        val map = cookies.split(";").mapNotNull { part ->
            val kv = part.trim().split("=", limit = 2)
            if (kv.size == 2 && kv[1].isNotBlank()) kv[0] to kv[1] else null
        }.toMap()
        val access = map["ACCESS_TOKEN"] ?: return null
        val refresh = map["REFRESH_TOKEN"] ?: return null
        return access to refresh
    }

    fun clearAuthCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setCookie(baseUrl, "ACCESS_TOKEN=; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=0")
        cookieManager.setCookie(baseUrl, "REFRESH_TOKEN=; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=0")
        cookieManager.flush()
    }
}
