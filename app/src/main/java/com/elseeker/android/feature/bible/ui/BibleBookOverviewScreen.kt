package com.elseeker.android.feature.bible.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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

/**
 * 책 개요 화면: 책 설명 요약 행(탭 → 전체 개요 다이얼로그) + 액션 버튼 4개(개요/듣기/퀴즈/메모)
 * + 장 번호 그리드 + 하단 책 전환 내비게이션. 웹 chapter-list 페이지와 동일한 구성.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleBookOverviewScreen(
    onBack: () -> Unit,
    onChapterClick: (chapterNumber: Int) -> Unit,
    onSelectBook: () -> Unit,
    onSwitchBook: (newBookOrder: Int) -> Unit,
    onOpenContent: (contentKey: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BibleBookOverviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bookMemo by viewModel.bookMemo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 리더에서 돌아오면 읽음 표시를 최신화한다(최초 진입 로드 포함).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bible_chapter_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        bottomBar = {
            BookSwitchNav(
                bookName = overview?.detail?.bookName,
                bookOrder = viewModel.bookOrder.takeIf { overview != null },
                onSelectBook = onSelectBook,
                onSwitchBook = onSwitchBook,
            )
        },
    ) { inner ->
        ResourceContent(
            resource = state,
            onRetry = viewModel::load,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) { data ->
            Column(modifier = Modifier.fillMaxSize()) {
                BookDescriptionRow(
                    summary = data.descriptionSummary,
                    onClick = { showDescriptionDialog = true },
                )
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

                val chapterSelectLabel = stringResource(R.string.bible_chapter_select_label)
                val chapterReadLegend = stringResource(R.string.bible_chapter_read_legend)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(
                                text = chapterSelectLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (data.readChapters.isNotEmpty()) {
                                Text(
                                    text = chapterReadLegend,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    items(data.chapters, key = { it }) { chapter ->
                        ChapterCell(
                            chapter = chapter,
                            isRead = chapter in data.readChapters,
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

/** 책 개요 요약 행 — 탭하면 전체 개요 다이얼로그를 연다. */
@Composable
private fun BookDescriptionRow(summary: String, onClick: () -> Unit) {
    val moreDesc = stringResource(R.string.bible_book_description_more_desc)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .clickable(onClickLabel = moreDesc, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            Text(text = "➡️")
        }
    }
}

/** 개요/듣기/퀴즈/메모 액션 버튼 4개(가로 배치). 메모는 저장된 메모가 있으면 강조. */
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BookActionButton(
            icon = "▶️",
            label = stringResource(R.string.bible_action_overview),
            modifier = Modifier.weight(1f),
            onClick = onOverviewClick,
        )
        BookActionButton(
            icon = "🎧",
            label = stringResource(R.string.bible_action_listen),
            modifier = Modifier.weight(1f),
            onClick = onListenClick,
        )
        BookActionButton(
            icon = "🎮",
            label = stringResource(R.string.bible_action_quiz),
            modifier = Modifier.weight(1f),
            onClick = onQuizClick,
        )
        BookActionButton(
            icon = "📝",
            label = stringResource(R.string.bible_action_memo),
            modifier = Modifier.weight(1f),
            highlighted = hasMemo,
            onClick = onMemoClick,
        )
    }
}

@Composable
private fun BookActionButton(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClickLabel = label, onClick = onClick),
        colors = if (highlighted) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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

/** 하단 고정 책 전환 내비게이션: 이전 책 / 책 선택(장 목록 재진입 X, 책 목록으로 이동) / 다음 책. */
@Composable
private fun BookSwitchNav(
    bookName: String?,
    bookOrder: Int?,
    onSelectBook: () -> Unit,
    onSwitchBook: (Int) -> Unit,
) {
    val prevDesc = stringResource(R.string.bible_prev_book_desc)
    val nextDesc = stringResource(R.string.bible_next_book_desc)
    val selectDesc = stringResource(R.string.bible_book_select_desc)
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedIconButton(
                onClick = { bookOrder?.let { onSwitchBook(it - 1) } },
                enabled = bookOrder != null && bookOrder > 1,
                modifier = Modifier.clearAndSetSemantics { contentDescription = prevDesc },
            ) {
                Text(text = "⬅")
            }
            Button(
                onClick = onSelectBook,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        contentDescription = bookName?.let { "$selectDesc $it" } ?: selectDesc
                    },
                enabled = bookName != null,
            ) {
                Text(text = "📖")
                Spacer(Modifier.width(8.dp))
                Text(text = bookName ?: stringResource(R.string.bible_book_overview_title_fallback))
            }
            OutlinedIconButton(
                onClick = { bookOrder?.let { onSwitchBook(it + 1) } },
                enabled = bookOrder != null && bookOrder < 66,
                modifier = Modifier.clearAndSetSemantics { contentDescription = nextDesc },
            ) {
                Text(text = "➡")
            }
        }
    }
}

@Composable
private fun ChapterCell(chapter: Int, isRead: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = if (isRead) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = chapter.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isRead) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}
