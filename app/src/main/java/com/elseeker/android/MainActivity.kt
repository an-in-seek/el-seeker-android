package com.elseeker.android

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import android.content.Intent
import com.elseeker.android.app.AppViewModel
import com.elseeker.android.app.ElSeekerApp
import com.elseeker.android.app.navigation.DeepLinkManager
import com.elseeker.android.core.auth.AuthState
import com.elseeker.android.feature.auth.data.SocialProvider
import com.elseeker.android.feature.auth.ui.AuthUiEvent
import com.elseeker.android.feature.auth.ui.AuthViewModel
import com.elseeker.android.ui.theme.ElSeekerTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 단일 Activity. v1 전부 네이티브(WebView 미사용 — PRD §4-A).
 * 화면 전환은 Compose Navigation([ElSeekerApp]) 이 담당하고, Activity 는
 * 소셜 SDK 호출(Activity 컨텍스트 필요)과 스플래시/인앱 업데이트만 담당한다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var deepLinkManager: DeepLinkManager

    private val appViewModel: AppViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* flexible update: 결과 처리 불필요 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            appViewModel.authState.value == AuthState.Unknown
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )

        super.onCreate(savedInstanceState)

        credentialManager = CredentialManager.create(this)
        deepLinkManager.onUri(intent?.data)
        checkForAppUpdate()
        observeAuthEvents()

        setContent {
            ElSeekerTheme {
                ElSeekerApp(
                    appViewModel = appViewModel,
                    authViewModel = authViewModel,
                    onSocialLogin = ::startSocialLogin,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 앱 실행 중 들어온 App Link 처리(singleTask 라 새 인스턴스 대신 이 콜백으로 전달됨).
        deepLinkManager.onUri(intent.data)
    }

    private fun observeAuthEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.events.collect { event ->
                    when (event) {
                        is AuthUiEvent.Message ->
                            Toast.makeText(this@MainActivity, event.text, Toast.LENGTH_SHORT).show()
                        AuthUiEvent.SocialLogout -> clearSocialSessions()
                    }
                }
            }
        }
    }

    private fun startSocialLogin(provider: String) {
        when (provider) {
            SocialProvider.GOOGLE -> loginWithGoogle()
            SocialProvider.KAKAO -> loginWithKakao()
            SocialProvider.NAVER -> loginWithNaver()
        }
    }

    private fun loginWithGoogle() {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        lifecycleScope.launch {
            val idOption = GetGoogleIdOption.Builder()
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val handled = tryGoogleCredential(
                GetCredentialRequest.Builder().addCredentialOption(idOption).build(),
                stage = "GoogleIdOption",
            )
            if (handled) return@launch

            val signInOption = GetSignInWithGoogleOption.Builder(clientId).build()
            tryGoogleCredential(
                GetCredentialRequest.Builder().addCredentialOption(signInOption).build(),
                stage = "SignInWithGoogle",
                finalStage = true,
            )
        }
    }

    /** @return true → 흐름 종료, false → NoCredential 이므로 폴백 시도. */
    private suspend fun tryGoogleCredential(
        request: GetCredentialRequest,
        stage: String,
        finalStage: Boolean = false,
    ): Boolean {
        return try {
            val result: GetCredentialResponse = credentialManager.getCredential(this, request)
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            authViewModel.onSocialToken(SocialProvider.GOOGLE, credential.idToken)
            true
        } catch (_: GetCredentialCancellationException) {
            Log.i(TAG, "Google login cancelled ($stage)")
            true
        } catch (e: NoCredentialException) {
            Log.w(TAG, "Google NoCredential ($stage): ${e.message}")
            if (finalStage) {
                toast(getString(R.string.auth_google_no_account))
                true
            } else {
                false
            }
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google sign-in failed ($stage): ${e.javaClass.simpleName}", e)
            toast(getString(R.string.auth_google_login_failed))
            true
        }
    }

    private fun loginWithKakao() {
        UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
            if (error != null) {
                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) return@loginWithKakaoAccount
                Log.e(TAG, "Kakao login failed: ${error.message}", error)
                toast(getString(R.string.auth_kakao_login_failed))
            } else if (token != null) {
                authViewModel.onSocialToken(SocialProvider.KAKAO, token.accessToken)
            }
        }
    }

    private fun loginWithNaver() {
        NaverIdLoginSDK.authenticate(this, object : OAuthLoginCallback {
            override fun onSuccess() {
                val accessToken = NaverIdLoginSDK.getAccessToken()
                if (accessToken != null) {
                    authViewModel.onSocialToken(SocialProvider.NAVER, accessToken)
                } else {
                    toast(getString(R.string.auth_naver_no_token))
                }
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Log.e(TAG, "Naver login failure: $httpStatus $message / ${NaverIdLoginSDK.getLastErrorDescription()}")
                toast(getString(R.string.auth_naver_login_failed))
            }

            override fun onError(errorCode: Int, message: String) = onFailure(errorCode, message)
        })
    }

    /**
     * 명시적 로그아웃 시 소셜 SDK 세션을 모두 폐기한다.
     * 이렇게 해야 다음 로그인에서 자동 재로그인이 아니라 계정 선택이 가능하다.
     * (세션 복원 중 일시적 네트워크 실패에서는 호출되지 않는다 — 명시적 이벤트 기반)
     */
    private fun clearSocialSessions() {
        runCatching { UserApiClient.instance.logout { } }
        runCatching { NaverIdLoginSDK.logout() }
        lifecycleScope.launch {
            runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
        }
    }

    private fun checkForAppUpdate() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    info, updateLauncher, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                )
            }
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val TAG = "ElSeekerAuth"
    }
}
