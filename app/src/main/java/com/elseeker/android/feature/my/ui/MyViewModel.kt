package com.elseeker.android.feature.my.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.network.ApiException
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.auth.data.AuthMeResponse
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

/** 마이 탭의 1회성 이벤트. */
sealed interface MyUiEvent {
    /** 회원 탈퇴 성공 → 로그아웃 경로로 라우팅(소셜 SDK 세션도 정리). */
    data object AccountDeleted : MyUiEvent
    /** 닉네임 저장 성공 → 화면에서 안내 토스트. */
    data object ProfileSaved : MyUiEvent
    data class Message(val text: String) : MyUiEvent
}

/**
 * 마이 탭 ViewModel(웹 mypage 단일 페이지 파리티) — 내 정보 로드,
 * 닉네임 인라인 수정, 연동 계정 목록/해제, 회원 탈퇴(Play 계정삭제 정책 §11)를 한 화면에서 처리한다.
 */
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiResource<AuthMeResponse>>(UiResource.Loading)
    val state: StateFlow<UiResource<AuthMeResponse>> = _state.asStateFlow()

    /** 연동 계정 목록 — /me 성공 후 이어서 로드(비치명적: 실패해도 본문은 유지). */
    private val _accounts = MutableStateFlow<List<MemberOAuthAccountResponse>>(emptyList())
    val accounts: StateFlow<List<MemberOAuthAccountResponse>> = _accounts.asStateFlow()

    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /** 연동 해제 진행 중(중복 요청 방지). */
    private val _working = MutableStateFlow(false)
    val working: StateFlow<Boolean> = _working.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    private val _events = Channel<MyUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // init 로딩 대신 화면 ON_RESUME 에서 로드 — 다른 탭/화면에서 돌아왔을 때 최신 정보 반영.

    fun load() {
        _state.value = UiResource.Loading
        viewModelScope.launch {
            repository.me()
                .onSuccess { me ->
                    _nickname.value = me.nickname
                    _state.value = UiResource.Success(me)
                    // 연동 계정은 후속 조회 — 실패해도 프로필/수정 영역은 그대로 노출한다.
                    repository.oauthAccounts(me.memberUid)
                        .onSuccess { _accounts.value = it }
                        .onFailure { _accounts.value = emptyList() }
                }
                .onFailure { _state.value = it.toUiError() }
        }
    }

    fun onNicknameChange(value: String) {
        if (value.length <= NICKNAME_MAX) _nickname.value = value
    }

    /** 닉네임 저장. 성공 시 프로필 카드에도 즉시 반영한다. */
    fun saveNickname() {
        val me = (_state.value as? UiResource.Success)?.data ?: return
        val nick = _nickname.value.trim()
        if (_saving.value || nick.isBlank()) return
        _saving.value = true
        viewModelScope.launch {
            repository.updateProfile(me.memberUid, nick, me.profileImageUrl)
                .onSuccess {
                    _state.value = UiResource.Success(me.copy(nickname = nick))
                    _events.send(MyUiEvent.ProfileSaved)
                }
                .onFailure {
                    _events.send(MyUiEvent.Message((it as? ApiException)?.message ?: "프로필 저장에 실패했습니다."))
                }
            _saving.value = false
        }
    }

    /** 연동 해제. 최초 가입(signup) 계정은 서버·UX 모두에서 차단(웹 파리티). */
    fun unlink(account: MemberOAuthAccountResponse) {
        val me = (_state.value as? UiResource.Success)?.data ?: return
        if (_working.value) return
        if (account.provider.equals(me.provider, ignoreCase = true)) {
            viewModelScope.launch {
                _events.send(MyUiEvent.Message("최초 가입 계정은 해제할 수 없습니다."))
            }
            return
        }
        _working.value = true
        viewModelScope.launch {
            repository.unlinkOauthAccount(me.memberUid, account.provider, account.providerUserId)
                .onSuccess {
                    // 서버 응답엔 목록이 없으므로 로컬에서 제거(전체 재조회로 인한 '성공 후 오류화면'을 피한다).
                    _accounts.value = _accounts.value - account
                    _events.send(MyUiEvent.Message("연동을 해제했습니다."))
                }
                .onFailure {
                    _events.send(MyUiEvent.Message((it as? ApiException)?.message ?: "연동 해제에 실패했습니다."))
                }
            _working.value = false
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

    private companion object {
        const val NICKNAME_MAX = 50
    }
}
