package com.elseeker.android.core.ui

import com.elseeker.android.core.network.ApiException

/** 화면 단위 비동기 상태 표준. 로딩/성공/오류 3분기. */
sealed interface UiResource<out T> {
    data object Loading : UiResource<Nothing>
    data class Success<T>(val data: T) : UiResource<T>
    data class Error(val message: String, val isNetwork: Boolean) : UiResource<Nothing>
}

fun Throwable.toUiError(): UiResource.Error {
    val api = this as? ApiException
    return UiResource.Error(
        message = api?.message ?: "문제가 발생했습니다. 잠시 후 다시 시도해 주세요.",
        isNetwork = api?.isNetwork == true,
    )
}
