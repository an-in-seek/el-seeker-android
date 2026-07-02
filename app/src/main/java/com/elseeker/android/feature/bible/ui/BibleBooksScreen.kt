package com.elseeker.android.feature.bible.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.feature.bible.data.BookDto

/**
 * 성경책 목록 화면: 이름 검색 + 구약/신약 접이식 섹션(웹 book-list 와 동일한 UX).
 * 검색어 입력 중에는 결과가 있는 섹션만 표시하고 항상 펼친 상태로 보여준다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleBooksScreen(
    onBack: () -> Unit,
    onBookClick: (translationId: Long, bookOrder: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BibleBooksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val oldExpanded by viewModel.oldExpanded.collectAsStateWithLifecycle()
    val newExpanded by viewModel.newExpanded.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bible_books_title)) },
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
            BookSearchField(query = query, onQueryChange = viewModel::onQueryChange)

            ResourceContent(resource = state, onRetry = viewModel::load, modifier = Modifier.fillMaxSize()) { data ->
                // LazyGridScope 는 컴포저블 컨텍스트가 아니므로 stringResource 를 밖에서 해석한다.
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

                if (searching && filteredOld.isEmpty() && filteredNew.isEmpty()) {
                    SearchEmptyHint(searchEmptyLabel)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // 검색 중에는 결과가 있는 섹션을 항상 펼친 상태로 보여준다(웹 book-list.js 와 동일).
                        if (filteredOld.isNotEmpty()) {
                            collapsibleSection(
                                label = oldTestamentLabel,
                                books = filteredOld,
                                expanded = searching || oldExpanded,
                                onToggleExpand = viewModel::toggleOldTestament,
                                translationId = viewModel.translationId,
                                onBookClick = onBookClick,
                            )
                        }
                        if (filteredNew.isNotEmpty()) {
                            collapsibleSection(
                                label = newTestamentLabel,
                                books = filteredNew,
                                expanded = searching || newExpanded,
                                onToggleExpand = viewModel::toggleNewTestament,
                                translationId = viewModel.translationId,
                                onBookClick = onBookClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
    )
}

@Composable
private fun SearchEmptyHint(message: String) {
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

private fun LazyGridScope.collapsibleSection(
    label: String,
    books: List<BookDto>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    translationId: Long,
    onBookClick: (Long, Int) -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        SectionHeader(label = label, count = books.size, expanded = expanded, onClick = onToggleExpand)
    }
    if (expanded) {
        items(books, key = { it.bookId }) { book ->
            BookCell(book) { onBookClick(translationId, book.bookOrder) }
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = stringResource(R.string.bible_books_count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BookCell(book: BookDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(
            text = book.bookName,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
        )
    }
}
