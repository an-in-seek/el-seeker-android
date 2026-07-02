package com.elseeker.android.feature.bible.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
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

/**
 * 절 본문 뷰어(웹 verse-list 화면과 동일 UX 패턴).
 * - 절 탭 = 다중 선택 토글(Set 기반). 선택된 절이 있으면 우하단 FAB 로 공유/복사/메모/형광펜 실행.
 * - 절 목록 아래 [장 메모] [읽음] 버튼, 하단 고정 내비에 이전/장 선택/다음.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    onBack: () -> Unit,
    onOpenChapterList: (translationId: Long, bookOrder: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BibleReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val memos by viewModel.memos.collectAsStateWithLifecycle()
    val chapterMemo by viewModel.chapterMemo.collectAsStateWithLifecycle()
    val isRead by viewModel.isRead.collectAsStateWithLifecycle()
    val success = state as? UiResource.Success<VersesDto>
    val title = success?.data?.let {
        stringResource(R.string.bible_reader_chapter_title, it.book.bookName, it.book.chapter.chapterNumber)
    } ?: stringResource(R.string.bible_reader_title_fallback)

    // LazyColumn items{} 본문에서 stringResource 를 호출하면 빌드 오류가 났던 이력이 있어
    // 필요한 문자열은 모두 컴포저블 상단에서 미리 해석해 둔다.
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val loginRequiredMessage = stringResource(R.string.bible_reader_login_required)
    val copyDoneMessage = stringResource(R.string.bible_copy_done)
    val markedReadMessage = stringResource(R.string.bible_marked_read)
    val prevLabel = stringResource(R.string.bible_prev_chapter)
    val nextLabel = stringResource(R.string.bible_next_chapter)
    val chapterSelectDesc = stringResource(R.string.bible_chapter_select_desc)
    val readLabel = stringResource(R.string.bible_mark_read)
    val chapterMemoBtnLabel = stringResource(R.string.bible_chapter_memo_btn)
    val chapterMemoTitle = stringResource(R.string.bible_chapter_memo_title)
    val chapterMemoPlaceholder = stringResource(R.string.bible_chapter_memo_placeholder)
    val memoSaveLabel = stringResource(R.string.bible_memo_save)
    val memoDeleteLabel = stringResource(R.string.bible_memo_delete)
    val cancelLabel = stringResource(R.string.common_cancel)
    val verseMemoPlaceholder = stringResource(R.string.bible_reader_memo_placeholder)
    val verseMemoSaveLabel = stringResource(R.string.bible_reader_memo_save)
    val verseMemoDeleteLabel = stringResource(R.string.bible_reader_memo_delete)
    val fabShareLabel = stringResource(R.string.bible_fab_share)
    val fabCopyLabel = stringResource(R.string.bible_fab_copy)
    val fabMemoLabel = stringResource(R.string.bible_fab_memo)
    val fabOpenDesc = stringResource(R.string.bible_fab_open_desc)
    val highlightClearDesc = stringResource(R.string.bible_highlight_clear_desc)

    var showPicker by remember { mutableStateOf(false) }
    var showChapterMemo by remember { mutableStateOf(false) }
    var memoVerseNumber by remember { mutableStateOf<Int?>(null) }
    var selectedVerses by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var fabExpanded by remember { mutableStateOf(false) }

    fun requireAuth(action: () -> Unit) {
        if (viewModel.canAnnotate) action()
        else Toast.makeText(context, loginRequiredMessage, Toast.LENGTH_SHORT).show()
    }

    // 장이 바뀌면 이전 장의 절 선택을 초기화한다(웹의 resetSelectionState 와 동일 목적).
    LaunchedEffect(success?.data?.book?.chapter?.chapterId) {
        selectedVerses = emptySet()
        fabExpanded = false
    }

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
                ReaderBottomBar(
                    data = success.data,
                    prevLabel = prevLabel,
                    nextLabel = nextLabel,
                    chapterLabel = title,
                    chapterSelectDesc = chapterSelectDesc,
                    onPrev = viewModel::goPrev,
                    onNext = viewModel::goNext,
                    onOpenChapterList = { onOpenChapterList(viewModel.translationId, success.data.book.bookOrder) },
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
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(data.book.chapter.verses, key = { it.verseId }) { verse ->
                        val highlightColor = highlights[verse.verseNumber]?.let(::highlightColorOf)
                        val hasMemo = memos.containsKey(verse.verseNumber)
                        val isSelected = verse.verseNumber in selectedVerses
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .then(if (highlightColor != null) Modifier.background(highlightColor) else Modifier)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable {
                                    // 절 탭 = 다중 선택 토글(웹 verse-list.js 의 Set 기반 selection 과 동일).
                                    selectedVerses = if (isSelected) {
                                        selectedVerses - verse.verseNumber
                                    } else {
                                        selectedVerses + verse.verseNumber
                                    }
                                }
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
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { requireAuth { showChapterMemo = true } },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("📝", modifier = Modifier.padding(end = 6.dp))
                                Text(chapterMemoBtnLabel)
                            }
                            if (isRead) {
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("✓", modifier = Modifier.padding(end = 6.dp))
                                    Text(readLabel)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        requireAuth {
                                            viewModel.markRead {
                                                Toast.makeText(context, markedReadMessage, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("✓", modifier = Modifier.padding(end = 6.dp))
                                    Text(readLabel)
                                }
                            }
                        }
                        // 절이 선택되어 FAB 가 뜨면 목록 마지막 항목을 가리지 않도록 여백 확보.
                        Spacer(Modifier.height(72.dp))
                    }
                }

                if (selectedVerses.isNotEmpty()) {
                    VerseSelectionFab(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        expanded = fabExpanded,
                        memoEnabled = selectedVerses.size == 1,
                        shareLabel = fabShareLabel,
                        copyLabel = fabCopyLabel,
                        memoLabel = fabMemoLabel,
                        toggleDesc = fabOpenDesc,
                        highlightClearDesc = highlightClearDesc,
                        onToggle = { fabExpanded = !fabExpanded },
                        onShare = {
                            fabExpanded = false
                            val text = buildSelectionText(data, selectedVerses)
                            if (text.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        },
                        onCopy = {
                            fabExpanded = false
                            val text = buildSelectionText(data, selectedVerses)
                            if (text.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, copyDoneMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onMemo = {
                            fabExpanded = false
                            requireAuth { memoVerseNumber = selectedVerses.first() }
                        },
                        onHighlight = { colorWire ->
                            fabExpanded = false
                            requireAuth {
                                if (colorWire == null) {
                                    selectedVerses.forEach(viewModel::removeHighlight)
                                } else {
                                    selectedVerses.forEach { viewModel.setHighlight(it, colorWire) }
                                }
                                selectedVerses = emptySet()
                            }
                        },
                    )
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

    if (showChapterMemo) {
        MemoDialog(
            title = chapterMemoTitle,
            placeholder = chapterMemoPlaceholder,
            initialContent = chapterMemo?.content ?: "",
            hasExisting = chapterMemo != null,
            saveLabel = memoSaveLabel,
            deleteLabel = memoDeleteLabel,
            cancelLabel = cancelLabel,
            onDismiss = { showChapterMemo = false },
            onSave = { text -> viewModel.saveChapterMemo(text) { showChapterMemo = false } },
            onDelete = { viewModel.deleteChapterMemo { showChapterMemo = false } },
        )
    }

    val memoVerse = memoVerseNumber
    if (memoVerse != null) {
        MemoDialog(
            title = stringResource(R.string.bible_reader_verse_number, memoVerse),
            placeholder = verseMemoPlaceholder,
            initialContent = memos[memoVerse] ?: "",
            hasExisting = memos.containsKey(memoVerse),
            saveLabel = verseMemoSaveLabel,
            deleteLabel = verseMemoDeleteLabel,
            cancelLabel = cancelLabel,
            onDismiss = { memoVerseNumber = null },
            onSave = { text ->
                viewModel.saveMemo(memoVerse, text)
                memoVerseNumber = null
            },
            onDelete = {
                viewModel.saveMemo(memoVerse, "")
                memoVerseNumber = null
            },
        )
    }
}

/** 선택된 절들을 "{책이름} {장}:{절} 본문" 줄로 합친다(공유/복사 공용). */
private fun buildSelectionText(data: VersesDto, selected: Set<Int>): String {
    val bookName = data.book.bookName
    val chapterNumber = data.book.chapter.chapterNumber
    return data.book.chapter.verses
        .filter { it.verseNumber in selected }
        .sortedBy { it.verseNumber }
        .joinToString(separator = "\n") { verse -> "$bookName $chapterNumber:${verse.verseNumber} ${verse.text}" }
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

/**
 * 선택된 절이 있을 때 우하단에 뜨는 FAB(웹 verse-fab 과 동일 패턴).
 * 펼치면 공유/복사/메모 + 형광펜 색상 줄(+지우기)을 노출한다.
 */
@Composable
private fun VerseSelectionFab(
    expanded: Boolean,
    memoEnabled: Boolean,
    shareLabel: String,
    copyLabel: String,
    memoLabel: String,
    toggleDesc: String,
    highlightClearDesc: String,
    onToggle: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onMemo: () -> Unit,
    onHighlight: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        if (expanded) {
            FabMenuItem(icon = "📤", label = shareLabel, onClick = onShare)
            Spacer(Modifier.height(8.dp))
            FabMenuItem(icon = "📋", label = copyLabel, onClick = onCopy)
            Spacer(Modifier.height(8.dp))
            FabMenuItem(icon = "📝", label = memoLabel, onClick = onMemo, enabled = memoEnabled)
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HIGHLIGHT_SWATCHES.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatch.color)
                                .clickable { onHighlight(swatch.wire) },
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .semantics { contentDescription = highlightClearDesc }
                            .clickable { onHighlight(null) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", fontSize = 12.sp)
                    }
                }
            }
        }
        FloatingActionButton(onClick = onToggle) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = toggleDesc,
            )
        }
    }
}

