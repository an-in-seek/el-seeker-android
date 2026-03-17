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
import androidx.lifecycle.lifecycleScope
import com.elseeker.android.ui.screen.MainScreen
import com.elseeker.android.ui.theme.ElSeekerTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
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

    private fun handleSocialLogin(provider: String) {
        when (provider) {
            "google" -> loginWithGoogle()
            "kakao" -> loginWithKakao()
            "naver" -> loginWithNaver()
        }
    }

    private fun loginWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result: GetCredentialResponse = credentialManager.getCredential(
                    context = this@ElSeekerActivity,
                    request = request
                )
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                viewModel.handleSocialLogin("google", googleIdTokenCredential.idToken)
            } catch (_: GetCredentialCancellationException) {
                // 사용자가 로그인을 취소한 경우 무시
            } catch (e: Exception) {
                Log.e("ElSeekerActivity", "Google sign-in failed", e)
                Toast.makeText(this@ElSeekerActivity, "Google 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginWithKakao() {
        UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
            if (error != null) {
                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) return@loginWithKakaoAccount
                Log.e("ElSeekerActivity", "Kakao login failed", error)
                Toast.makeText(this, "카카오 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
            } else if (token != null) {
                viewModel.handleSocialLogin("kakao", token.accessToken)
            }
        }
    }

    private fun loginWithNaver() {
        NaverIdLoginSDK.authenticate(this, object : OAuthLoginCallback {
            override fun onSuccess() {
                val accessToken = NaverIdLoginSDK.getAccessToken()
                if (accessToken != null) {
                    viewModel.handleSocialLogin("naver", accessToken)
                } else {
                    Toast.makeText(this@ElSeekerActivity, "네이버 로그인 토큰을 받지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Log.e("ElSeekerActivity", "Naver login failed: $httpStatus $message")
                Toast.makeText(this@ElSeekerActivity, "네이버 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onError(errorCode: Int, message: String) {
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
