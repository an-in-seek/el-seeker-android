package com.elseeker.android.feature.bible.ui

import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.elseeker.android.feature.bible.ui.components.BibleBottomBar
import com.elseeker.android.feature.bible.ui.components.BiblePageTitle
import com.elseeker.android.feature.bible.ui.components.BibleTopBar

/**
 * 절 본문 뷰어(웹 verse-list 화면과 동일 UX 패턴, docs/view/verse-list.jpg).
 * - 절 탭 = 다중 선택 토글(Set 기반). 선택된 절이 있으면 우하단 FAB 로 공유/복사/메모/형광펜 실행.
 * - 타이틀 아래 [장 메모][읽음] 버튼, 하단 고정 내비에 이전/장 선택/다음.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    onBack: () -> Unit,
    onOpenChapterList: (translationId: Long, bookOrder: Int) -> Unit,
    onChangeTranslation: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    onChromeVisibleChange: (Boolean) -> Unit = {},
    viewModel: BibleReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val memos by viewModel.memos.collectAsStateWithLifecycle()
    val chapterMemo by viewModel.chapterMemo.collectAsStateWithLifecycle()
    val isRead by viewModel.isRead.collectAsStateWithLifecycle()
    val translationCode by viewModel.translationCode.collectAsStateWithLifecycle()
    val fontStep by viewModel.fontStep.collectAsStateWithLifecycle()
    val success = state as? UiResource.Success<VersesDto>
    // 이미지 파리티: "창세기 1" — "장" 접미 없이 책이름+장번호만 표기.
    val title = success?.data?.let { "${it.book.bookName} ${it.book.chapter.chapterNumber}" }
        ?: stringResource(R.string.bible_reader_title_fallback)
    val verseFontSizeValue = fontSizeForStep(fontStep)
    val verseFontSize = verseFontSizeValue.sp
    val verseLineHeight = (verseFontSizeValue * 1.6f).sp

    // LazyColumn items{} 본문에서 stringResource 를 호출하면 빌드 오류가 났던 이력이 있어
    // 필요한 문자열은 모두 컴포저블 상단에서 미리 해석해 둔다.
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val loginRequiredMessage = stringResource(R.string.bible_reader_login_required)
    val copyDoneMessage = stringResource(R.string.bible_copy_done)
    val markedReadMessage = stringResource(R.string.bible_marked_read)
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
    val memoHasDesc = stringResource(R.string.bible_verse_has_memo_desc)

    var showChapterMemo by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
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

    // 스크롤 반응형(웹 파리티): 아래로 스크롤 → 상단바 숨김 + 하단 탭 숨김(onChromeVisibleChange),
    // 위로 스크롤 → 복원. 하단 내비(⬅|📖|➡)는 웹 section-nav 처럼 항상 유지한다.
    val listState = rememberLazyListState()
    var chromeVisible by remember { mutableStateOf(true) }
    val currentOnChromeVisibleChange by rememberUpdatedState(onChromeVisibleChange)
    LaunchedEffect(chromeVisible) { currentOnChromeVisibleChange(chromeVisible) }

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -SCROLL_HIDE_THRESHOLD_PX && listState.canScrollBackward) {
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
        topBar = {
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
                    onFontSizeClick = { showFontSizeDialog = true },
                    onProfileClick = onProfileClick,
                )
            }
        },
        bottomBar = {
            if (success != null) {
                BibleBottomBar(
                    centerLabel = title,
                    onPrev = viewModel::goPrev,
                    onCenter = { onOpenChapterList(viewModel.translationId, success.data.book.bookOrder) },
                    onNext = viewModel::goNext,
                    prevEnabled = success.data.hasPrev,
                    nextEnabled = success.data.hasNext,
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
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 4.dp),
                ) {
                    // 타이틀은 웹처럼 콘텐츠와 함께 스크롤된다.
                    // 첫 절 행의 상단 패딩(14dp)이 아래에 붙으므로 하단 패딩을 줄여 상하 균형을 맞춘다.
                    item(key = "page-title") { BiblePageTitle(text = title, bottomPadding = 2.dp) }
                    items(data.book.chapter.verses, key = { it.verseId }) { verse ->
                        val highlightColor = highlights[verse.verseNumber]?.let(::highlightColorOf)
                        val hasMemo = memos.containsKey(verse.verseNumber)
                        val isSelected = verse.verseNumber in selectedVerses
                        Column {
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
                                    .padding(horizontal = 4.dp, vertical = 14.dp),
                            ) {
                                Text(
                                    text = "${verse.verseNumber}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    // 웹 파리티: 번호 셀은 고정폭 + 우측 정렬(td:first-child).
                                    textAlign = TextAlign.End,
                                    modifier = Modifier
                                        .width(36.dp)
                                        .padding(end = 8.dp),
                                )
                                Text(
                                    text = if (hasMemo) "${verse.text} 📝" else verse.text,
                                    fontSize = verseFontSize,
                                    lineHeight = verseLineHeight,
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics {
                                            if (hasMemo) contentDescription = memoHasDesc
                                        },
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    item {
                        // 웹 verse-list 와 동일: 절 목록 아래 [장 메모][읽음] 반반 폭 버튼 행.
                        ChapterActionsRow(
                            isRead = isRead,
                            chapterMemoLabel = chapterMemoBtnLabel,
                            readLabel = readLabel,
                            onChapterMemoClick = { requireAuth { showChapterMemo = true } },
                            onMarkReadClick = {
                                requireAuth {
                                    viewModel.markRead {
                                        Toast.makeText(context, markedReadMessage, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        )
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

    if (showFontSizeDialog) {
        FontSizeDialog(
            currentStep = fontStep,
            onSelect = viewModel::setFontStep,
            onReset = { viewModel.setFontStep(3) },
            onDismiss = { showFontSizeDialog = false },
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

/** 스크롤 이벤트 1회당 상단바/하단 탭 표시 전환 임계값(px) — 책/장 목록 화면과 동일 기준. */
private const val SCROLL_HIDE_THRESHOLD_PX = 3f

