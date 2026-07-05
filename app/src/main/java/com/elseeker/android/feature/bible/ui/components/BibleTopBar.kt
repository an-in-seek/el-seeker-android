package com.elseeker.android.feature.bible.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elseeker.android.R

/**
 * 성경 4단계 화면 공용 상단바(웹 모바일 헤더 파리티 — `docs/view` 스크린샷 기준).
 * 연한 회색 바 위에 흰색 라운드 사각 버튼들: [뒤로] [KRV ▼] ... [검색] [Aa] [프로필].
 * [title] 을 주면 M3 CenterAlignedTopAppBar 처럼 바 중앙에 화면/브랜드 타이틀을 고정 노출한다
 * (홈 탭부터 적용 중 — 미지정 시 종전처럼 본문 [BiblePageTitle] 로 표시).
 */
@Composable
fun BibleTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    translationCode: String? = null,
    onChangeTranslation: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onFontSizeClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer) {
        // 총 높이 48dp(버튼 40 + 상하 4) — 컴팩트 앱바(Material 아이콘 터치 타깃 48dp 하한).
        Box(modifier = Modifier.fillMaxWidth()) {
            // 좌우에 컨트롤(뒤로/칩/검색/Aa)이 있으면 타이틀을 Row 중앙 가중치 슬롯에 인라인 배치하고,
            // 비어있는 바(홈/학습/번역본)면 화면 정중앙에 오버레이한다(아이콘과 겹치지 않도록).
            val inlineTitle = title != null &&
                (onBack != null || translationCode != null || onSearchClick != null || onFontSizeClick != null)
            // 오버레이 중앙 타이틀 — 좌우 아이콘 영역(≈52dp)을 피하도록 좌우 여백을 두어 광학 중앙 유지.
            if (title != null && !inlineTitle) {
                Text(
                    text = title,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 56.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
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
                // 중앙 슬롯: 인라인 타이틀이면 가중치 중앙 텍스트(긴 이름은 말줄임), 아니면 우측 컨트롤을 미는 스페이서.
                if (inlineTitle) {
                    Text(
                        text = title!!,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
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
}

/**
 * 페이지 타이틀 — 상단바 아래 본문 중앙 정렬(스크린샷의 "성경 번역본"/"개역한글" 등).
 * 다음 요소가 자체 상단 여백(리스트 간격·행 패딩 등)을 가지면 [bottomPadding] 을 줄여
 * 타이틀의 체감 상/하 여백이 같아지도록 화면별로 보정한다.
 */
@Composable
fun BiblePageTitle(
    text: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = 16.dp,
    bottomPadding: Dp = 16.dp,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding),
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
