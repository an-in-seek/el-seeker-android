package com.elseeker.android.feature.support.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.network.ApiException
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.support.data.InquiryDetailDto
import com.elseeker.android.feature.support.domain.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 문의 상세의 1회성 이벤트. */
sealed interface InquiryDetailEvent {
    /** 삭제 완료 → 목록으로 복귀. */
    data object Deleted : InquiryDetailEvent
    data class Message(val text: String) : InquiryDetailEvent
}

/** 문의 상세 ViewModel — 조회 + 삭제(본인·답변 전만 서버 허용). 로드는 화면 ON_RESUME 이 구동. */
@HiltViewModel
class InquiryDetailViewModel @Inject constructor(
    private val repository: SupportRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: -1L

    private val _state = MutableStateFlow<UiResource<InquiryDetailDto>>(UiResource.Loading)
    val state: StateFlow<UiResource<InquiryDetailDto>> = _state.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    private val _events = Channel<InquiryDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun load() {
        _state.value = UiResource.Loading
        if (id <= 0L) {
            _state.value = UiResource.Error("잘못된 문의입니다.", isNetwork = false)
            return
        }
        viewModelScope.launch {
            repository.detail(id)
                .onSuccess { _state.value = UiResource.Success(it) }
                .onFailure { _state.value = it.toUiError() }
        }
    }

    fun delete() {
        if (_deleting.value || id <= 0L) return
        _deleting.value = true
        viewModelScope.launch {
            repository.delete(id)
                .onSuccess { _events.send(InquiryDetailEvent.Deleted) }
                .onFailure {
                    _events.send(
                        InquiryDetailEvent.Message(
                            (it as? ApiException)?.message ?: "문의 삭제에 실패했습니다.",
                        ),
                    )
                }
            _deleting.value = false
        }
    }
}
