package com.elseeker.android.auth

import android.webkit.CookieManager
import com.elseeker.android.BuildConfig

object CookieHelper {

    private val baseUrl = BuildConfig.BASE_URL

    fun setAuthCookies(accessToken: String, refreshToken: String) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setCookie(baseUrl, "ACCESS_TOKEN=$accessToken; Path=/; Secure; SameSite=Lax")
        cookieManager.setCookie(baseUrl, "REFRESH_TOKEN=$refreshToken; Path=/; Secure; HttpOnly; SameSite=Lax")
        cookieManager.flush()
    }

    fun clearAuthCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setCookie(baseUrl, "ACCESS_TOKEN=; Path=/; Max-Age=0")
        cookieManager.setCookie(baseUrl, "REFRESH_TOKEN=; Path=/; Max-Age=0")
        cookieManager.flush()
    }
}
