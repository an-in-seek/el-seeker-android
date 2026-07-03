package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.BookDetailDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 책 개요(요약·저자·년도·시대·배경·내용) 화면 모델 — 웹 book-description.html 대응.
 * 장 목록 화면에서 📘 요약 행을 탭하면 진입한다.
 */
@HiltViewModel
class BookDescriptionViewModel @Inject constructor(
    private val repository: BibleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val translationId: Long = savedStateHandle.get<String>("translationId")?.toLongOrNull() ?: -1L
    val bookOrder: Int = savedStateHandle.get<String>("bookOrder")?.toIntOrNull() ?: -1

    private val _state = MutableStateFlow<UiResource<BookDetailDto>>(UiResource.Loading)
    val state: StateFlow<UiResource<BookDetailDto>> = _state.asStateFlow()

    // 상단바 번역본 코드 칩("KRV" 등) — 조회 실패 시 null 로 유지해 칩을 숨긴다.
    private val _translationCode = MutableStateFlow<String?>(null)
    val translationCode: StateFlow<String?> = _translationCode.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiResource.Loading
        if (translationId <= 0L || bookOrder <= 0) {
            _state.value = UiResource.Error("잘못된 책입니다.", isNetwork = false)
            return
        }
        viewModelScope.launch {
            // 상세와 번역본 코드(칩)를 병렬로 조회한다.
            val detailDeferred = async { repository.bookDetail(translationId, bookOrder) }
            val codeDeferred = async { repository.translations() }
            _state.value = detailDeferred.await().fold(
                onSuccess = { UiResource.Success(it) },
                onFailure = { it.toUiError() },
            )
            _translationCode.value = codeDeferred.await().getOrNull()
                ?.firstOrNull { it.translationId == translationId }
                ?.translationType
        }
    }
}
