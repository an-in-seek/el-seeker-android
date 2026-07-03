package com.elseeker.android.feature.home.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.elseeker.android.feature.bible.ui.components.BiblePageTitle
import com.elseeker.android.feature.bible.ui.components.BibleTopBar
import kotlinx.coroutines.delay

/**
 * 홈 탭 — 웹 `templates/index.html` 과 동일한 구성(docs/view/home.jpg 파리티):
 * 상단바(프로필) → "ElSeeker" 타이틀 → 히어로 캐러셀(풀블리드, 흰색 CTA) → 통합 검색바
 * → 메뉴 카드(성경/학습/게임/커뮤니티) → 인기 검색어 2카드(순위 배지 + 더보기)
 * → 창조 섹션(풀블리드) → 푸터(로고/소셜/링크/사업자 정보).
 * 게임·커뮤니티는 v1 네이티브 범위 밖이라 웹과 동일 화면을 Custom Tabs 로 위임하고,
 * 3D 우주 캔버스는 그라데이션 섹션으로 대체한다(PRD §4-A.4-1 경량 비주얼).
 */
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val bibleRanking by viewModel.bibleRanking.collectAsStateWithLifecycle()
    val dictionaryRanking by viewModel.dictionaryRanking.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val baseUrl = BuildConfig.BASE_URL.trimEnd('/')

    // 인기 검색어 '더보기' 다이얼로그 대상(웹 popular-search-dialog 파리티).
    var moreTarget by remember { mutableStateOf<PopularMoreTarget?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        BibleTopBar(onProfileClick = onProfileClick)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 0) 페이지 타이틀 — 웹처럼 콘텐츠와 함께 스크롤.
            item { BiblePageTitle(stringResource(R.string.home_title)) }

            // 1) 히어로 캐러셀 (home-hero) — 웹처럼 좌우 여백 없는 풀블리드.
            item { HeroCarousel(baseUrl = baseUrl, onCta = { onNavigate(Routes.BIBLE) }) }

            // 2) 통합 검색바 (home-unified-search) — v1 은 성경 절 검색으로 위임.
            item {
                UnifiedSearchBar(
                    onClick = { onNavigate(Routes.bibleSearch()) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // 3) 메뉴 카드 (home-menu-grid) — 웹과 동일한 보더 카드.
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
                        onMoreClick = { moreTarget = PopularMoreTarget.BIBLE },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            if (dictionaryRanking.isNotEmpty()) {
                item {
                    PopularSearchCard(
                        title = stringResource(R.string.home_popular_dictionary_title),
                        items = dictionaryRanking.map { it.rank to it.keyword },
                        onKeywordClick = { keyword -> onNavigate(Routes.studyDictionary(keyword)) },
                        onMoreClick = { moreTarget = PopularMoreTarget.DICTIONARY },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            // 5) 창조 섹션 (universe-section) — 3D 캔버스 대신 그라데이션, 풀블리드.
            item { UniverseSection(onCta = { onNavigate(Routes.studyContent("creation")) }) }

            // 6) 푸터 (fragments/footer.html 파리티).
            item { HomeFooter(baseUrl = baseUrl) }
        }
    }

    // 인기 검색어 전체 목록 다이얼로그.
    val target = moreTarget
    if (target != null) {
        val (dialogTitle, dialogItems) = when (target) {
            PopularMoreTarget.BIBLE ->
                stringResource(R.string.home_popular_bible_title) to bibleRanking.map { it.rank to it.keyword }
            PopularMoreTarget.DICTIONARY ->
                stringResource(R.string.home_popular_dictionary_title) to dictionaryRanking.map { it.rank to it.keyword }
        }
        PopularMoreDialog(
            title = dialogTitle,
            items = dialogItems,
            onKeywordClick = { keyword ->
                moreTarget = null
                when (target) {
                    PopularMoreTarget.BIBLE -> onNavigate(Routes.bibleSearch(keyword))
                    PopularMoreTarget.DICTIONARY -> onNavigate(Routes.studyDictionary(keyword))
                }
            },
            onDismiss = { moreTarget = null },
        )
    }
}

private enum class PopularMoreTarget { BIBLE, DICTIONARY }

private val HERO_IMAGES = listOf("/images/thebible1.png", "/images/thebible2.png")

/** 카드에 노출할 인기 검색어 수(웹과 동일 상위 5개 — 나머지는 '더보기'). */
private const val POPULAR_VISIBLE_COUNT = 5

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
            .height(220.dp),
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
                .fillMaxWidth()
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
            // 웹 home-hero-cta 와 동일: 흰색 필 형태의 풀폭 버튼.
            Button(
                onClick = onCta,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF111827),
                ),
            ) {
                Text(stringResource(R.string.home_hero_cta), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Text("›", fontWeight = FontWeight.Bold)
            }
        }
        // 인디케이터 점(home-hero-indicators) — 웹처럼 우하단.
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 10.dp),
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
private fun UnifiedSearchBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
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
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.home_popular_more),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onMoreClick)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            items.take(POPULAR_VISIBLE_COUNT).forEach { (rank, keyword) ->
                PopularKeywordRow(rank = rank, keyword = keyword, onClick = { onKeywordClick(keyword) })
            }
        }
    }
}

