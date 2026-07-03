package com.elseeker.android.feature.bible.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elseeker.android.R

/**
 * 성경 4단계 화면 공용 상단바(웹 모바일 헤더 파리티 — `docs/view` 스크린샷 기준).
 * 연한 회색 바 위에 흰색 라운드 사각 버튼들: [뒤로] [KRV ▼] ... [검색] [Aa] [프로필].
 * 페이지 타이틀은 이 바에 넣지 않고 본문 상단 중앙에 [BiblePageTitle] 로 표시한다.
 */
@Composable
fun BibleTopBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    translationCode: String? = null,
    onChangeTranslation: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onFontSizeClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer) {
        // 총 높이 56dp(버튼 40 + 상하 8) — 전통 툴바 표준이자 하단 탭(56dp)과 동일, 웹 헤더(52px) 근접.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                TopBarIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    onClick = onBack,
                )
                Spacer(Modifier.width(8.dp))
            }
            if (translationCode != null && onChangeTranslation != null) {
                TranslationChip(code = translationCode, onClick = onChangeTranslation)
            }
            Spacer(Modifier.weight(1f))
            if (onSearchClick != null) {
                TopBarIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = stringResource(R.string.bible_topbar_search),
                    onClick = onSearchClick,
                )
                Spacer(Modifier.width(8.dp))
            }
            if (onFontSizeClick != null) {
                TopBarBox(onClick = onFontSizeClick) {
                    Text(
                        text = stringResource(R.string.bible_topbar_font_size_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            if (onProfileClick != null) {
                TopBarIconButton(
                    icon = Icons.Default.Person,
                    contentDescription = stringResource(R.string.bible_topbar_profile),
                    onClick = onProfileClick,
                )
            }
        }
    }
}

/** 페이지 타이틀 — 상단바 아래 본문 중앙 정렬(스크린샷의 "성경 번역본"/"개역한글" 등). */
@Composable
fun BiblePageTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

/** 번역본 변경 칩: 흰색 라운드 보더 박스 안 `KRV ▼`. */
@Composable
private fun TranslationChip(code: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.bible_topbar_change_translation),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TopBarIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    TopBarBox(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun TopBarBox(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
