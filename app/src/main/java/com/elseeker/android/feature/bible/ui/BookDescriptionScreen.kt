package com.elseeker.android.feature.bible.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.feature.bible.data.BookDescriptionDto
import com.elseeker.android.feature.bible.ui.components.BibleBottomBar
import com.elseeker.android.feature.bible.ui.components.BiblePageTitle
import com.elseeker.android.feature.bible.ui.components.BibleTopBar

/**
 * 책 개요 화면 — 웹 book-description.html 파리티(docs/view/book-overview.jpg).
 * 요약(구분선) → 저자 → 년도 → 시대 → 배경 → 내용 순의 라벨+본문 섹션.
 * 장 목록 화면에서 📘 요약 행을 탭하면 진입하며, 하단 내비로 이전/다음 책 개요를 오간다.
 */
@Composable
fun BookDescriptionScreen(
    onBack: () -> Unit,
    onOpenChapterList: (translationId: Long, bookOrder: Int) -> Unit,
    onSwitchBook: (newBookOrder: Int) -> Unit,
    onChangeTranslation: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    onChromeVisibleChange: (Boolean) -> Unit = {},
    viewModel: BookDescriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val translationCode by viewModel.translationCode.collectAsStateWithLifecycle()
    val detail = (state as? UiResource.Success)?.data
    val pageTitle = detail?.bookName ?: stringResource(R.string.bible_book_overview_title_fallback)

    // 스크롤 반응형(다른 성경 화면과 동일): 아래로 스크롤 → 상단바·하단 탭 숨김, 위로 → 복원.
    val listState = rememberLazyListState()
    var chromeVisible by remember { mutableStateOf(true) }
    val currentOnChromeVisibleChange by rememberUpdatedState(onChromeVisibleChange)
    LaunchedEffect(chromeVisible) { currentOnChromeVisibleChange(chromeVisible) }
    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -3f && listState.canScrollBackward) {
                    chromeVisible = false
                } else if (available.y > 3f) {
                    chromeVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val order = viewModel.bookOrder.takeIf { detail != null }
            BibleBottomBar(
                centerLabel = pageTitle,
                onPrev = { order?.let { onSwitchBook(it - 1) } },
                onCenter = { order?.let { onOpenChapterList(viewModel.translationId, it) } },
                onNext = { order?.let { onSwitchBook(it + 1) } },
                prevEnabled = order != null && order > 1,
                nextEnabled = order != null && order < 66,
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            AnimatedVisibility(
                visible = chromeVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                BibleTopBar(
                    onBack = onBack,
                    translationCode = translationCode,
                    onChangeTranslation = onChangeTranslation,
                    onSearchClick = onSearchClick,
                    onProfileClick = onProfileClick,
                )
            }
            ResourceContent(
                resource = state,
                onRetry = viewModel::load,
                modifier = Modifier.fillMaxSize(),
            ) { data ->
                val d = data.description
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                ) {
                    item(key = "page-title") { BiblePageTitle(pageTitle, bottomPadding = 8.dp) }
                    item(key = "sections") { DescriptionSections(d) }
                }
            }
        }
    }
}

/** 요약(구분선 뒤 저자/년도/시대/배경/내용) 섹션 묶음 — 빈 필드는 건너뛴다. */
@Composable
private fun DescriptionSections(d: BookDescriptionDto) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DescriptionSection(stringResource(R.string.bible_book_desc_summary), d.summary)
        if (d.summary.isNotBlank()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        DescriptionSection(stringResource(R.string.bible_book_desc_author), d.author)
        DescriptionSection(stringResource(R.string.bible_book_desc_written_year), d.writtenYear)
        DescriptionSection(stringResource(R.string.bible_book_desc_period), d.historicalPeriod)
        DescriptionSection(stringResource(R.string.bible_book_desc_background), d.background)
        DescriptionSection(stringResource(R.string.bible_book_desc_content), d.content)
    }
}

/** 굵은 라벨 + 본문 한 섹션(웹 `<strong>라벨</strong><br><span>본문</span>` 대응). 본문 공백이면 미표시. */
@Composable
private fun DescriptionSection(label: String, text: String) {
    if (text.isBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // 배경/내용은 여러 문단(빈 줄 구분)을 유지하도록 원문 개행을 그대로 렌더한다.
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.5,
        )
    }
    Spacer(Modifier.height(0.dp))
}
