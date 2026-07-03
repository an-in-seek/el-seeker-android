package com.elseeker.android.feature.study.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.feature.bible.ui.components.BibleTopBar
import com.elseeker.android.feature.study.data.DictionaryDetailWithRefs
import com.elseeker.android.feature.study.data.DictionaryReferenceDto

/**
 * 사전 상세 화면(웹 bible-dictionary3 파리티): 전체를 하나의 외곽 카드로 묶고
 * 헤더(중앙 강조 바 + 용어명 + 하단 divider) → 📖 의미 및 설명 → 🔗 관련 성경 구절 순으로 렌더링한다.
 * 섹션 헤더는 제목 우측으로 뻗는 구분선을 가지며, 관련 구절이 없으면 빈 상태 안내를 표시한다.
 */
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailHeader(term = data.detail.term)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                    // 의미 및 설명.
                    SectionHeader(emoji = "📖", title = stringResource(R.string.dictionary_meaning_header))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = data.detail.description?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.dictionary_detail_no_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.6,
                    )

                    Spacer(Modifier.height(28.dp))

                    // 관련 성경 구절 — 항상 표시(없으면 빈 상태 안내).
                    SectionHeader(emoji = "🔗", title = stringResource(R.string.dictionary_references_header))
                    Spacer(Modifier.height(14.dp))
                    ReferenceCard(references = data.references)
                }
            }
        }
    }
}

/** 헤더 — 상단 중앙 강조 바(탭 인디케이터형) + 은은한 그라데이션 + 중앙 용어명. */
@Composable
private fun DetailHeader(term: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = term,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 24.dp),
        )
    }
}

/** 섹션 헤더 — 이모지 + 제목 + 우측으로 뻗는 구분선(웹 파리티). */
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
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** 관련 구절 카드 — 좌측 강조 바 + 옅은 배경 + 우상단 인용부호. 비어 있으면 안내 문구. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReferenceCard(references: List<DictionaryReferenceDto>) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
            if (references.isEmpty()) {
                Text(
                    text = stringResource(R.string.dictionary_references_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                FlowRow(
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
        // 우상단 장식 인용부호(웹 파리티) — 옅은 primary 큰따옴표.
        Text(
            text = "”",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 18.dp),
        )
    }
}

@Composable
private fun ReferenceChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
