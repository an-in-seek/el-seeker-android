package com.elseeker.android.feature.bible.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.feature.bible.data.BibleSearchSliceDto.BibleSearchItemDto

/** 성경 절 검색 화면: 검색 필드 + 인기 검색어 칩 + 결과 목록. 결과 탭 → 해당 장 본문. */
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
    val translationId by viewModel.translationId.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bible_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text(stringResource(R.string.bible_search_placeholder)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = viewModel::search) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.common_search))
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            )

            if (ranking.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ranking.forEach { item ->
                        AssistChip(
                            onClick = { viewModel.onKeywordClick(item.keyword) },
                            label = { Text("${item.rank}. ${item.keyword}") },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (!hasSearched) {
                Hint(stringResource(R.string.bible_search_hint_initial))
            } else {
                ResourceContent(
                    resource = results,
                    onRetry = viewModel::search,
                    modifier = Modifier.fillMaxSize(),
                ) { items ->
                    if (items.isEmpty()) {
                        Hint(stringResource(R.string.bible_search_hint_no_results))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(items, key = { it.verseId }) { item ->
                                SearchResultRow(item) {
                                    translationId?.let { tid ->
                                        onResultClick(tid, item.bookOrder, item.chapterNumber)
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

@Composable
private fun SearchResultRow(item: BibleSearchItemDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = "${item.bookName} ${item.chapterNumber}:${item.verseNumber}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Hint(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
