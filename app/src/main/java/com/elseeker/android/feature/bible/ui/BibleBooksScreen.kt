package com.elseeker.android.feature.bible.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.feature.bible.data.BookDto
import com.elseeker.android.feature.bible.ui.components.BiblePageTitle
import com.elseeker.android.feature.bible.ui.components.BibleTopBar

/**
 * 성경책 목록 화면: 이름 검색 + 구약/신약 접이식 섹션(웹 book-list 와 동일한 UX).
 * 검색어 입력 중에는 결과가 있는 섹션만 표시하고 항상 펼친 상태로 보여준다.
 *
 * 스크롤 반응형(웹 common-nav.js + book-search.css 파리티):
 * - 아래로 스크롤: 상단바 숨김 + [onChromeVisibleChange]로 하단 탭도 숨김.
 * - 위로 스크롤: 즉시 다시 표시.
 * - 검색 필드는 sticky — 타이틀이 스크롤로 사라진 뒤에도 목록 상단에 고정.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BibleBooksScreen(
    onBookClick: (translationId: Long, bookOrder: Int) -> Unit,
    onChangeTranslation: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    onChromeVisibleChange: (Boolean) -> Unit = {},
    viewModel: BibleBooksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val oldExpanded by viewModel.oldExpanded.collectAsStateWithLifecycle()
    val newExpanded by viewModel.newExpanded.collectAsStateWithLifecycle()

    val translationCode = (state as? UiResource.Success)?.data?.translationType
        ?.takeIf { it.isNotBlank() }
    val pageTitle = (state as? UiResource.Success)?.data?.translationName
        ?.takeIf { it.isNotBlank() } ?: stringResource(R.string.bible_books_title)

    // 스크롤 방향에 따른 상단바/하단 탭 표시 상태(웹 top-nav-hidden/bottom-tab-hidden 파리티).
    val listState = rememberLazyListState()
    var chromeVisible by remember { mutableStateOf(true) }
    val currentOnChromeVisibleChange by rememberUpdatedState(onChromeVisibleChange)
    LaunchedEffect(chromeVisible) { currentOnChromeVisibleChange(chromeVisible) }

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 웹과 동일: 맨 위에서는 숨기지 않고, 위로 스크롤 시 즉시 복원한다.
                if (available.y < -SCROLL_HIDE_THRESHOLD_PX && listState.canScrollBackward) {
                    chromeVisible = false
                } else if (available.y > SCROLL_HIDE_THRESHOLD_PX) {
                    chromeVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        AnimatedVisibility(
            visible = chromeVisible,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            BibleTopBar(
                translationCode = translationCode,
                onChangeTranslation = onChangeTranslation,
                onSearchClick = onSearchClick,
                onProfileClick = onProfileClick,
            )
        }

        ResourceContent(resource = state, onRetry = viewModel::load, modifier = Modifier.fillMaxSize()) { data ->
            // Lazy DSL 스코프는 컴포저블 컨텍스트가 아니므로 stringResource 를 밖에서 해석한다.
            val oldTestamentLabel = stringResource(R.string.bible_books_old_testament)
            val newTestamentLabel = stringResource(R.string.bible_books_new_testament)
            val searchEmptyLabel = stringResource(R.string.bible_books_search_empty)
            val searching = query.isNotBlank()

            val filteredOld = remember(data.oldTestament, query) {
                if (searching) data.oldTestament.filter { it.bookName.contains(query, ignoreCase = true) }
                else data.oldTestament
            }
            val filteredNew = remember(data.newTestament, query) {
                if (searching) data.newTestament.filter { it.bookName.contains(query, ignoreCase = true) }
                else data.newTestament
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                // 타이틀은 일반 콘텐츠라 스크롤과 함께 사라진다(웹 파리티).
                // 검색창 자체 상단 여백(8dp)과 합쳐 타이틀-검색창 간격이 16dp 가 되도록 하단 패딩을 줄인다.
                item(key = "page-title") { BiblePageTitle(pageTitle, bottomPadding = 8.dp) }

                // 검색 필드는 sticky — 상단바가 숨으면 화면 최상단에 붙는다(book-search.css top:52px↔0 파리티).
                stickyHeader(key = "search") {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        BookSearchField(query = query, onQueryChange = viewModel::onQueryChange)
                    }
                }

                if (searching && filteredOld.isEmpty() && filteredNew.isEmpty()) {
                    item(key = "search-empty") {
                        SearchEmptyHint(searchEmptyLabel, Modifier.fillParentMaxHeight(0.5f))
                    }
                } else {
                    // 검색 중에는 결과가 있는 섹션을 항상 펼친 상태로 보여준다(웹 book-list.js 와 동일).
                    if (filteredOld.isNotEmpty()) {
                        item(key = "old-section") {
                            SectionCard(
                                label = oldTestamentLabel,
                                books = filteredOld,
                                expanded = searching || oldExpanded,
                                onToggleExpand = viewModel::toggleOldTestament,
                                translationId = viewModel.translationId,
                                onBookClick = onBookClick,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                            )
                        }
                    }
                    if (filteredNew.isNotEmpty()) {
                        item(key = "new-section") {
                            SectionCard(
                                label = newTestamentLabel,
                                books = filteredNew,
                                expanded = searching || newExpanded,
                                onToggleExpand = viewModel::toggleNewTestament,
                                translationId = viewModel.translationId,
                                onBookClick = onBookClick,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            )
                        }
                    }
                    item(key = "bottom-spacer") { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/** 스크롤 이벤트 1회당 표시 전환 임계값(px). 웹은 누적 10px 기준이지만 이벤트 단위라 소폭으로 잡는다. */
private const val SCROLL_HIDE_THRESHOLD_PX = 3f

@Composable
private fun BookSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        // 상단 8dp: sticky 로 화면 최상단에 붙었을 때도 여백이 유지된다(배경 Surface 가 함께 덮음).
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text(stringResource(R.string.bible_books_search_placeholder)) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }
        },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SearchEmptyHint(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
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

/** 구약/신약 섹션 카드: 헤더(접기/펼치기) + 2열 책 버튼 그리드(비지연 — 최대 39권이라 충분히 가볍다). */
@Composable
private fun SectionCard(
    label: String,
    books: List<BookDto>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    translationId: Long,
    onBookClick: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        // 웹 파리티: 패널 배경은 흰색(surface). 책 버튼색(surfaceContainerHigh)은 유지한다.
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(label = label, count = books.size, expanded = expanded, onClick = onToggleExpand)
            if (expanded) {
                books.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pair.forEach { book ->
                            BookCell(
                                book = book,
                                modifier = Modifier.weight(1f),
                                onClick = { onBookClick(translationId, book.bookOrder) },
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int, expanded: Boolean, onClick: () -> Unit) {
    val chevronRotation by animateFloatAsState(targetValue = if (expanded) 0f else -90f, label = "chevronRotation")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
        CountBadge(count = count)
    }
}

@Composable
private fun CountBadge(count: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = stringResource(R.string.bible_books_count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun BookCell(book: BookDto, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = book.bookName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 4.dp),
        )
    }
}
