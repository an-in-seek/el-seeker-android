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

sealed interface ProfileEditEvent {
    data object Saved : ProfileEditEvent
    data class Error(val message: String) : ProfileEditEvent
}

/** 프로필 수정 ViewModel — /me 로 현재 값을 프리필하고 닉네임을 저장한다. */
@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiResource<AuthMeResponse>>(UiResource.Loading)
    val state: StateFlow<UiResource<AuthMeResponse>> = _state.asStateFlow()

    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _events = Channel<ProfileEditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { load() }

    fun load() {
        _state.value = UiResource.Loading
        viewModelScope.launch {
            repository.me()
                .onSuccess {
                    _nickname.value = it.nickname
                    _state.value = UiResource.Success(it)
                }
                .onFailure { _state.value = it.toUiError() }
        }
    }

    fun onNicknameChange(value: String) { _nickname.value = value }

    fun save() {
        val me = (_state.value as? UiResource.Success)?.data ?: return
        val nick = _nickname.value.trim()
        if (_saving.value || nick.isBlank()) return
        _saving.value = true
        viewModelScope.launch {
            repository.updateProfile(me.memberUid, nick, me.profileImageUrl)
                .onSuccess { _events.send(ProfileEditEvent.Saved) }
                .onFailure {
                    _events.send(
                        ProfileEditEvent.Error(
                            (it as? ApiException)?.message ?: "프로필 저장에 실패했습니다.",
                        ),
                    )
                }
            _saving.value = false
        }
    }
}
