package com.elseeker.android.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.app.navigation.Routes
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.feature.bible.data.DailyVerseDto

/** 홈 탭: 경량 브랜딩 히어로 + 오늘의 말씀 + 메뉴 그리드(진입 허브). */
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 히어로 (3D 우주 대체 경량 비주얼 — PRD §4-A.4-1)
        item(span = { GridItemSpan(maxLineSpan) }) { HeroBanner() }

        // 오늘의 말씀 — 로딩/오류 시에도 홈 레이아웃을 막지 않도록 카드 내부에서만 분기
        item(span = { GridItemSpan(maxLineSpan) }) {
            when (val s = state) {
                is UiResource.Success -> DailyVerseCard(s.data)
                is UiResource.Error -> DailyVersePlaceholder(s.message)
                UiResource.Loading -> DailyVersePlaceholder(stringResource(R.string.home_daily_verse_loading))
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.home_menu_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        items(HOME_MENU, key = { it.route }) { entry ->
            MenuCard(entry = entry, onClick = { onNavigate(entry.route) })
        }

        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(8.dp)) }
    }
}

private data class HomeMenuEntry(@StringRes val labelRes: Int, val icon: ImageVector, val route: String)

private val HOME_MENU = listOf(
    HomeMenuEntry(R.string.home_menu_bible_read, Icons.AutoMirrored.Outlined.MenuBook, Routes.BIBLE),
    HomeMenuEntry(R.string.home_menu_bible_search, Icons.Outlined.Search, Routes.BIBLE_SEARCH),
    HomeMenuEntry(R.string.home_menu_study, Icons.Outlined.School, Routes.STUDY),
    HomeMenuEntry(R.string.home_menu_dictionary, Icons.Outlined.Book, Routes.STUDY_DICTIONARY),
    HomeMenuEntry(R.string.home_menu_my, Icons.Outlined.Person, Routes.MY),
)

@Composable
private fun HeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ElSeeker",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = stringResource(R.string.home_hero_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun DailyVerseCard(verse: DailyVerseDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_daily_verse_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = verse.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${verse.bookName} ${verse.chapterNumber}:${verse.verseNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun DailyVersePlaceholder(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
private fun MenuCard(entry: HomeMenuEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(entry.labelRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
