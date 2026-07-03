package com.elseeker.android.feature.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.feature.bible.ui.components.BiblePageTitle
import com.elseeker.android.feature.bible.ui.components.BibleTopBar
import com.elseeker.android.feature.study.data.DictionaryDetailWithRefs
import com.elseeker.android.feature.study.data.DictionaryReferenceDto

/** 사전 상세 화면(웹 파리티): 헤더 카드 + 의미·설명 + 관련 성경 구절 칩. */
@Composable
fun DictionaryDetailScreen(
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictionaryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        BibleTopBar(onBack = onBack, onProfileClick = onProfileClick)
        ResourceContent(
            resource = state,
            onRetry = viewModel::load,
            modifier = Modifier.fillMaxWidth(),
        ) { data ->
            DictionaryDetailContent(data)
        }
    }
}

@Composable
private fun DictionaryDetailContent(data: DictionaryDetailWithRefs) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 헤더 카드 — 상단 파란 강조 바 + 중앙 용어명.
        item(key = "header") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text = data.detail.term,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                    )
                }
            }
        }

        // 의미 및 설명.
        item(key = "meaning") {
            SectionHeader(emoji = "📖", title = stringResource(R.string.dictionary_meaning_header))
            Spacer(Modifier.height(12.dp))
            Text(
                text = data.detail.description?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.dictionary_detail_no_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.6,
            )
        }

        // 관련 성경 구절 — 좌측 강조 보더 카드 안에 칩(FlowRow).
        if (data.references.isNotEmpty()) {
            item(key = "refs") {
                SectionHeader(emoji = "🔗", title = stringResource(R.string.dictionary_references_header))
                Spacer(Modifier.height(12.dp))
                ReferenceCard(references = data.references)
            }
        }
    }
}

@Composable
private fun SectionHeader(emoji: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReferenceCard(references: List<DictionaryReferenceDto>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            references.sortedBy { it.displayOrder }.forEach { ref ->
                ReferenceChip(ref.verseLabel)
            }
        }
    }
}

@Composable
private fun ReferenceChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
