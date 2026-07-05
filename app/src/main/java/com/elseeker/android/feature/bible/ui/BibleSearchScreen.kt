package com.elseeker.android.feature.bible.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.feature.bible.data.BibleSearchSliceDto.BibleSearchItemDto
import com.elseeker.android.feature.bible.data.KeywordRankingDto.RankingItemDto
import com.elseeker.android.feature.bible.data.TranslationDto

/**
 * 성경 절 검색 화면(웹 verse-search 파리티, docs/view/bible-verse-search1·2.png).
 * 상단 고정: 검색 필드 + 번역본 선택 + 결과 카운트. 스크롤: 인기 검색어 카드 → 결과 카드.
 * 결과 카드는 검색어를 노란색으로 강조 표시하고, 탭하면 해당 장 본문으로 이동한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleSearchScreen(
    onBack: () -> Unit,
    onResultClick: (translationId: Long, bookOrder: Int, chapterNumber: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BibleSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val ranking by viewModel.ranking.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val hasSearched by viewModel.hasSearched.collectAsStateWithLifecycle()
    val translations by viewModel.translations.collectAsStateWithLifecycle()
    val selectedTranslation by viewModel.selectedTranslation.collectAsStateWithLifecycle()
    val resultCount by viewModel.resultCount.collectAsStateWithLifecycle()
    val searchedKeyword by viewModel.searchedKeyword.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                expandedHeight = 56.dp,
                title = {
                    Text(
                        text = stringResource(R.string.bible_search_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            // --- 상단 고정 영역: 검색 필드 + 번역본 선택 + 결과 카운트 ---
            SearchField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = viewModel::search,
            )
            if (translations.isNotEmpty()) {
                TranslationSelector(
                    selected = selectedTranslation,
                    translations = translations,
                    onSelect = viewModel::onTranslationSelected,
                )
            }
            val count = resultCount
            if (hasSearched && count != null) {
                ResultCountLine(keyword = searchedKeyword, count = count)
            }

            // --- 스크롤 영역: 인기 검색어 카드 → 결과 ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (ranking.isNotEmpty()) {
                    item(key = "popular") {
                        PopularSearchCard(ranking = ranking, onKeywordClick = viewModel::onKeywordClick)
                    }
                }

                when (val res = results) {
                    is UiResource.Loading -> item(key = "loading") { LoadingRow() }
                    is UiResource.Error -> item(key = "error") {
                        ErrorRow(message = res.message, isNetwork = res.isNetwork, onRetry = viewModel::search)
                    }
                    is UiResource.Success -> {
                        val list = res.data
                        when {
                            !hasSearched -> item(key = "hint") {
                                HintRow(stringResource(R.string.bible_search_hint_initial))
                            }
                            list.isEmpty() -> item(key = "empty") {
                                HintRow(stringResource(R.string.bible_search_hint_no_results))
                            }
                            else -> items(list, key = { it.verseId }) { item ->
                                SearchResultCard(item = item, keyword = searchedKeyword) {
                                    selectedTranslation?.let { tr ->
                                        onResultClick(tr.translationId, item.bookOrder, item.chapterNumber)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 둥근 검색 입력 필드 + 우측 파란 테두리 검색 버튼(웹 verse-search 헤더 파리티). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.bible_search_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )
        Surface(
            onClick = onSearch,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.common_search),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** 번역본 선택 드롭다운(웹의 "전체" 셀렉트 위치) — 최근 선택 번역본을 표시/변경한다. */
@Composable
private fun TranslationSelector(
    selected: TranslationDto?,
    translations: List<TranslationDto>,
    onSelect: (TranslationDto) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selected?.translationName.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.bible_search_translation_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            translations.forEach { translation ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = translation.translationName,
                            fontWeight = if (translation.translationId == selected?.translationId) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(translation)
                    },
                )
            }
        }
    }
}

/** ""{검색어}"에 대한 결과 {건수}건" 헤더. */
@Composable
private fun ResultCountLine(keyword: String, count: Long) {
    Text(
        text = stringResource(R.string.bible_search_result_count, keyword, "%,d".format(count)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

/** 인기 검색어 카드 — 상위 순위 컬러 배지 + 키워드. "더보기"로 전체/일부 토글. */
@Composable
private fun PopularSearchCard(
    ranking: List<RankingItemDto>,
    onKeywordClick: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) ranking else ranking.take(DEFAULT_RANKING_COUNT)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.bible_search_popular),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (ranking.size > DEFAULT_RANKING_COUNT) {
                    Text(
                        text = stringResource(
                            if (expanded) R.string.bible_search_less else R.string.bible_search_more,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(4.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            visible.forEach { item ->
                RankingRow(item = item, onClick = { onKeywordClick(item.keyword) })
            }
        }
    }
}

@Composable
private fun RankingRow(item: RankingItemDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(rank = item.rank)
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.keyword,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 순위 배지 — 1~3위는 금/은/동 계열 파스텔, 그 외는 중립색. */
@Composable
private fun RankBadge(rank: Int) {
    val (bg, fg) = rankBadgeColors(rank)
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

/** 검색 결과 카드 — 파란 참조 배지 + 검색어를 노란색으로 강조한 본문. */
@Composable
private fun SearchResultCard(item: BibleSearchItemDto, keyword: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            ReferenceBadge("${item.bookName} ${item.chapterNumber}:${item.verseNumber}")
            Spacer(Modifier.height(8.dp))
            Text(
                text = highlightKeyword(item.text, keyword),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** "역대상 24:11" 형태의 파란 참조 배지. */
@Composable
private fun ReferenceBadge(reference: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Text(
            text = reference,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorRow(message: String, isNetwork: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isNetwork) "📡" else "⚠️",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun HintRow(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 본문에서 검색어(대소문자 무시) 일치 구간을 노란 배경 + 볼드로 강조한 문자열을 만든다. */
private fun highlightKeyword(text: String, keyword: String): AnnotatedString = buildAnnotatedString {
    val key = keyword.trim()
    if (key.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    val lowerText = text.lowercase()
    val lowerKey = key.lowercase()
    var start = 0
    while (true) {
        val idx = lowerText.indexOf(lowerKey, start)
        if (idx < 0) {
            append(text.substring(start))
            break
        }
        append(text.substring(start, idx))
        withStyle(SpanStyle(background = HighlightYellow, fontWeight = FontWeight.Bold)) {
            append(text.substring(idx, idx + lowerKey.length))
        }
        start = idx + lowerKey.length
    }
}

/** 인기 검색어 기본 표시 개수(초과분은 "더보기"로 펼침). */
private const val DEFAULT_RANKING_COUNT = 3

/** 본문 검색어 강조 배경(노란 형광펜, 라이트/다크 공통으로 읽히는 파스텔). */
private val HighlightYellow = Color(0xFFFDE047)

/**
 * 순위 배지 (배경, 글자) 색 — 1·2·3위는 금/은/동 파스텔, 그 외는 중립.
 * 작은 원 위 파스텔 배경 + 진한 글자라 라이트/다크 양쪽에서 읽힌다.
 */
private fun rankBadgeColors(rank: Int): Pair<Color, Color> = when (rank) {
    1 -> Color(0xFFFEF08A) to Color(0xFFB45309)
    2 -> Color(0xFFE5E7EB) to Color(0xFF4B5563)
    3 -> Color(0xFFFED7AA) to Color(0xFFC2410C)
    else -> Color(0xFFF1F5F9) to Color(0xFF64748B)
}
