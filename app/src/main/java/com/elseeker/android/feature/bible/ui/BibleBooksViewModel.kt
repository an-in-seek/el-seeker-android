package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.BookDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BibleBooksUiState(
    val oldTestament: List<BookDto> = emptyList(),
    val newTestament: List<BookDto> = emptyList(),
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
        _state.value = UiResource.Loading
        viewModelScope.launch {
            repository.books(translationId)
                .onSuccess { books ->
                    _state.value = UiResource.Success(
                        BibleBooksUiState(
                            oldTestament = books.filter { it.testamentType == TESTAMENT_OLD },
                            newTestament = books.filter { it.testamentType == TESTAMENT_NEW },
                        )
                    )
                }
                .onFailure { _state.value = it.toUiError() }
        }
    }

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
