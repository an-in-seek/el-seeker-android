package com.elseeker.android.feature.bible.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.feature.bible.data.BookDto

/** 성경 탭: 노출 번역본(KRV)의 책 목록을 구약/신약으로 나눠 그리드 표시. */
@Composable
fun BibleBooksScreen(
    onBookClick: (translationId: Long, bookOrder: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BibleBooksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ResourceContent(resource = state, onRetry = viewModel::load, modifier = modifier) { data ->
        // nullable 수신자 재참조를 피하려 non-null 지역 변수로 호이스팅(스마트캐스트 안전).
        val translation = data.translation ?: return@ResourceContent
        val translationId = translation.translationId
        // LazyGridScope 는 컴포저블 컨텍스트가 아니므로 stringResource 를 밖에서 해석한다.
        val oldTestamentLabel = stringResource(R.string.bible_books_old_testament)
        val newTestamentLabel = stringResource(R.string.bible_books_new_testament)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            sectionHeader(translation.translationName)
            section(oldTestamentLabel, data.oldTestament, translationId, onBookClick)
            section(newTestamentLabel, data.newTestament, translationId, onBookClick)
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.sectionHeader(title: String) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.section(
    label: String,
    books: List<BookDto>,
    translationId: Long,
    onBookClick: (Long, Int) -> Unit,
) {
    if (books.isEmpty()) return
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
        )
    }
    items(books, key = { it.bookId }) { book ->
        BookCell(book) { onBookClick(translationId, book.bookOrder) }
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
