package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.BookDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BibleBooksUiState(
    val oldTestament: List<BookDto> = emptyList(),
    val newTestament: List<BookDto> = emptyList(),
    val translationName: String = "",
    val translationType: String = "",
)

/**
 * 성경책 목록 ViewModel — 라우트 인자로 받은 번역본의 구약/신약 목록을 로드한다.
 * 웹 book-list.js 와 동일하게 이름 검색(부분일치)·섹션 접기/펼치기 상태를 함께 관리한다.
 */
@HiltViewModel
class BibleBooksViewModel @Inject constructor(
    private val repository: BibleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val translationId: Long = savedStateHandle.get<String>("translationId")?.toLongOrNull() ?: -1L

    private val _state = MutableStateFlow<UiResource<BibleBooksUiState>>(UiResource.Loading)
    val state: StateFlow<UiResource<BibleBooksUiState>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _oldExpanded = MutableStateFlow(true)
    val oldExpanded: StateFlow<Boolean> = _oldExpanded.asStateFlow()

    private val _newExpanded = MutableStateFlow(true)
    val newExpanded: StateFlow<Boolean> = _newExpanded.asStateFlow()

    init { load() }

    fun load() {
        if (translationId <= 0L) {
            _state.value = UiResource.Error("잘못된 번역본입니다.", isNetwork = false)
            return
        }
        // 책·번역본이 캐시에 있으면 Loading 없이 즉시 렌더(재진입 시 스피너 깜빡임 제거).
        val cachedBooks = repository.peekBooks(translationId)
        val cachedTranslation = repository.peekTranslations()?.firstOrNull { it.translationId == translationId }
        val hasCache = cachedBooks != null
        _state.value = if (cachedBooks != null) {
            UiResource.Success(buildState(cachedBooks, cachedTranslation))
        } else {
            UiResource.Loading
        }
        viewModelScope.launch {
            val booksDeferred = async { repository.books(translationId) }
            val translationsDeferred = async { repository.translations() }
            val booksResult = booksDeferred.await()
            val translationsResult = translationsDeferred.await()

            booksResult
                .onSuccess { books ->
                    val translation = translationsResult.getOrNull()
                        ?.firstOrNull { it.translationId == translationId }
                    _state.value = UiResource.Success(buildState(books, translation))
                }
                // 캐시로 이미 렌더 중이면 일시적 오류로 화면을 덮지 않는다.
                .onFailure { if (!hasCache) _state.value = it.toUiError() }
        }
    }

    private fun buildState(
        books: List<BookDto>,
        translation: com.elseeker.android.feature.bible.data.TranslationDto?,
    ): BibleBooksUiState = BibleBooksUiState(
        oldTestament = books.filter { it.testamentType == TESTAMENT_OLD },
        newTestament = books.filter { it.testamentType == TESTAMENT_NEW },
        translationName = translation?.translationName ?: "",
        translationType = translation?.translationType ?: "",
    )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun toggleOldTestament() {
        _oldExpanded.value = !_oldExpanded.value
    }

    fun toggleNewTestament() {
        _newExpanded.value = !_newExpanded.value
    }

    companion object {
        private const val TESTAMENT_OLD = "OLD"
        private const val TESTAMENT_NEW = "NEW"
    }
}
