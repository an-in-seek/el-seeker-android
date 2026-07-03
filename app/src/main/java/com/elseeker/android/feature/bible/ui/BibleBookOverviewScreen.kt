package com.elseeker.android.feature.bible.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.BuildConfig
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.openExternalUrl
import com.elseeker.android.feature.bible.data.BookDetailDto
import com.elseeker.android.feature.bible.ui.components.BibleBottomBar
import com.elseeker.android.feature.bible.ui.components.BiblePageTitle
import com.elseeker.android.feature.bible.ui.components.BibleTopBar

/**
 * 책 개요 화면: 책 설명 요약 행(탭 → 전체 개요 다이얼로그) + 액션 버튼 4개(개요/듣기/퀴즈/메모)
 * + 장 번호 그리드 + 하단 책 전환 내비게이션. 웹 chapter-list 페이지와 동일한 구성(docs/view/chapter-list.jpg).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleBookOverviewScreen(
    onBack: () -> Unit,
    onChapterClick: (chapterNumber: Int) -> Unit,
    onSelectBook: () -> Unit,
    onSwitchBook: (newBookOrder: Int) -> Unit,
    onOpenContent: (contentKey: String) -> Unit,
    onChangeTranslation: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    onChromeVisibleChange: (Boolean) -> Unit = {},
    viewModel: BibleBookOverviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bookMemo by viewModel.bookMemo.collectAsStateWithLifecycle()
    val translationCode by viewModel.translationCode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 리더에서 돌아오면 읽음 표시만 무음 갱신한다(최초 진입은 내부에서 전체 로드).
    // 전체 리로드로 스피너가 깜빡이고 스크롤 위치가 초기화되던 버그 수정.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshOnResume() }

    var showDescriptionDialog by remember { mutableStateOf(false) }
    var showMemoDialog by remember { mutableStateOf(false) }

    val loginRequiredMsg = stringResource(R.string.bible_reader_login_required)
    val memoSavedMsg = stringResource(R.string.bible_memo_saved)
    val memoDeletedMsg = stringResource(R.string.bible_memo_deleted)
    LaunchedEffect(viewModel) {
        viewModel.memoEvents.collect { event ->
            val message = when (event) {
                is BookMemoEvent.Saved -> memoSavedMsg
                is BookMemoEvent.Deleted -> memoDeletedMsg
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            showMemoDialog = false
        }
    }

    val overview = (state as? UiResource.Success)?.data
    val chapterReadDesc = stringResource(R.string.bible_chapter_read_desc)
    val pageTitle = overview?.detail?.bookName
        ?: stringResource(R.string.bible_book_overview_title_fallback)

    // 스크롤 반응형(웹 파리티): 아래로 스크롤 → 상단바 숨김 + 하단 탭 숨김(onChromeVisibleChange),
    // 위로 스크롤 → 복원. 가운데 하단 내비(⬅|📖|➡)는 웹 section-nav 처럼 항상 유지한다.
    val gridState = rememberLazyGridState()
    var chromeVisible by remember { mutableStateOf(true) }
    val currentOnChromeVisibleChange by rememberUpdatedState(onChromeVisibleChange)
    LaunchedEffect(chromeVisible) { currentOnChromeVisibleChange(chromeVisible) }

    val nestedScrollConnection = remember(gridState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -SCROLL_HIDE_THRESHOLD_PX && gridState.canScrollBackward) {
                    chromeVisible = false
                } else if (available.y > SCROLL_HIDE_THRESHOLD_PX) {
                    chromeVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val bookOrder = viewModel.bookOrder.takeIf { overview != null }
            BibleBottomBar(
                centerLabel = overview?.detail?.bookName ?: stringResource(R.string.bible_book_overview_title_fallback),
                onPrev = { bookOrder?.let { onSwitchBook(it - 1) } },
                onCenter = onSelectBook,
                onNext = { bookOrder?.let { onSwitchBook(it + 1) } },
                prevEnabled = bookOrder != null && bookOrder > 1,
                nextEnabled = bookOrder != null && bookOrder < 66,
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            // 상단바를 topBar 슬롯(콘텐츠 위 오버레이) 대신 콘텐츠와 같은 Column 에 두어
            // 타이틀이 바 아래로 밀려 가려지는 일이 구조적으로 불가능하게 한다(책 목록과 동일 구조).
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
                LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 타이틀·설명·액션바도 웹처럼 콘텐츠와 함께 스크롤된다(full-span 헤더 아이템).
                item(key = "page-title", span = { GridItemSpan(maxLineSpan) }) {
                    // 아이템 간격(8dp)이 아래에 붙으므로 하단 패딩을 줄여 상하 균형(16dp)을 맞춘다.
                    BiblePageTitle(pageTitle, bottomPadding = 8.dp)
                }
                // 요약이 비어 있으면(설명 미등록 책) 빈 보더 행을 그리지 않는다.
                if (data.descriptionSummary.isNotBlank()) {
                    item(key = "description", span = { GridItemSpan(maxLineSpan) }) {
                        BookDescriptionRow(
                            summary = data.descriptionSummary,
                            onClick = { showDescriptionDialog = true },
                        )
                    }
                }
                item(key = "actions", span = { GridItemSpan(maxLineSpan) }) {
                    BookActionButtons(
                        hasMemo = bookMemo != null,
                        onOverviewClick = { onOpenContent("overview-video") },
                        onListenClick = { onOpenContent("public-reading") },
                        onQuizClick = {
                            openExternalUrl(
                                context,
                                BuildConfig.BASE_URL.trimEnd('/') + "/web/game/bible-ox-quiz/map",
                            )
                        },
                        onMemoClick = {
                            if (viewModel.hasAuthSession) {
                                showMemoDialog = true
                            } else {
                                Toast.makeText(context, loginRequiredMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }
                items(data.chapters, key = { it }) { chapter ->
                    ChapterCell(
                        chapter = chapter,
                        isRead = chapter in data.readChapters,
                        readDesc = chapterReadDesc,
                        onClick = { onChapterClick(chapter) },
                    )
                }
                }
            }
        }
    }

    if (showDescriptionDialog && overview != null) {
        BookDescriptionDialog(
            detail = overview.detail,
            onDismiss = { showDescriptionDialog = false },
        )
    }

    if (showMemoDialog) {
        BookMemoDialog(
            initialContent = bookMemo?.content.orEmpty(),
            hasExisting = bookMemo != null,
            onSave = viewModel::saveBookMemo,
            onDelete = viewModel::deleteBookMemo,
            onDismiss = { showMemoDialog = false },
        )
    }
}

/** 스크롤 이벤트 1회당 상단바/하단 탭 표시 전환 임계값(px) — 책 목록 화면과 동일 기준. */
private const val SCROLL_HIDE_THRESHOLD_PX = 3f

