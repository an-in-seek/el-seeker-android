package com.elseeker.android.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elseeker.android.R
import com.elseeker.android.feature.auth.data.SocialProvider

/**
 * 네이티브 로그인 화면. 소셜 버튼 탭 → [onSocialLogin] 으로 provider 를 전달하면
 * Activity 가 해당 SDK(Google CM/Kakao/Naver)를 띄운다.
 */
@Composable
fun LoginScreen(
    busy: Boolean,
    onSocialLogin: (provider: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "ElSeeker",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(48.dp))

        SocialButton(
            text = stringResource(R.string.login_google_continue),
            container = Color.White,
            content = Color(0xFF1F1F1F),
            enabled = !busy,
        ) { onSocialLogin(SocialProvider.GOOGLE) }
        Spacer(Modifier.height(12.dp))
        SocialButton(
            text = stringResource(R.string.login_kakao_continue),
            container = Color(0xFFFEE500),
            content = Color(0xFF191600),
            enabled = !busy,
        ) { onSocialLogin(SocialProvider.KAKAO) }
        Spacer(Modifier.height(12.dp))
        SocialButton(
            text = stringResource(R.string.login_naver_continue),
            container = Color(0xFF03C75A),
            content = Color.White,
            enabled = !busy,
        ) { onSocialLogin(SocialProvider.NAVER) }

        Spacer(Modifier.height(32.dp))
        if (busy) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun SocialButton(
    text: String,
    container: Color,
    content: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}
