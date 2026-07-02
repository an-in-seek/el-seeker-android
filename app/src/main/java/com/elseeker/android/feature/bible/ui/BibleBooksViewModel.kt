package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.BookDto
import com.elseeker.android.feature.bible.data.TranslationDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BibleBooksUiState(
    val translation: TranslationDto? = null,
    val oldTestament: List<BookDto> = emptyList(),
    val newTestament: List<BookDto> = emptyList(),
)

@HiltViewModel
class BibleBooksViewModel @Inject constructor(
    private val repository: BibleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiResource<BibleBooksUiState>>(UiResource.Loading)
    val state: StateFlow<UiResource<BibleBooksUiState>> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiResource.Loading
        viewModelScope.launch {
            repository.visibleTranslations()
                .onSuccess { translations ->
                    val translation = translations.firstOrNull()
                    if (translation == null) {
                        _state.value = UiResource.Error("표시할 번역본이 없습니다.", isNetwork = false)
                        return@onSuccess
                    }
                    loadBooks(translation)
                }
                .onFailure { _state.value = it.toUiError() }
        }
    }

    private suspend fun loadBooks(translation: TranslationDto) {
        repository.books(translation.translationId)
            .onSuccess { books ->
                _state.value = UiResource.Success(
                    BibleBooksUiState(
                        translation = translation,
                        oldTestament = books.filter { it.testamentType == TESTAMENT_OLD },
                        newTestament = books.filter { it.testamentType == TESTAMENT_NEW },
                    )
                )
            }
            .onFailure { _state.value = it.toUiError() }
    }

    companion object {
        private const val TESTAMENT_OLD = "OLD"
        private const val TESTAMENT_NEW = "NEW"
    }
}
