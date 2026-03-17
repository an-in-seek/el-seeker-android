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
        Log.i(TAG, "Google login started - clientId: ${BuildConfig.GOOGLE_WEB_CLIENT_ID.take(8)}***")
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
                Log.i(TAG, "Google login success - idToken present: ${googleIdTokenCredential.idToken.isNotEmpty()}")
                viewModel.handleSocialLogin("google", googleIdTokenCredential.idToken)
            } catch (_: GetCredentialCancellationException) {
                Log.i(TAG, "Google login cancelled by user")
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in failed: ${e.javaClass.simpleName} - ${e.message}", e)
                Toast.makeText(this@ElSeekerActivity, "Google 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginWithKakao() {
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
                viewModel.handleSocialLogin("kakao", token.accessToken)
            }
        }
    }

    private fun loginWithNaver() {
        Log.i(TAG, "Naver login started - clientId: ${BuildConfig.NAVER_CLIENT_ID.take(4)}***, initialized: ${NaverIdLoginSDK.getState()}")
        NaverIdLoginSDK.authenticate(this, object : OAuthLoginCallback {
            override fun onSuccess() {
                val accessToken = NaverIdLoginSDK.getAccessToken()
                Log.i(TAG, "Naver login success - token present: ${accessToken != null}")
                if (accessToken != null) {
                    viewModel.handleSocialLogin("naver", accessToken)
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
