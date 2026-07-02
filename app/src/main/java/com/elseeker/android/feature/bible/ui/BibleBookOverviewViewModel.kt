package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.BookDetailDto
import com.elseeker.android.feature.bible.data.ChaptersDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 책 개요 + 장 목록 + 읽은 장을 묶은 화면 모델. */
data class BookOverview(
    val detail: BookDetailDto,
    val chapters: List<Int>,
    val readChapters: Set<Int> = emptySet(),
)

/**
 * 책 개요 ViewModel — 책 설명(description) + 장 목록 + 읽기 진도 로드.
 * 로드는 화면 ON_RESUME 이 구동한다(리더에서 돌아오면 읽음 표시 최신화).
 */
@HiltViewModel
class BibleBookOverviewViewModel @Inject constructor(
    private val repository: BibleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val translationId: Long = savedStateHandle.get<String>("translationId")?.toLongOrNull() ?: -1L
    val bookOrder: Int = savedStateHandle.get<String>("bookOrder")?.toIntOrNull() ?: -1

    private val _state = MutableStateFlow<UiResource<BookOverview>>(UiResource.Loading)
    val state: StateFlow<UiResource<BookOverview>> = _state.asStateFlow()

    fun load() {
        _state.value = UiResource.Loading
        if (translationId <= 0L || bookOrder <= 0) {
            _state.value = UiResource.Error("잘못된 책입니다.", isNetwork = false)
            return
        }
        viewModelScope.launch {
            val detailResult = repository.bookDetail(translationId, bookOrder)
            val detail = detailResult.getOrElse {
                _state.value = it.toUiError()
                return@launch
            }
            val chapters = repository.chapters(translationId, bookOrder)
                .map { it.chapterNumbers() }
                .getOrDefault(emptyList())
            // 읽기 진도(인증 필요)는 실패해도 화면을 막지 않는다 — 표시만 생략.
            val read = repository.readChapters(translationId, bookOrder)
                .getOrDefault(emptyList())
                .toSet()
            _state.value = UiResource.Success(BookOverview(detail, chapters, read))
        }
    }
}

/** ChaptersDto → 장 번호 목록. */
private fun ChaptersDto.chapterNumbers(): List<Int> =
    book.chapters.map { it.chapterNumber }.sorted()
