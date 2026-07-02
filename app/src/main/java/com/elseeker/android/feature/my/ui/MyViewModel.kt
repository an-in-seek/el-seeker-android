package com.elseeker.android.feature.my.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.network.ApiException
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.auth.data.AuthMeResponse
import com.elseeker.android.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 마이 탭의 1회성 이벤트. */
sealed interface MyUiEvent {
    /** 회원 탈퇴 성공 → 로그아웃 경로로 라우팅(소셜 SDK 세션도 정리). */
    data object AccountDeleted : MyUiEvent
    data class Message(val text: String) : MyUiEvent
}

/** 마이 탭 ViewModel — 내 정보 로드 및 회원 탈퇴(Play 계정삭제 정책 §11). */
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiResource<AuthMeResponse>>(UiResource.Loading)
    val state: StateFlow<UiResource<AuthMeResponse>> = _state.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    private val _events = Channel<MyUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // init 로딩 대신 화면 ON_RESUME 에서 로드 — 프로필 수정 후 돌아왔을 때 최신 정보 반영.

    fun load() {
        _state.value = UiResource.Loading
        viewModelScope.launch {
            repository.me()
                .onSuccess { _state.value = UiResource.Success(it) }
                .onFailure { _state.value = it.toUiError() }
        }
    }

    /** 회원 탈퇴. 성공 시 로컬 세션은 repository 에서 폐기되고 [MyUiEvent.AccountDeleted] 를 보낸다. */
    fun deleteAccount() {
        val me = (_state.value as? UiResource.Success)?.data ?: return
        if (_deleting.value) return
        _deleting.value = true
        viewModelScope.launch {
            repository.deleteMember(me.memberUid)
                .onSuccess { _events.send(MyUiEvent.AccountDeleted) }
                .onFailure {
                    _events.send(MyUiEvent.Message((it as? ApiException)?.message ?: "회원 탈퇴에 실패했습니다."))
                }
            _deleting.value = false
        }
    }
}
