package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.auth.SessionManager
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.BookDetailDto
import com.elseeker.android.feature.bible.data.BookMemoItemDto
import com.elseeker.android.feature.bible.data.ChaptersDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 책 개요 + 장 목록 + 읽은 장을 묶은 화면 모델. */
data class BookOverview(
    val detail: BookDetailDto,
    val descriptionSummary: String,
    val chapters: List<Int>,
    val readChapters: Set<Int> = emptySet(),
)

/** 책 메모 저장/삭제 완료를 화면에 1회성으로 알리는 이벤트(토스트 트리거). */
sealed interface BookMemoEvent {
    data object Saved : BookMemoEvent
    data object Deleted : BookMemoEvent
}

/**
 * 책 개요 ViewModel — 책 설명(description) + 장 목록 + 읽기 진도 + 책 메모 로드.
 * 로드는 화면 ON_RESUME 이 구동한다(리더에서 돌아오면 읽음 표시 최신화).
 */
@HiltViewModel
class BibleBookOverviewViewModel @Inject constructor(
    private val repository: BibleRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val translationId: Long = savedStateHandle.get<String>("translationId")?.toLongOrNull() ?: -1L
    val bookOrder: Int = savedStateHandle.get<String>("bookOrder")?.toIntOrNull() ?: -1

    private val _state = MutableStateFlow<UiResource<BookOverview>>(UiResource.Loading)
    val state: StateFlow<UiResource<BookOverview>> = _state.asStateFlow()

    private val _bookMemo = MutableStateFlow<BookMemoItemDto?>(null)
    val bookMemo: StateFlow<BookMemoItemDto?> = _bookMemo.asStateFlow()

    private val _memoEvents = Channel<BookMemoEvent>(Channel.BUFFERED)
    val memoEvents: Flow<BookMemoEvent> = _memoEvents.receiveAsFlow()

    /** 인증 세션(정식 토큰) 없이 인증 필요 API 를 호출하지 않는다 — 불필요한 401/재발급 시도 방지. */
    val hasAuthSession: Boolean
        get() = sessionManager.hasSession() && !sessionManager.isSignupSession

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
            val chaptersDto = repository.chapters(translationId, bookOrder).getOrNull()
            val chapters = chaptersDto?.chapterNumbers().orEmpty()
            val descriptionSummary = chaptersDto?.book?.descriptionSummary.orEmpty()
            // 읽기 진도(인증 필요)는 실패해도 화면을 막지 않는다 — 표시만 생략.
            val read = repository.readChapters(translationId, bookOrder)
                .getOrDefault(emptyList())
                .toSet()
            _state.value = UiResource.Success(BookOverview(detail, descriptionSummary, chapters, read))

            _bookMemo.value = if (hasAuthSession) {
                repository.bookMemo(translationId, bookOrder).getOrNull()
            } else {
                null
            }
        }
    }

    /** 책 메모 저장(신규/수정 공통 — PUT upsert). */
    fun saveBookMemo(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.putBookMemo(translationId, bookOrder, trimmed)
                .onSuccess {
                    _bookMemo.value = it
                    _memoEvents.send(BookMemoEvent.Saved)
                }
        }
    }

    fun deleteBookMemo() {
        viewModelScope.launch {
            repository.deleteBookMemo(translationId, bookOrder)
                .onSuccess {
                    _bookMemo.value = null
                    _memoEvents.send(BookMemoEvent.Deleted)
                }
        }
    }
}

/** ChaptersDto → 장 번호 목록. */
private fun ChaptersDto.chapterNumbers(): List<Int> =
    book.chapters.map { it.chapterNumber }.sorted()
