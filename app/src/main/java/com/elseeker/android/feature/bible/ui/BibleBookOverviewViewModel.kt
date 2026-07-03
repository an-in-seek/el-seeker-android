package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.auth.SessionManager
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.BookMemoItemDto
import com.elseeker.android.feature.bible.data.ChaptersDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
    // 제목/하단바 라벨·요약 행·장 번호는 모두 장 목록 응답(ChaptersDto.book)에서 확보한다.
    // 책 상세(저자·년도·배경 등)는 별도 개요 화면에서 조회하므로 여기서는 부르지 않는다.
    val bookName: String,
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

    // 상단바 번역본 코드 칩("KRV" 등) — 조회 실패 시 null 로 유지해 칩을 숨긴다.
    private val _translationCode = MutableStateFlow<String?>(null)
    val translationCode: StateFlow<String?> = _translationCode.asStateFlow()

    private val _memoEvents = Channel<BookMemoEvent>(Channel.BUFFERED)
    val memoEvents: Flow<BookMemoEvent> = _memoEvents.receiveAsFlow()

    /** 인증 세션(정식 토큰) 없이 인증 필요 API 를 호출하지 않는다 — 불필요한 401/재발급 시도 방지. */
    val hasAuthSession: Boolean
        get() = sessionManager.hasSession() && !sessionManager.isSignupSession

    init {
        loadTranslationCode()
    }

    private fun loadTranslationCode() {
        viewModelScope.launch {
            _translationCode.value = repository.translations().getOrNull()
                ?.firstOrNull { it.translationId == translationId }
                ?.translationType
        }
    }

    fun load() {
        if (translationId <= 0L || bookOrder <= 0) {
            _state.value = UiResource.Error("잘못된 책입니다.", isNetwork = false)
            return
        }
        // 캐시(프리페치 포함)에 이미 있으면 Loading 없이 즉시 렌더 — 이전/다음 이동 시 스피너 깜빡임 제거.
        val cached = repository.peekChapters(translationId, bookOrder)
        _state.value = if (cached != null) {
            UiResource.Success(cached.toBookOverview())
        } else {
            UiResource.Loading
        }
        viewModelScope.launch {
            // 장 목록·읽음 진도·책 메모를 모두 병렬로 조회한다.
            // 각 응답은 서로 독립된 StateFlow 를 갱신해, 먼저 오는 것부터 개별적으로 즉시 렌더된다.
            val chaptersDeferred = async { repository.chapters(translationId, bookOrder) }
            val readDeferred = async { loadReadChapters() }
            // 책 메모는 읽음 진도와 독립 — 병렬로 조회해 도착 즉시 반영(읽음 완료를 기다리지 않음).
            launch { _bookMemo.value = loadBookMemo() }

            // 장 목록이 오면 즉시 그리드를 렌더한다 — 제목·요약·장 번호가 모두 이 응답에 있다.
            val chaptersDto = chaptersDeferred.await().getOrElse {
                readDeferred.cancel()
                _state.value = it.toUiError()
                return@launch
            }
            // 캐시로 이미 동일 값이면 StateFlow 가 재방출하지 않아 불필요한 리컴포지션도 없다.
            _state.value = UiResource.Success(chaptersDto.toBookOverview())
            // 인접 책(±1) 장 목록을 미리 캐시해 이전/다음 이동을 즉시 렌더한다.
            prefetchNeighbors()

            // 읽음 진도는 도착하는 대로 초록 체크로 채운다 — 그리드를 막지 않는다.
            val read = readDeferred.await()
            (_state.value as? UiResource.Success)?.let { current ->
                _state.value = UiResource.Success(current.data.copy(readChapters = read))
            }
        }
    }

    /** 이전/다음 책의 장 목록을 백그라운드로 미리 조회(결과는 Repository 캐시에만 적재). 실패 무시. */
    private fun prefetchNeighbors() {
        viewModelScope.launch {
            listOf(bookOrder - 1, bookOrder + 1)
                .filter { it in 1..66 }
                .forEach { repository.chapters(translationId, it) }
        }
    }

    /**
     * 리더에서 돌아왔을 때(ON_RESUME)의 무음 갱신 — 읽음 표시/책 메모만 다시 조회한다.
     * 이미 로드된 화면을 Loading 으로 되돌리지 않아 스피너 깜빡임·스크롤 위치 초기화가 없다.
     */
    fun refreshOnResume() {
        val current = _state.value
        if (current !is UiResource.Success) {
            load()
            return
        }
        viewModelScope.launch {
            _state.value = UiResource.Success(current.data.copy(readChapters = loadReadChapters()))
            _bookMemo.value = loadBookMemo()
        }
    }

    /** 읽기 진도(인증 필요) — 게스트는 호출 자체를 생략하고, 실패해도 화면을 막지 않는다. */
    private suspend fun loadReadChapters(): Set<Int> =
        if (hasAuthSession) {
            repository.readChapters(translationId, bookOrder).getOrDefault(emptyList()).toSet()
        } else {
            emptySet()
        }

    private suspend fun loadBookMemo(): BookMemoItemDto? =
        if (hasAuthSession) repository.bookMemo(translationId, bookOrder).getOrNull() else null

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

/** ChaptersDto → 화면 모델(읽음 진도는 별도 로드라 빈 값으로 시작). */
private fun ChaptersDto.toBookOverview(): BookOverview =
    BookOverview(
        bookName = book.bookName,
        descriptionSummary = book.descriptionSummary,
        chapters = chapterNumbers(),
        readChapters = emptySet(),
    )
