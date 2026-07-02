package com.elseeker.android.core.network

import kotlinx.serialization.Serializable

/**
 * 백엔드 공통 에러 본문(`common/adapter/input/web/ErrorResponse.kt`).
 *
 * 두 가지 경로로 내려온다:
 * - 도메인 예외(`GlobalExceptionHandler`): `{status, code, message}` — `code`는 `ErrorType.name`.
 * - Spring Security 예외(401/403): 컨테이너 기본 바디라 `code`가 없을 수 있다.
 *
 * 따라서 [code]는 nullable 로 두고, 분기는 HTTP status 를 1차 신호로 삼는다(PRD §8).
 */
@Serializable
data class ErrorResponse(
    val status: Int? = null,
    val code: String? = null,
    val message: String? = null,
)