/** 글씨 크기 단계(1~5) → 본문 sp 값. */
private fun fontSizeForStep(step: Int): Float = when (step) {
    1 -> 14f
    2 -> 16f
    4 -> 21f
    5 -> 24f
    else -> 18f
}

/** 절 목록 아래 [장 메모][읽음] 반반 폭 버튼 행(웹 verse-list 의 flex-half 구성과 동일). */
@Composable
private fun ChapterActionsRow(
    isRead: Boolean,
    chapterMemoLabel: String,
    readLabel: String,
    onChapterMemoClick: () -> Unit,
    onMarkReadClick: () -> Unit,
) {
    val readGreen = Color(0xFF16A34A)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onChapterMemoClick,
            modifier = Modifier.weight(1f),
        ) {
            Text("📝", modifier = Modifier.padding(end = 6.dp))
            Text(chapterMemoLabel)
        }
        OutlinedButton(
            onClick = onMarkReadClick,
            enabled = !isRead,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = readGreen),
        ) {
            Text("✓", modifier = Modifier.padding(end = 6.dp))
            Text(readLabel)
        }
    }
}

/** Aa 글씨 크기 다이얼로그 — 5단계 라디오 선택 + 기본으로 초기화. */
@Composable
private fun FontSizeDialog(
    currentStep: Int,
    onSelect: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val stepLabels = listOf(
        stringResource(R.string.bible_font_step_1),
        stringResource(R.string.bible_font_step_2),
        stringResource(R.string.bible_font_step_3),
        stringResource(R.string.bible_font_step_4),
        stringResource(R.string.bible_font_step_5),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bible_font_size_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                stepLabels.forEachIndexed { index, label ->
                    val step = index + 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(step) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = step == currentStep, onClick = { onSelect(step) })
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
        dismissButton = {
            TextButton(onClick = onReset) { Text(stringResource(R.string.bible_font_reset)) }
        },
    )
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
