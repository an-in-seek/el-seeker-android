package com.elseeker.android.feature.bible.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.feature.bible.data.BookDetailDto

/** 책 개요 화면: 책 설명(요약/저자/배경) + 장 번호 그리드. 장 탭 → 본문. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleBookOverviewScreen(
    onBack: () -> Unit,
    onChapterClick: (chapterNumber: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BibleBookOverviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 리더에서 돌아오면 읽음 표시를 최신화한다(최초 진입 로드 포함).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text((state as? com.elseeker.android.core.ui.UiResource.Success)?.data?.detail?.bookName ?: stringResource(R.string.bible_book_overview_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { inner ->
        ResourceContent(
            resource = state,
            onRetry = viewModel::load,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) { overview ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DescriptionCard(overview.detail)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.bible_chapter_select_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (overview.readChapters.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.bible_chapter_read_legend),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                items(overview.chapters, key = { it }) { chapter ->
                    ChapterCell(
                        chapter = chapter,
                        isRead = chapter in overview.readChapters,
                        onClick = { onChapterClick(chapter) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DescriptionCard(detail: BookDetailDto) {
    val d = detail.description
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
