package com.elseeker.android.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.core.auth.AuthState
import com.elseeker.android.core.ui.LoadingBox
import com.elseeker.android.feature.auth.ui.AuthViewModel
import com.elseeker.android.feature.auth.ui.ConsentScreen

/**
 * 앱 루트. 웹(the_bible_project)과 동일하게 **비로그인도 홈부터** 탐색한다:
 * 콜드 스타트 후 곧바로 메인 셸(홈/성경/학습)을 띄우고, 로그인은 마이 탭에서 수행한다.
 * 유일한 전면 게이트는 가입 중 약관 동의([AuthState.NeedsConsent]) 뿐이다(PRD §5.3).
 * 소셜 SDK 호출은 Activity 만 가능하므로 [onSocialLogin] 콜백으로 위임한다.
 */
@Composable
fun ElSeekerApp(
    appViewModel: AppViewModel,
    authViewModel: AuthViewModel,
    onSocialLogin: (provider: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val authState by appViewModel.authState.collectAsStateWithLifecycle()
    val busy by authViewModel.busy.collectAsStateWithLifecycle()
    val pendingDeepLink by appViewModel.pendingDeepLink.collectAsStateWithLifecycle()

    Surface(modifier = modifier.fillMaxSize()) {
        when (authState) {
            AuthState.Unknown -> LoadingBox()

            AuthState.NeedsConsent -> ConsentScreen(
                busy = busy,
                onSubmit = authViewModel::submitConsent,
                onCancel = authViewModel::cancelConsent,
            )

            // Unauthenticated(게스트)·Offline(세션 복원 보류)·Authenticated 모두 홈부터.
            else -> MainScaffold(
                authState = authState,
                loginBusy = busy,
                onSocialLogin = onSocialLogin,
                onRetrySession = appViewModel::retry,
                onLoggedOut = authViewModel::logout,
                pendingDeepLink = pendingDeepLink,
                onDeepLinkConsumed = appViewModel::consumeDeepLink,
            )
        }
    }
}
