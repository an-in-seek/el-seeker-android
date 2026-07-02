package com.elseeker.android.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Retrofit suspend 호출을 [ApiException] 으로 정규화한다.
 *
 * - 2xx: 본문 그대로 반환
 * - non-2xx([HttpException]): 에러 본문을 [ErrorResponse] 로 파싱해 status/code 추출
 * - [IOException]: 네트워크 오류([ApiException.STATUS_NETWORK])
 *
 * [CancellationException] 은 코루틴 취소 신호이므로 그대로 전파한다(삼키지 않음).
 */
suspend fun <T> safeApiCall(json: Json, block: suspend () -> T): T {
    try {
        return block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        throw e.toApiException(json)
    } catch (e: IOException) {
        throw ApiException(
            status = ApiException.STATUS_NETWORK,
            code = null,
            message = "네트워크 연결을 확인해 주세요.",
            cause = e,
        )
    } catch (e: ApiException) {
        throw e
    } catch (e: Exception) {
        throw ApiException(
            status = ApiException.STATUS_PARSE,
            code = null,
            message = "응답을 처리하지 못했습니다.",
            cause = e,
        )
    }
}

/**
 * `Response<T>` 반환 메서드(204 등)는 non-2xx 에서도 예외를 던지지 않고
 * isSuccessful=false 인 Response 를 돌려준다. 실패를 [HttpException] 으로 승격해
 * [safeApiCall] 의 에러 정규화 경로를 타게 한다.
 */
fun <T> Response<T>.orThrow(): Response<T> {
    if (!isSuccessful) throw HttpException(this)
    return this
}

private fun HttpException.toApiException(json: Json): ApiException {
    val raw = try {
        response()?.errorBody()?.string()
    } catch (_: Exception) {
        null
    }
    val parsed = raw?.takeIf { it.isNotBlank() }?.let {
        try {
            json.decodeFromString(ErrorResponse.serializer(), it)
        } catch (_: Exception) {
            null
        }
    }
    return ApiException(
        status = code(),
        code = parsed?.code,
        message = parsed?.message?.takeIf { it.isNotBlank() }
            ?: "요청에 실패했습니다. (${code()})",
        cause = this,
    )
}
