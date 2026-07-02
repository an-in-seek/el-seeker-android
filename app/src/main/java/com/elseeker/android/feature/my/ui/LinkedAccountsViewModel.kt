package com.elseeker.android.feature.my.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.network.ApiException
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.auth.data.MemberOAuthAccountResponse
import com.elseeker.android.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 연동 계정 관리 ViewModel — /me 로 memberUid 확보 후 연동 계정 목록 로드/해제. */
@HiltViewModel
class LinkedAccountsViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state =
        MutableStateFlow<UiResource<List<MemberOAuthAccountResponse>>>(UiResource.Loading)
    val state: StateFlow<UiResource<List<MemberOAuthAccountResponse>>> = _state.asStateFlow()

    private val _working = MutableStateFlow(false)
    val working: StateFlow<Boolean> = _working.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var memberUid: String? = null

    init { load() }

    fun load() {
        _state.value = UiResource.Loading
        viewModelScope.launch {
            repository.me()
                .onFailure { _state.value = it.toUiError() }
                .onSuccess { me ->
                    memberUid = me.memberUid
                    repository.oauthAccounts(me.memberUid)
                        .onSuccess { _state.value = UiResource.Success(it) }
                        .onFailure { _state.value = it.toUiError() }
                }
        }
    }

    /** 연동 해제. 마지막 1개는 계정 접근 상실 위험이라 서버 검증에 위임하고 결과 메시지를 노출한다. */
    fun unlink(account: MemberOAuthAccountResponse) {
        val uid = memberUid ?: return
        val current = (_state.value as? UiResource.Success)?.data ?: return
        if (_working.value) return
        if (current.size <= 1) {
            viewModelScope.launch { _messages.send("마지막 연동 계정은 해제할 수 없습니다.") }
            return
        }
        _working.value = true
        viewModelScope.launch {
            repository.unlinkOauthAccount(uid, account.provider, account.providerUserId)
                .onSuccess {
                    // 서버 응답(AuthMeResponse)엔 계정 목록이 없으므로 로컬에서 제거한다.
                    // 전체 재조회(me+oauthAccounts)를 하지 않아 '성공 후 오류화면' 상호작용을 피한다.
                    _state.value = UiResource.Success(current - account)
                    _messages.send("연동을 해제했습니다.")
                }
                .onFailure {
                    _messages.send((it as? ApiException)?.message ?: "연동 해제에 실패했습니다.")
                }
            _working.value = false
        }
    }
}
