package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.domain.MyMemoRepository
import com.elseeker.android.feature.bible.domain.MyMemos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 내 메모 모아보기 ViewModel. */
@HiltViewModel
class MyMemosViewModel @Inject constructor(
    private val repository: MyMemoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiResource<MyMemos>>(UiResource.Loading)
    val state: StateFlow<UiResource<MyMemos>> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiResource.Loading
        viewModelScope.launch {
            repository.load()
                .onSuccess { _state.value = UiResource.Success(it) }
                .onFailure { _state.value = it.toUiError() }
        }
    }
}
