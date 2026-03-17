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
import androidx.lifecycle.lifecycleScope
import com.elseeker.android.ui.screen.MainScreen
import com.elseeker.android.ui.theme.ElSeekerTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ElSeekerActivity : ComponentActivity() {

    private val viewModel: ElSeekerViewModel by viewModels()
    private var backPressedOnce = false

    private lateinit var googleSignInClient: GoogleSignInClient

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* Update flow result - no action needed for flexible updates */ }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.handleSocialLogin("google", idToken)
            } else {
                Toast.makeText(this, "Google 로그인 토큰을 받지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            // 사용자가 로그인을 취소한 경우 (statusCode 12501) 무시
            if (e.statusCode != 12501) {
                Log.e("ElSeekerActivity", "Google sign-in failed: ${e.statusCode}", e)
                Toast.makeText(this, "Google 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

        initGoogleSignIn()
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

    private fun initGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun handleSocialLogin(provider: String) {
        when (provider) {
            "google" -> loginWithGoogle()
            "kakao" -> loginWithKakao()
            "naver" -> loginWithNaver()
        }
    }

    private fun loginWithGoogle() {
        // 기존 로그인 세션을 먼저 해제하여 계정 선택 화면을 항상 표시
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun loginWithKakao() {
        UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
            if (error != null) {
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
