package com.elseeker.android.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.app.navigation.DeepLinkManager
import com.elseeker.android.core.auth.AuthState
import com.elseeker.android.core.auth.SessionManager
import com.elseeker.android.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 앱 루트 상태. 콜드 스타트에 세션을 복원하고 전역 [authState] 를 노출한다.
 * 스플래시는 [AuthState.Unknown] 동안 유지된다.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deepLinkManager: DeepLinkManager,
    sessionManager: SessionManager,
) : ViewModel() {

    val authState: StateFlow<AuthState> = sessionManager.authState

    /** App Links 로 보류된 내부 라우트(인증 완료 후 1회 소비). */
    val pendingDeepLink: StateFlow<String?> = deepLinkManager.pendingRoute

    fun consumeDeepLink() = deepLinkManager.consume()

    init { restore() }

    /** 오프라인 상태에서 '다시 시도' 시 세션 복원을 재실행한다(D4). */
    fun retry() = restore()

    private fun restore() {
        viewModelScope.launch { authRepository.restoreSession() }
    }
}
