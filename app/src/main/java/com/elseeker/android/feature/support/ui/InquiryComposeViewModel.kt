package com.elseeker.android.feature.support.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.network.ApiException
import com.elseeker.android.feature.support.data.InquiryCategory
import com.elseeker.android.feature.support.domain.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 문의 작성/수정 결과 1회성 이벤트. */
sealed interface InquiryComposeEvent {
    /** 등록 또는 수정 완료. */
    data object Created : InquiryComposeEvent
    data class Error(val message: String) : InquiryComposeEvent
}

/**
 * 문의 작성/수정 ViewModel — 폼 상태 관리 + 제출.
 * 라우트에 `id` 가 있으면 수정 모드: 기존 문의를 불러와 프리필하고 제출 시 PUT.
 */
@HiltViewModel
class InquiryComposeViewModel @Inject constructor(
    private val repository: SupportRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editId: Long? = savedStateHandle.get<String>("id")?.toLongOrNull()
    val isEdit: Boolean = editId != null

    private val _category = MutableStateFlow(InquiryCategory.ACCOUNT)
    val category: StateFlow<InquiryCategory> = _category.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _events = Channel<InquiryComposeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // 수정 모드에서 기존 문의를 불러오는 동안 입력/제출을 잠근다.
    private val _prefilling = MutableStateFlow(isEdit)
    val prefilling: StateFlow<Boolean> = _prefilling.asStateFlow()

    init {
        val id = editId
        if (id != null) {
            viewModelScope.launch {
                repository.detail(id)
                    .onSuccess { d ->
                        _category.value =
                            InquiryCategory.entries.firstOrNull { it.wire == d.category }
                                ?: InquiryCategory.ETC
                        _title.value = d.title
                        _content.value = d.content
                    }
                    .onFailure {
                        _events.send(
                            InquiryComposeEvent.Error(
                                (it as? ApiException)?.message ?: "문의를 불러오지 못했습니다.",
                            ),
                        )
                    }
                _prefilling.value = false
            }
        }
    }

    fun onCategoryChange(value: InquiryCategory) { _category.value = value }
    fun onTitleChange(value: String) { _title.value = value }
    fun onContentChange(value: String) { _content.value = value }

    /** 제출 가능 여부(제목·내용 비공백). 화면이 관찰해 버튼 활성화를 갱신한다. */
    val canSubmit: StateFlow<Boolean> =
        combine(_title, _content) { t, c -> t.isNotBlank() && c.isNotBlank() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun submit() {
        if (_submitting.value || _prefilling.value || !canSubmit.value) return
        _submitting.value = true
        viewModelScope.launch {
            val id = editId
            val result = if (id != null) {
                repository.update(id, _category.value.wire, _title.value.trim(), _content.value.trim())
            } else {
                repository.create(_category.value.wire, _title.value.trim(), _content.value.trim())
            }
            result
                .onSuccess { _events.send(InquiryComposeEvent.Created) }
                .onFailure {
                    _events.send(
                        InquiryComposeEvent.Error(
                            (it as? ApiException)?.message ?: "문의 등록에 실패했습니다.",
                        ),
                    )
                }
            _submitting.value = false
        }
    }
}