@Composable
private fun FabMenuItem(icon: String, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = label,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 장 메모/절 메모 다이얼로그 — 저장(공백이면 비활성)/삭제(기존 메모 있을 때)/취소. */
@Composable
private fun MemoDialog(
    title: String,
    placeholder: String,
    initialContent: String,
    hasExisting: Boolean,
    saveLabel: String,
    deleteLabel: String,
    cancelLabel: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var text by remember { mutableStateOf(initialContent) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
            )
        },
        confirmButton = {
            Row {
                if (hasExisting) {
                    TextButton(onClick = onDelete) { Text(deleteLabel) }
                }
                TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
                    Text(saveLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        },
    )
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

/** 하단 고정 내비 — 이전 장 / 장 선택(onOpenChapterList) / 다음 장. */
@Composable
private fun ReaderBottomBar(
    data: VersesDto,
    prevLabel: String,
    nextLabel: String,
    chapterLabel: String,
    chapterSelectDesc: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onOpenChapterList: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onPrev, enabled = data.hasPrev, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = null)
            Text(prevLabel)
        }
        Button(
            onClick = onOpenChapterList,
            modifier = Modifier
                .weight(1.3f)
                .semantics { contentDescription = chapterSelectDesc },
        ) {
            Text("📖", modifier = Modifier.padding(end = 6.dp))
            Text(chapterLabel, maxLines = 1)
        }
        OutlinedButton(onClick = onNext, enabled = data.hasNext, modifier = Modifier.weight(1f)) {
            Text(nextLabel)
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}
