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
import com.elseeker.android.feature.auth.ui.LoginScreen
import com.elseeker.android.ui.screen.OfflineScreen

/**
 * 앱 루트. 전역 [AuthState] 에 따라 로그인/동의/메인을 전환한다(PRD §5.4).
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

            AuthState.Unauthenticated -> LoginScreen(
                busy = busy,
                onSocialLogin = onSocialLogin,
            )

            AuthState.Offline -> OfflineScreen(onRetry = appViewModel::retry)

            AuthState.NeedsConsent -> ConsentScreen(
                busy = busy,
                onSubmit = authViewModel::submitConsent,
                onCancel = authViewModel::cancelConsent,
            )

            AuthState.Authenticated -> MainScaffold(
                onLoggedOut = authViewModel::logout,
                pendingDeepLink = pendingDeepLink,
                onDeepLinkConsumed = appViewModel::consumeDeepLink,
            )
        }
    }
}