/** 책 개요 요약 행 — 탭하면 전체 개요 다이얼로그를 연다. */
@Composable
private fun BookDescriptionRow(summary: String, onClick: () -> Unit) {
    val moreDesc = stringResource(R.string.bible_book_description_more_desc)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(onClickLabel = moreDesc, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "📘", modifier = Modifier.padding(end = 8.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "➡")
        }
    }
}

/** 개요/듣기/퀴즈/메모 액션 버튼 4개 — 하나의 보더 컨테이너를 세로 구분선으로 4등분. */
@Composable
private fun BookActionButtons(
    hasMemo: Boolean,
    onOverviewClick: () -> Unit,
    onListenClick: () -> Unit,
    onQuizClick: () -> Unit,
    onMemoClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookActionCell(
            icon = "▶️",
            label = stringResource(R.string.bible_action_overview),
            modifier = Modifier.weight(1f),
            onClick = onOverviewClick,
        )
        VerticalDivider(modifier = Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
        BookActionCell(
            icon = "🎧",
            label = stringResource(R.string.bible_action_listen),
            modifier = Modifier.weight(1f),
            onClick = onListenClick,
        )
        VerticalDivider(modifier = Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
        BookActionCell(
            icon = "🎮",
            label = stringResource(R.string.bible_action_quiz),
            modifier = Modifier.weight(1f),
            onClick = onQuizClick,
        )
        VerticalDivider(modifier = Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)
        BookActionCell(
            icon = "📝",
            label = stringResource(R.string.bible_action_memo),
            modifier = Modifier.weight(1f),
            highlighted = hasMemo,
            onClick = onMemoClick,
        )
    }
}

@Composable
private fun BookActionCell(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = icon, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 책 전체 개요(요약/저자/연대/배경) 다이얼로그 — 웹의 book-description 페이지를 시트로 대체. */
@Composable
private fun BookDescriptionDialog(detail: BookDetailDto, onDismiss: () -> Unit) {
    val d = detail.description
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(detail.bookName) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (d.summary.isNotBlank()) {
                    Text(
                        text = d.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                val meta = buildList {
                    if (d.author.isNotBlank()) add(stringResource(R.string.bible_book_overview_author, d.author))
                    if (d.writtenYear.isNotBlank()) add(d.writtenYear)
                    if (d.historicalPeriod.isNotBlank()) add(d.historicalPeriod)
                }
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = meta.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (d.background.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = d.background,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            // 정보성 다이얼로그라 '취소'가 아닌 '닫기'로 표기한다.
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        },
    )
}

/** 책 메모 다이얼로그 — 저장(신규/수정 공통 PUT)/삭제(기존 메모 있을 때만)/취소. */
@Composable
private fun BookMemoDialog(
    initialContent: String,
    hasExisting: Boolean,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var content by remember { mutableStateOf(initialContent) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bible_book_memo_title)) },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text(stringResource(R.string.bible_book_memo_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                minLines = 4,
            )
        },
        confirmButton = {
            Row {
                if (hasExisting) {
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.bible_memo_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                TextButton(onClick = { onSave(content) }) {
                    Text(stringResource(R.string.bible_memo_save))
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

/** 장 번호 셀 — 흰 배경 + 보더, 읽은 장은 우상단에 초록 체크 표시(배경은 미읽음과 동일). */
@Composable
private fun ChapterCell(chapter: Int, isRead: Boolean, readDesc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            // 이미지 파리티: 정사각형이 아니라 가로로 넓은 셀(≈5:3).
            .aspectRatio(1.6f)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = chapter.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (isRead) {
            Text(
                text = "✓",
                color = READ_CHECK_COLOR,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .semantics { contentDescription = readDesc },
            )
        }
    }
}

private val READ_CHECK_COLOR = Color(0xFF16A34A)
