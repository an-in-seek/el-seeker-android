package com.elseeker.android.auth

import com.elseeker.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)

object AuthApi {

    suspend fun socialLogin(provider: String, token: String): Result<TokenResponse> =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("${BuildConfig.BASE_URL}/api/v1/auth/social-login")
                conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                conn.doOutput = true

                val body = JSONObject().apply {
                    put("provider", provider)
                    put("token", token)
                }.toString()

                conn.outputStream.bufferedWriter().use { it.write(body) }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    Result.success(
                        TokenResponse(
                            accessToken = json.getString("accessToken"),
                            refreshToken = json.getString("refreshToken")
                        )
                    )
                } else {
                    val errorBody = conn.errorStream?.bufferedReader()?.readText()
                    val message = errorBody?.let {
                        try { JSONObject(it).getString("message") } catch (_: Exception) { it }
                    } ?: "로그인에 실패했습니다. (${conn.responseCode})"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(Exception("네트워크 오류가 발생했습니다.", e))
            } finally {
                conn?.disconnect()
            }
        }
}
