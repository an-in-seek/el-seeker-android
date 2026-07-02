package com.elseeker.android.feature.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elseeker.android.BuildConfig
import com.elseeker.android.R
import com.elseeker.android.core.ui.openExternalUrl
import com.elseeker.android.feature.auth.data.SocialProvider

/**
 * 로그인 화면 — 웹 `templates/login/login.html` 과 동일한 구성:
 * 로고 + 히어로 타이틀 → "간편 로그인" 카드(Kakao/Naver/Google 순, 브랜드 아이콘)
 * → 이용약관·개인정보처리방침·문의하기 링크 → "지금은 둘러보기"(게스트 홈 이동).
 * 소셜 버튼 탭 → [onSocialLogin] 으로 provider 를 전달하면 Activity 가 해당 SDK 를 띄운다.
 */
@Composable
fun LoginScreen(
    busy: Boolean,
    onSocialLogin: (provider: String) -> Unit,
    modifier: Modifier = Modifier,
    onBrowse: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val baseUrl = BuildConfig.BASE_URL.trimEnd('/')

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 히어로 — 로고 + 타이틀 (login-hero)
        Image(
            painter = painterResource(R.drawable.elseeker_login),
            contentDescription = stringResource(R.string.login_logo_desc),
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.login_title),
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(32.dp))

        // 간편 로그인 카드 (login-card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.login_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(20.dp))

                // 웹과 동일한 순서: Kakao → Naver → Google
                SocialButton(
                    text = stringResource(R.string.login_kakao_continue),
                    iconRes = R.drawable.ic_login_kakao,
                    container = Color(0xFFFEE500),
                    content = Color(0xFF000000),
                    enabled = !busy,
                ) { onSocialLogin(SocialProvider.KAKAO) }
                Spacer(Modifier.height(12.dp))
                SocialButton(
                    text = stringResource(R.string.login_naver_continue),
                    iconRes = R.drawable.ic_login_naver,
                    container = Color(0xFF03C75A),
                    content = Color.White,
                    enabled = !busy,
                ) { onSocialLogin(SocialProvider.NAVER) }
                Spacer(Modifier.height(12.dp))
                SocialButton(
                    text = stringResource(R.string.login_google_continue),
                    iconRes = R.drawable.ic_login_google,
                    container = Color.White,
                    content = Color(0xFF3C4043),
                    enabled = !busy,
                ) { onSocialLogin(SocialProvider.GOOGLE) }

                if (busy) {
                    Spacer(Modifier.height(20.dp))
                    CircularProgressIndicator()
                }

                Spacer(Modifier.height(20.dp))
                // 약관·개인정보·문의 링크 (login-meta) — Custom Tabs 위임.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetaLink(stringResource(R.string.login_terms)) {
                        openExternalUrl(context, "$baseUrl/web/legal/terms")
                    }
                    MetaDot()
                    MetaLink(stringResource(R.string.login_privacy)) {
                        openExternalUrl(context, "$baseUrl/web/legal/privacy")
                    }
                    MetaDot()
                    MetaLink(stringResource(R.string.login_contact)) {
                        openExternalUrl(context, "$baseUrl/web/support/contact")
                    }
                }

                // 지금은 둘러보기 (login-skip) — 게스트로 홈 탐색.
                if (onBrowse != null) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.clickable(enabled = !busy, onClick = onBrowse),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.login_browse),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialButton(
    text: String,
    iconRes: Int,
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
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MetaLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
private fun MetaDot() {
    Text(
        text = "·",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
