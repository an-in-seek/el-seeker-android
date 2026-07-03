package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.TranslationDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 성경 탭 루트 — 번역본 목록 조회 API(`GET /api/v1/bibles/translations`)를 호출해
 * 서버가 내려주는 전체 번역본을 그대로 표시한다(웹 /web/bible/translation 과 동일).
 */
@HiltViewModel
class TranslationListViewModel @Inject constructor(
    private val repository: BibleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiResource<List<TranslationDto>>>(UiResource.Loading)
    val state: StateFlow<UiResource<List<TranslationDto>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiResource.Loading
        viewModelScope.launch {
            repository.translations()
                .onSuccess { _state.value = UiResource.Success(it) }
                .onFailure { _state.value = it.toUiError() }
        }
    }
}
