package com.elseeker.android.feature.support.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.support.data.InquirySummaryDto
import com.elseeker.android.feature.support.domain.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 내 문의 목록 ViewModel. */
@HiltViewModel
class InquiryListViewModel @Inject constructor(
    private val repository: SupportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiResource<List<InquirySummaryDto>>>(UiResource.Loading)
    val state: StateFlow<UiResource<List<InquirySummaryDto>>> = _state.asStateFlow()

    // init 로딩 대신 화면 ON_RESUME 에서 로드한다 — 작성 화면에서 돌아왔을 때 최신 목록을 보장(중복 제출 방지).

    fun load() {
        _state.value = UiResource.Loading
        viewModelScope.launch {
            repository.myInquiries()
                .onSuccess { _state.value = UiResource.Success(it.content) }
                .onFailure { _state.value = it.toUiError() }
        }
    }
}
