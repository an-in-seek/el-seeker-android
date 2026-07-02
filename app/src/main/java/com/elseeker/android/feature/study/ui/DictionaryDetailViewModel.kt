package com.elseeker.android.feature.study.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.study.data.DictionaryDetailWithRefs
import com.elseeker.android.feature.study.data.DictionaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 사전 상세 ViewModel — 상세 + 참조 목록을 로드한다. */
@HiltViewModel
class DictionaryDetailViewModel @Inject constructor(
    private val repository: DictionaryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // nav arg 는 StringType 으로 전달되므로 Long 으로 파싱한다.
    private val id: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: -1L

    private val _state = MutableStateFlow<UiResource<DictionaryDetailWithRefs>>(UiResource.Loading)
    val state: StateFlow<UiResource<DictionaryDetailWithRefs>> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiResource.Loading
        if (id <= 0L) {
            _state.value = UiResource.Error("잘못된 사전 항목입니다.", isNetwork = false)
            return
        }
        viewModelScope.launch {
            repository.detail(id)
                .onSuccess { _state.value = UiResource.Success(it) }
                .onFailure { _state.value = it.toUiError() }
        }
    }
}
