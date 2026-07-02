package com.elseeker.android.feature.bible.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.feature.bible.data.VersesDto

/** 절 본문 뷰어. 상단에 책·장 제목, 하단에 이전/다음 장 이동(navigate API). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BibleReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val memos by viewModel.memos.collectAsStateWithLifecycle()
    val success = state as? UiResource.Success<VersesDto>
    val title = success?.data?.let {
        stringResource(R.string.bible_reader_chapter_title, it.book.bookName, it.book.chapter.chapterNumber)
    } ?: stringResource(R.string.bible_reader_title_fallback)
    var showPicker by remember { mutableStateOf(false) }
    var selectedVerse by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    // 제목 탭 → 장 선택 시트(임의 장으로 점프). 데이터가 있을 때만 활성화.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (success != null) Modifier.clickable { showPicker = true } else Modifier,
                    ) {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        if (success != null) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.bible_chapter_select_label))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        bottomBar = {
            if (success != null) {
                ReaderNavBar(
                    data = success.data,
                    onPrev = viewModel::goPrev,
                    onNext = viewModel::goNext,
                )
            }
        },
    ) { inner ->
        ResourceContent(
            resource = state,
            onRetry = viewModel::retry,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) { data ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(data.book.chapter.verses, key = { it.verseId }) { verse ->
                    val highlightColor = highlights[verse.verseNumber]?.let(::highlightColorOf)
                    val hasMemo = memos.containsKey(verse.verseNumber)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .then(if (highlightColor != null) Modifier.background(highlightColor) else Modifier)
                            .clickable { selectedVerse = verse.verseNumber }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${verse.verseNumber}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 10.dp, top = 3.dp),
                        )
                        Text(
                            text = verse.text,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 26.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (hasMemo) {
                            Text(
                                text = "📝",
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPicker && success != null) {
        ChapterPickerSheet(
            current = success.data.book.chapter.chapterNumber,
            total = success.data.book.totalChapterCount,
            onDismiss = { showPicker = false },
            onSelect = { chapter ->
                showPicker = false
                viewModel.loadChapter(success.data.book.bookOrder, chapter)
            },
        )
    }

    val activeVerse = selectedVerse
    if (activeVerse != null) {
        VerseActionSheet(
            verseNumber = activeVerse,
            currentColor = highlights[activeVerse],
            currentMemo = memos[activeVerse] ?: "",
            onDismiss = { selectedVerse = null },
            onPickColor = { color ->
                viewModel.setHighlight(activeVerse, color)
                selectedVerse = null
            },
            onClearColor = {
                viewModel.removeHighlight(activeVerse)
                selectedVerse = null
            },
            onSaveMemo = { text ->
                viewModel.saveMemo(activeVerse, text)
                selectedVerse = null
            },
        )
    }
}

private data class HighlightSwatch(val wire: String, val color: Color)

private val HIGHLIGHT_SWATCHES = listOf(
    HighlightSwatch("yellow", Color(0xFFFFF176)),
    HighlightSwatch("green", Color(0xFFA5D6A7)),
    HighlightSwatch("blue", Color(0xFF90CAF9)),
    HighlightSwatch("pink", Color(0xFFF48FB1)),
    HighlightSwatch("purple", Color(0xFFCE93D8)),
    HighlightSwatch("orange", Color(0xFFFFCC80)),
)

/** 하이라이트 색상 id → 본문 배경색(반투명). 미지정 값은 노랑으로 폴백. */
private fun highlightColorOf(wire: String): Color =
    (HIGHLIGHT_SWATCHES.firstOrNull { it.wire == wire }?.color ?: Color(0xFFFFF176)).copy(alpha = 0.5f)

/** 절 탭 시 하이라이트/메모 편집 시트. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerseActionSheet(
    verseNumber: Int,
    currentColor: String?,
    currentMemo: String,
    onDismiss: () -> Unit,
    onPickColor: (String) -> Unit,
    onClearColor: () -> Unit,
    onSaveMemo: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var memoText by remember(verseNumber) { mutableStateOf(currentMemo) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.bible_reader_verse_number, verseNumber),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.bible_reader_highlight_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HIGHLIGHT_SWATCHES.forEach { swatch ->
                    val selected = swatch.wire == currentColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(swatch.color)
                            .then(
                                if (selected) Modifier.background(swatch.color) else Modifier,
                            )
                            .clickable { onPickColor(swatch.wire) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Text("✓", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (currentColor != null) {
                TextButton(onClick = onClearColor) { Text(stringResource(R.string.bible_reader_highlight_clear)) }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.bible_reader_memo_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = memoText,
                onValueChange = { memoText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
                placeholder = { Text(stringResource(R.string.bible_reader_memo_placeholder)) },
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onSaveMemo(memoText) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (memoText.isBlank()) stringResource(R.string.bible_reader_memo_delete) else stringResource(R.string.bible_reader_memo_save))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterPickerSheet(
    current: Int,
    total: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.bible_chapter_select_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 12.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 56.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            items((1..total.coerceAtLeast(1)).toList()) { chapter ->
                val selected = chapter == current
                Surface(
                    onClick = { onSelect(chapter) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$chapter",
                            textAlign = TextAlign.Center,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderNavBar(data: VersesDto, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrev, enabled = data.hasPrev) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = null)
            Text(stringResource(R.string.bible_reader_prev_chapter))
        }
        TextButton(onClick = onNext, enabled = data.hasNext) {
            Text(stringResource(R.string.bible_reader_next_chapter))
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}
