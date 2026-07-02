package com.elseeker.android.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.DailyVerseDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 홈 탭 ViewModel — 오늘의 말씀을 로드한다. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BibleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiResource<DailyVerseDto>>(UiResource.Loading)
    val state: StateFlow<UiResource<DailyVerseDto>> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiResource.Loading
        viewModelScope.launch {
            repository.dailyVerse()
                .onSuccess { _state.value = UiResource.Success(it) }
                .onFailure { _state.value = it.toUiError() }
        }
    }
}
