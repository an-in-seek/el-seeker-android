package com.elseeker.android.feature.auth.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elseeker.android.BuildConfig
import com.elseeker.android.R
import com.elseeker.android.core.ui.openExternalUrl

/**
 * 약관 동의 화면(PRD §5.3). 세 항목(이용약관/개인정보/만14세) 모두 필수.
 * 제출 → 정식 토큰 수령, 취소 → signup 회원 삭제 + 로그아웃.
 */
@Composable
fun ConsentScreen(
    busy: Boolean,
    onSubmit: (agreeTerms: Boolean, agreePrivacy: Boolean, ageOver14: Boolean) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var agreeTerms by remember { mutableStateOf(false) }
    var agreePrivacy by remember { mutableStateOf(false) }
    var ageOver14 by remember { mutableStateOf(false) }
    val allChecked = agreeTerms && agreePrivacy && ageOver14
    val context = LocalContext.current
    val baseUrl = BuildConfig.BASE_URL.trimEnd('/')

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.consent_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(28.dp))

        ConsentRow(stringResource(R.string.consent_agree_all), checked = allChecked, bold = true) {
            val next = !allChecked
            agreeTerms = next; agreePrivacy = next; ageOver14 = next
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        // 필수 약관 2종은 동의 전에 원문을 읽을 수 있어야 한다(§5.3·Play 정책) — Custom Tabs 로 열람.
        ConsentRow(
            label = stringResource(R.string.consent_agree_terms),
            checked = agreeTerms,
            onDetail = { openExternalUrl(context, "$baseUrl/web/legal/terms") },
        ) { agreeTerms = !agreeTerms }
        ConsentRow(
            label = stringResource(R.string.consent_agree_privacy),
            checked = agreePrivacy,
            onDetail = { openExternalUrl(context, "$baseUrl/web/legal/privacy") },
        ) { agreePrivacy = !agreePrivacy }
        ConsentRow(stringResource(R.string.consent_agree_age), checked = ageOver14) { ageOver14 = !ageOver14 }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onSubmit(agreeTerms, agreePrivacy, ageOver14) },
            enabled = allChecked && !busy,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (busy) CircularProgressIndicator(strokeWidth = 2.dp) else Text(stringResource(R.string.consent_submit))
        }
        TextButton(
            onClick = onCancel,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConsentRow(
    label: String,
    checked: Boolean,
    bold: Boolean = false,
    onDetail: (() -> Unit)? = null,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            text = label,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (onDetail != null) {
            TextButton(onClick = onDetail) {
                Text(
                    text = stringResource(R.string.consent_view_document),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
