package com.elseeker.android.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.elseeker.android.BuildConfig
import com.elseeker.android.R
import com.elseeker.android.app.navigation.Routes
import com.elseeker.android.core.ui.openExternalUrl
import kotlinx.coroutines.delay

/**
 * 홈 탭 — 웹 `templates/index.html` 과 동일한 구성:
 * 히어로 캐러셀(2장) → 통합 검색바 → 메뉴 카드(성경/학습/게임/커뮤니티)
 * → 인기 검색어 2카드(구절/사전) → 창조 섹션("태초에…").
 * 게임·커뮤니티는 v1 네이티브 범위 밖이라 웹과 동일 화면을 Custom Tabs 로 위임하고,
 * 3D 우주 캔버스는 그라데이션 섹션으로 대체한다(PRD §4-A.4-1 경량 비주얼).
 */
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val bibleRanking by viewModel.bibleRanking.collectAsStateWithLifecycle()
    val dictionaryRanking by viewModel.dictionaryRanking.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val baseUrl = BuildConfig.BASE_URL.trimEnd('/')

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1) 히어로 캐러셀 (home-hero)
        item { HeroCarousel(baseUrl = baseUrl, onCta = { onNavigate(Routes.BIBLE) }) }

        // 2) 통합 검색바 (home-unified-search) — v1 은 성경 절 검색으로 위임.
        item { UnifiedSearchBar(onClick = { onNavigate(Routes.bibleSearch()) }) }

        // 3) 메뉴 카드 (home-menu-grid) — 모바일 웹과 동일한 가로형 풀폭 카드.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeMenuCard(
                    emoji = "📖",
                    title = stringResource(R.string.home_menu_bible),
                    titleEn = stringResource(R.string.home_menu_bible_en),
                    description = stringResource(R.string.home_menu_bible_desc),
                    onClick = { onNavigate(Routes.BIBLE) },
                )
                HomeMenuCard(
                    emoji = "📚",
                    title = stringResource(R.string.home_menu_study),
                    titleEn = stringResource(R.string.home_menu_study_en),
                    description = stringResource(R.string.home_menu_study_desc),
                    onClick = { onNavigate(Routes.STUDY) },
                )
                HomeMenuCard(
                    emoji = "🎮",
                    title = stringResource(R.string.home_menu_game),
                    titleEn = stringResource(R.string.home_menu_game_en),
                    description = stringResource(R.string.home_menu_game_desc),
                    onClick = { openExternalUrl(context, "$baseUrl/web/game") },
                )
                HomeMenuCard(
                    emoji = "💬",
                    title = stringResource(R.string.home_menu_community),
                    titleEn = stringResource(R.string.home_menu_community_en),
                    description = stringResource(R.string.home_menu_community_desc),
                    onClick = { openExternalUrl(context, "$baseUrl/web/community") },
                )
            }
        }

        // 4) 인기 검색어 (home-popular-search) — 웹처럼 데이터 없으면 카드 숨김.
        if (bibleRanking.isNotEmpty()) {
            item {
                PopularSearchCard(
                    title = stringResource(R.string.home_popular_bible_title),
                    items = bibleRanking.map { it.rank to it.keyword },
                    onKeywordClick = { keyword -> onNavigate(Routes.bibleSearch(keyword)) },
                )
            }
        }
        if (dictionaryRanking.isNotEmpty()) {
            item {
                PopularSearchCard(
                    title = stringResource(R.string.home_popular_dictionary_title),
                    items = dictionaryRanking.map { it.rank to it.keyword },
                    onKeywordClick = { keyword -> onNavigate(Routes.studyDictionary(keyword)) },
                )
            }
        }

        // 5) 창조 섹션 (universe-section) — 3D 캔버스 대신 그라데이션.
        item { UniverseSection(onCta = { onNavigate(Routes.studyContent("creation")) }) }
    }
}

private val HERO_IMAGES = listOf("/images/thebible1.png", "/images/thebible2.png")

@Composable
private fun HeroCarousel(baseUrl: String, onCta: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { HERO_IMAGES.size })

    // 웹 캐러셀처럼 자동 롤링.
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % HERO_IMAGES.size)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = "$baseUrl${HERO_IMAGES[page]}",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // 오버레이 — 텍스트 가독성 확보(home-hero-overlay).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.home_hero_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_hero_sub),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = onCta) {
                Text(stringResource(R.string.home_hero_cta))
                Spacer(Modifier.width(4.dp))
                Text("›", fontWeight = FontWeight.Bold)
            }
        }
        // 인디케이터 점(home-hero-indicators).
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(HERO_IMAGES.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) Color.White
                            else Color.White.copy(alpha = 0.45f),
                        ),
                )
            }
        }
    }
}

@Composable
private fun UnifiedSearchBar(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.home_search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeMenuCard(
    emoji: String,
    title: String,
    titleEn: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = titleEn,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PopularSearchCard(
    title: String,
    items: List<Pair<Int, String>>,
    onKeywordClick: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            items.forEach { (rank, keyword) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onKeywordClick(keyword) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(
                        text = keyword,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun UniverseSection(onCta: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF060B1F), Color(0xFF15224D), Color(0xFF060B1F)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.home_universe_verse),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.clickable(onClick = onCta),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_universe_cta),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFBFD3FF),
                )
                Spacer(Modifier.width(4.dp))
                Text(text = "→", color = Color(0xFFBFD3FF), fontWeight = FontWeight.Bold)
            }
        }
    }
}
