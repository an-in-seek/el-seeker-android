package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.auth.SessionManager
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.VersesDto
import com.elseeker.android.feature.bible.domain.BibleNav
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BibleReaderViewModel @Inject constructor(
    private val repository: BibleRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val translationId: Long = savedStateHandle.get<String>("translationId")?.toLongOrNull() ?: 0L
    private val initialBookOrder: Int = savedStateHandle.get<String>("bookOrder")?.toIntOrNull() ?: 1
    private val initialChapter: Int = savedStateHandle.get<String>("chapterNumber")?.toIntOrNull() ?: 1

    private val _state = MutableStateFlow<UiResource<VersesDto>>(UiResource.Loading)
    val state: StateFlow<UiResource<VersesDto>> = _state.asStateFlow()

    // 장 상태(인증 필요) — 절 번호 → 하이라이트 색상 / 메모 내용. 미인증/오류 시 빈 맵.
    private val _highlights = MutableStateFlow<Map<Int, String>>(emptyMap())
    val highlights: StateFlow<Map<Int, String>> = _highlights.asStateFlow()

    private val _memos = MutableStateFlow<Map<Int, String>>(emptyMap())
    val memos: StateFlow<Map<Int, String>> = _memos.asStateFlow()

    // 오류 상태에서도 재시도할 수 있도록 마지막 시도 좌표를 보관한다.
    private var lastBookOrder: Int = initialBookOrder
    private var lastChapter: Int = initialChapter

    // 마지막 시도가 이전/다음 장 이동이었다면 그 방향을 보관해 재시도 시 같은 이동을 다시 수행한다.
    private var pendingDirection: String? = null

    // 장 상태 응답의 유효 세대. 사용자 조작(하이라이트/메모)이나 장 전환이 일어나면 증가시켜
    // 그 이전에 시작된 인플라이트 chapterState 응답이 낙관적 갱신을 덮어쓰지 못하게 한다.
    private var chapterStateGeneration = 0

    /** 인증 세션(정식 토큰) 없이 인증 필요 API 를 호출하지 않는다 — 불필요한 401/재발급 시도 방지. */
    private val hasAuthSession: Boolean
        get() = sessionManager.hasSession() && !sessionManager.isSignupSession

    /** 게스트는 하이라이트/메모를 쓸 수 없다(웹과 동일 — 보호 API). 화면이 절 탭 시 확인. */
    val canAnnotate: Boolean
        get() = hasAuthSession

    init { loadChapter(initialBookOrder, initialChapter) }

    fun loadChapter(bookOrder: Int, chapterNumber: Int) {
        pendingDirection = null
        lastBookOrder = bookOrder
        lastChapter = chapterNumber
        _state.value = UiResource.Loading
        // 이전 장의 하이라이트/메모가 잔상으로 보이지 않도록 초기화(인플라이트 응답도 무효화).
        chapterStateGeneration++
        _highlights.value = emptyMap()
        _memos.value = emptyMap()
        viewModelScope.launch {
            repository.verses(translationId, bookOrder, chapterNumber)
                .onSuccess { onLoaded(it) }
                .onFailure { _state.value = it.toUiError() }
        }
    }

    /** 오류 상태의 '다시 시도'. 직전 동작이 장 이동이면 같은 방향으로 재이동, 아니면 현재 장 재로드. */
    fun retry() {
        val direction = pendingDirection
        if (direction != null) move(direction, lastBookOrder, lastChapter)
        else loadChapter(lastBookOrder, lastChapter)
    }

    fun goPrev() = move(BibleNav.PREV)
    fun goNext() = move(BibleNav.NEXT)

    private fun move(direction: String) {
        val current = (_state.value as? UiResource.Success)?.data ?: return
        move(direction, current.book.bookOrder, current.book.chapter.chapterNumber)
    }

    private fun move(direction: String, fromBookOrder: Int, fromChapter: Int) {
        pendingDirection = direction
        lastBookOrder = fromBookOrder
        lastChapter = fromChapter
        _state.value = UiResource.Loading
        viewModelScope.launch {
            repository.navigate(translationId, fromBookOrder, fromChapter, direction)
                .onSuccess { onLoaded(it) }
                .onFailure { _state.value = it.toUiError() }
        }
    }

    private fun onLoaded(verses: VersesDto) {
        val bookOrder = verses.book.bookOrder
        val chapterNumber = verses.book.chapter.chapterNumber
        pendingDirection = null
        lastBookOrder = bookOrder
        lastChapter = chapterNumber
        _state.value = UiResource.Success(verses)
        // 읽기 진도 기록(인증 필요). 미인증이면 호출하지 않고, 실패는 본문 읽기를 막지 않으므로 무시.
        if (hasAuthSession) {
            viewModelScope.launch {
                repository.markChapterRead(translationId, bookOrder, chapterNumber)
            }
        }
        loadChapterState(bookOrder, chapterNumber)
    }

    /** 장 상태(하이라이트·메모) 로드. 인증 세션이 없으면 호출 자체를 생략(빈 맵 유지). */
    private fun loadChapterState(bookOrder: Int, chapterNumber: Int) {
        if (!hasAuthSession) return
        val generation = ++chapterStateGeneration
        viewModelScope.launch {
            repository.chapterState(translationId, bookOrder, chapterNumber)
                .onSuccess { st ->
                    if (generation != chapterStateGeneration) return@onSuccess
                    _highlights.value = st.highlights.associate { it.verseNumber to it.color }
                    _memos.value = st.memos.associate { it.verseNumber to it.content }
                }
                .onFailure {
                    if (generation != chapterStateGeneration) return@onFailure
                    _highlights.value = emptyMap()
                    _memos.value = emptyMap()
                }
        }
    }

    /** 하이라이트 지정/변경. 낙관적 갱신 후 실패 시 상태를 재조회한다. */
    fun setHighlight(verseNumber: Int, color: String) {
        val book = lastBookOrder
        val chapter = lastChapter
        chapterStateGeneration++ // 인플라이트 상태 응답이 낙관적 갱신을 덮지 않게 무효화.
        _highlights.value = _highlights.value + (verseNumber to color)
        viewModelScope.launch {
            repository.putHighlight(translationId, book, chapter, verseNumber, color)
                .onFailure { loadChapterState(book, chapter) }
        }
    }

    fun removeHighlight(verseNumber: Int) {
        val book = lastBookOrder
        val chapter = lastChapter
        chapterStateGeneration++
        _highlights.value = _highlights.value - verseNumber
        viewModelScope.launch {
            repository.deleteHighlight(translationId, book, chapter, verseNumber)
                .onFailure { loadChapterState(book, chapter) }
        }
    }

    /** 절 메모 저장(공백이면 삭제). 성공/실패에 따라 로컬 맵을 갱신한다. */
    fun saveMemo(verseNumber: Int, content: String) {
        val book = lastBookOrder
        val chapter = lastChapter
        val trimmed = content.trim()
        // 원래 메모가 없는데 빈 내용으로 저장하면 존재하지 않는 메모 삭제(404)라 아무것도 하지 않는다.
        if (trimmed.isBlank() && verseNumber !in _memos.value) return
        chapterStateGeneration++
        viewModelScope.launch {
            if (trimmed.isBlank()) {
                repository.deleteVerseMemo(translationId, book, chapter, verseNumber)
                    .onSuccess { _memos.value = _memos.value - verseNumber }
                    .onFailure { loadChapterState(book, chapter) }
            } else {
                repository.putVerseMemo(translationId, book, chapter, verseNumber, trimmed)
                    .onSuccess { _memos.value = _memos.value + (verseNumber to trimmed) }
                    .onFailure { loadChapterState(book, chapter) }
            }
        }
    }
}
