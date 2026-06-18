package com.elseeker.android

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.elseeker.android.ui.screen.MainScreen
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ElSeekerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ElSeekerAuth"
    }

    private val viewModel: ElSeekerViewModel by viewModels()
    private var backPressedOnce = false
    private lateinit var credentialManager: CredentialManager

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* Update flow result - no action needed for flexible updates */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value is UiState.Loading
        }

        // 시스템 바 아이콘 명암을 OS 다크/라이트 테마에 맞춰 자동 전환한다.
        // (auto = 라이트 테마면 어두운 아이콘, 다크 테마면 밝은 아이콘)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        super.onCreate(savedInstanceState)

        credentialManager = CredentialManager.create(this)
        checkForAppUpdate()

        setContent {
            ElSeekerTheme {
                MainScreen(
                    viewModel = viewModel,
                    onBackPressed = ::handleBackPress,
                    onSocialLoginRequested = ::handleSocialLogin
                )
            }
        }
    }

    private fun handleSocialLogin(provider: String, isLink: Boolean) {
        when (provider) {
            "google" -> loginWithGoogle(isLink)
            "kakao" -> loginWithKakao(isLink)
            "naver" -> loginWithNaver(isLink)
        }
    }

    private fun loginWithGoogle(isLink: Boolean) {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        // serverClientId 는 반드시 "웹 애플리케이션" 클라이언트여야 한다(Android 클라이언트 X).
        Log.i(
            TAG,
            "Google login started - clientId: ${clientId.take(12)}..., " +
                "isWebClient: ${clientId.endsWith("apps.googleusercontent.com")}"
        )

        lifecycleScope.launch {
            // 1차: 기기에 등록된 Google 계정 바텀시트(모든 계정 노출).
            val idOption = GetGoogleIdOption.Builder()
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val handled = tryGoogleCredential(
                GetCredentialRequest.Builder().addCredentialOption(idOption).build(),
                isLink,
                stage = "GoogleIdOption"
            )
            if (handled) return@launch

            // 2차: 1차에서 자격증명이 없을 때 명시적 "Sign in with Google" 흐름으로 폴백.
            // (계정 선택/추가를 유도하므로 NoCredential 상황에 더 강건하다.)
            val signInOption = GetSignInWithGoogleOption.Builder(clientId).build()
            tryGoogleCredential(
                GetCredentialRequest.Builder().addCredentialOption(signInOption).build(),
                isLink,
                stage = "SignInWithGoogle",
                finalStage = true
            )
        }
    }

    /**
     * Google 자격증명 요청을 1회 시도한다.
     * @return true  → 성공/사용자취소/최종실패로 흐름 종료
     *         false → NoCredential 이므로 호출자가 다음 단계(폴백)를 시도해야 함
     */
    private suspend fun tryGoogleCredential(
        request: GetCredentialRequest,
        isLink: Boolean,
        stage: String,
        finalStage: Boolean = false
    ): Boolean {
        return try {
            val result: GetCredentialResponse = credentialManager.getCredential(
                context = this@ElSeekerActivity,
                request = request
            )
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            Log.i(TAG, "Google login success ($stage) - idToken present: ${credential.idToken.isNotEmpty()}")
            viewModel.handleSocialLogin("google", credential.idToken, isLink)
            true
        } catch (_: GetCredentialCancellationException) {
            Log.i(TAG, "Google login cancelled by user ($stage)")
            true
        } catch (e: NoCredentialException) {
            // 자격증명 없음. 원인: ① 기기에 Google 계정 없음 ② 이 빌드 서명키 SHA-1이
            // GCP Android OAuth 클라이언트에 미등록(릴리즈/Play 앱 서명 키). 코드로는 해결 불가.
            Log.w(TAG, "Google login NoCredential ($stage): ${e.message}")
            if (finalStage) {
                Toast.makeText(
                    this@ElSeekerActivity,
                    "사용 가능한 Google 계정이 없습니다. 기기에 Google 계정이 추가되어 있는지 확인해 주세요.",
                    Toast.LENGTH_LONG
                ).show()
                true
            } else {
                false
            }
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google sign-in failed ($stage): ${e.javaClass.simpleName} - ${e.message}", e)
            Toast.makeText(this@ElSeekerActivity, "Google 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun loginWithKakao(isLink: Boolean) {
        Log.i(TAG, "Kakao login started")
        UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
            if (error != null) {
                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                    Log.i(TAG, "Kakao login cancelled by user")
                    return@loginWithKakaoAccount
                }
                Log.e(TAG, "Kakao login failed: ${error.javaClass.simpleName} - ${error.message}", error)
                Toast.makeText(this, "카카오 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
            } else if (token != null) {
                Log.i(TAG, "Kakao login success - accessToken present: ${token.accessToken.isNotEmpty()}")
                viewModel.handleSocialLogin("kakao", token.accessToken, isLink)
            }
        }
    }

    private fun loginWithNaver(isLink: Boolean) {
        Log.i(TAG, "Naver login started - clientId: ${BuildConfig.NAVER_CLIENT_ID.take(4)}***, initialized: ${NaverIdLoginSDK.getState()}")
        NaverIdLoginSDK.authenticate(this, object : OAuthLoginCallback {
            override fun onSuccess() {
                val accessToken = NaverIdLoginSDK.getAccessToken()
                Log.i(TAG, "Naver login success - token present: ${accessToken != null}")
                if (accessToken != null) {
                    viewModel.handleSocialLogin("naver", accessToken, isLink)
                } else {
                    Toast.makeText(this@ElSeekerActivity, "네이버 로그인 토큰을 받지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(httpStatus: Int, message: String) {
                val lastError = NaverIdLoginSDK.getLastErrorCode()
                val lastErrorDesc = NaverIdLoginSDK.getLastErrorDescription()
                Log.e(TAG, "Naver login failure - httpStatus: $httpStatus, message: $message, lastError: $lastError, lastErrorDesc: $lastErrorDesc")
                Toast.makeText(this@ElSeekerActivity, "네이버 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onError(errorCode: Int, message: String) {
                Log.e(TAG, "Naver login error - errorCode: $errorCode, message: $message")
                onFailure(errorCode, message)
            }
        })
    }

    private fun handleBackPress(canGoBack: Boolean, goBack: () -> Unit) {
        when {
            canGoBack -> goBack()
            else -> {
                if (backPressedOnce) {
                    finish()
                } else {
                    backPressedOnce = true
                    Toast.makeText(this, getString(R.string.back_press_exit), Toast.LENGTH_SHORT).show()
                    lifecycleScope.launch {
                        delay(2000)
                        backPressedOnce = false
                    }
                }
            }
        }
    }

    private fun checkForAppUpdate() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)
                )
            }
        }
    }
}
