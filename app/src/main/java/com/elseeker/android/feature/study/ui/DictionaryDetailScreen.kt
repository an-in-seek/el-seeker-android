package com.elseeker.android.feature.study.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.feature.study.data.DictionaryDetailWithRefs
import com.elseeker.android.feature.study.data.DictionaryReferenceDto

/** 사전 상세 화면: 용어/설명 + 성경 참조 목록. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictionaryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dictionary_detail_title)) },
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
        ) { data ->
            DictionaryDetailContent(data)
        }
    }
}

@Composable
private fun DictionaryDetailContent(data: DictionaryDetailWithRefs) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text(
                text = data.detail.term,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = data.detail.description?.takeIf { it.isNotBlank() } ?: stringResource(R.string.dictionary_detail_no_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (data.references.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.dictionary_detail_bible_reference_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
            }
            items(data.references, key = { it.referenceId }) { ref ->
                ReferenceCard(ref)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReferenceCard(ref: DictionaryReferenceDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = ref.verseLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp),
        )
    }
}
