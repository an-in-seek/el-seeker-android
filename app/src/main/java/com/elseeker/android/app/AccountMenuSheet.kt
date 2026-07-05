package com.elseeker.android.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elseeker.android.R
import com.elseeker.android.core.ui.ForceBlackNavigationBarInDialog
import com.elseeker.android.ui.theme.ThemeMode

/**
 * 계정 메뉴 바텀시트(웹 account-menu 파리티) — 상단바 프로필 아이콘으로 연다.
 * 로그인 상태에 따라 항목이 갈리고, 하단에 라이트/다크/시스템 테마 전환을 제공한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountMenuSheet(
    loggedIn: Boolean,
    themeMode: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    onLogin: () -> Unit,
    onMyPage: () -> Unit,
    onMyMemos: () -> Unit,
    onInquiries: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // 콘텐츠 자동 하단 인셋을 끄고, 내비바 영역은 아래 검정 Box 로 직접 채운다(Android 15+ 대응).
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        // 바텀시트는 별도 윈도우라 하단 내비바가 흰색으로 뜬다.
        // Android 14 이하는 윈도우 색으로, Android 15+ 는 색상 API 가 무시되므로 아래 검정 Box 로 맞춘다.
        ForceBlackNavigationBarInDialog()
        // 닫기는 드래그 핸들/스크림/스와이프로 처리한다(네이티브 관행) — 별도 × 버튼 미사용.
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp)) {
            if (loggedIn) {
                AccountRow(stringResource(R.string.account_mypage), onMyPage)
                AccountRow(stringResource(R.string.account_my_memos), onMyMemos)
                AccountRow(stringResource(R.string.account_inquiries), onInquiries)
            } else {
                AccountRow(stringResource(R.string.account_login), onLogin)
            }

            ThemeSection(themeMode = themeMode, onSelectTheme = onSelectTheme)

            if (loggedIn) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AccountRow(stringResource(R.string.account_logout), onLogout)
            }
        }
        // 하단 시스템 내비게이션 바 영역을 검정으로 채운다(시트가 그 뒤로 그리는 흰 배경을 덮음).
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(Color.Black),
        )
    }
}

/** 일반 메뉴 행 — 좌측 정렬 라벨, 전체 폭 클릭. */
@Composable
private fun AccountRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    )
}

/** 테마 섹션 — "테마" 헤더(현재 모드 이모지+접기 화살표) + 라이트/다크/시스템 옵션. */
@Composable
private fun ThemeSection(themeMode: ThemeMode, onSelectTheme: (ThemeMode) -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    val chevron by animateFloatAsState(if (expanded) 0f else 180f, label = "themeChevron")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.account_theme),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(text = themeMode.emoji(), fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = stringResource(R.string.account_theme_expand),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { rotationZ = chevron },
        )
    }
    AnimatedVisibility(visible = expanded) {
        Column {
            ThemeOption(ThemeMode.LIGHT, "🌞", stringResource(R.string.account_theme_light), themeMode, onSelectTheme)
            ThemeOption(ThemeMode.DARK, "🌙", stringResource(R.string.account_theme_dark), themeMode, onSelectTheme)
            ThemeOption(ThemeMode.SYSTEM, "🖥️", stringResource(R.string.account_theme_system), themeMode, onSelectTheme)
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    emoji: String,
    label: String,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val selected = mode == current
    val selectedDesc = stringResource(R.string.account_theme_selected)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) }
            .padding(start = 32.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = selectedDesc,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun ThemeMode.emoji(): String = when (this) {
    ThemeMode.LIGHT -> "🌞"
    ThemeMode.DARK -> "🌙"
    ThemeMode.SYSTEM -> "🖥️"
}