@Composable
private fun PopularKeywordRow(rank: Int, keyword: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(rank = rank)
        Spacer(Modifier.width(12.dp))
        Text(
            text = keyword,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 순위 배지 — 웹 popular-search 와 동일하게 1위 금색/2위 은색/3위 동색, 그 외 옅은 회색 원. */
@Composable
private fun RankBadge(rank: Int) {
    val background = when (rank) {
        1 -> Color(0xFFFDE68A)
        2 -> Color(0xFFE5E7EB)
        3 -> Color(0xFFFED7AA)
        else -> Color(0xFFF3F4F6)
    }
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
        )
    }
}

/** 인기 검색어 전체 목록(웹 popular-search-dialog 파리티). */
@Composable
private fun PopularMoreDialog(
    title: String,
    items: List<Pair<Int, String>>,
    onKeywordClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                items.forEach { (rank, keyword) ->
                    PopularKeywordRow(rank = rank, keyword = keyword, onClick = { onKeywordClick(keyword) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        },
    )
}

@Composable
private fun UniverseSection(onCta: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
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
            Spacer(Modifier.height(16.dp))
            // 웹과 동일: 보더 필 형태의 CTA 버튼.
            OutlinedButton(
                onClick = onCta,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0x80BFD3FF)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDBE7FF)),
            ) {
                Text(
                    text = stringResource(R.string.home_universe_cta),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                Text(text = "→", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 푸터 — 웹 fragments/footer.html 파리티(로고/소셜/링크/사업자 정보). */
@Composable
private fun HomeFooter(baseUrl: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.elseeker_login),
            contentDescription = stringResource(R.string.home_title),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(44.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = { sendSupportEmail(context) }) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = stringResource(R.string.home_footer_email_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { openExternalUrl(context, YOUTUBE_CHANNEL_URL) }) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(R.string.home_footer_youtube_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FooterLink(stringResource(R.string.home_footer_about)) {
                openExternalUrl(context, "$baseUrl/web/about")
            }
            FooterLink(stringResource(R.string.home_footer_contact)) {
                openExternalUrl(context, "$baseUrl/web/support/contact")
            }
            FooterLink(stringResource(R.string.home_footer_terms)) {
                openExternalUrl(context, "$baseUrl/web/legal/terms")
            }
            FooterLink(stringResource(R.string.home_footer_privacy)) {
                openExternalUrl(context, "$baseUrl/web/legal/privacy")
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.home_footer_company),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.home_footer_support),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.home_footer_copyright),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FooterLink(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

private const val YOUTUBE_CHANNEL_URL = "https://www.youtube.com/@seek539"

/** 고객센터 메일 작성(mailto:) — 메일 앱이 없는 기기에서는 조용히 무시한다. */
private fun sendSupportEmail(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hello@elseeker.com")))
    }
}
