package com.elseeker.android.feature.bible.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.R
import com.elseeker.android.core.ui.ResourceContent
import com.elseeker.android.feature.bible.data.TranslationDto
import com.elseeker.android.feature.bible.ui.components.BibleTopBar

/**
 * 성경 탭 루트 화면: 노출 번역본 목록(웹 /web/bible/translation 과 동일한 UX, docs/view/translation-list.jpg).
 * 언어별로 그룹핑한 카드 목록. 탭 루트라 뒤로가기 없음. 항목 탭 → [onTranslationClick]으로 책 목록 이동.
 */
@Composable
fun TranslationListScreen(
    onTranslationClick: (Long) -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TranslationListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BibleTopBar(
            title = stringResource(R.string.bible_translation_title),
            onBack = onBack,
            onProfileClick = onProfileClick,
        )

        ResourceContent(
            resource = state,
            onRetry = viewModel::load,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) { translations ->
            // groupByLanguage 는 @Composable(내부 remember + stringResource) — remember 람다로 감싸면 컴파일 불가.
            val groups = groupByLanguage(translations)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // 타이틀이 상단바로 이동했으므로 상단바와 첫 카드 사이 간격을 top 패딩으로 확보한다.
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(groups, key = { it.languageLabel }) { group ->
                    LanguageGroupCard(group, onTranslationClick)
                }
            }
        }
    }
}

private data class LanguageGroup(
    val languageLabel: String,
    val translations: List<TranslationDto>,
)

@Composable
private fun groupByLanguage(translations: List<TranslationDto>): List<LanguageGroup> {
    val labelMap = mapOf(
        "ko" to stringResource(R.string.bible_lang_ko),
        "en" to stringResource(R.string.bible_lang_en),
        "zh" to stringResource(R.string.bible_lang_zh),
        "ja" to stringResource(R.string.bible_lang_ja),
        "es" to stringResource(R.string.bible_lang_es),
        "de" to stringResource(R.string.bible_lang_de),
        "la" to stringResource(R.string.bible_lang_la),
    )
    return remember(translations) {
        val order = mutableListOf<String>()
        val byLanguage = mutableMapOf<String, MutableList<TranslationDto>>()
        translations.forEach { translation ->
            val key = translation.translationLanguage?.lowercase() ?: "-"
            if (key !in byLanguage) {
                order += key
                byLanguage[key] = mutableListOf()
            }
            byLanguage.getValue(key) += translation
        }
        order.map { key ->
            LanguageGroup(
                languageLabel = labelMap[key] ?: key,
                translations = byLanguage.getValue(key),
            )
        }
    }
}

@Composable
private fun LanguageGroupCard(group: LanguageGroup, onTranslationClick: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = group.languageLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                CountBadge(count = group.translations.size)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column {
                    group.translations.forEachIndexed { index, translation ->
                        TranslationRow(translation) { onTranslationClick(translation.translationId) }
                        if (index != group.translations.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = stringResource(R.string.bible_translation_count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun TranslationRow(translation: TranslationDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(
            text = translation.translationName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = translation.translationType,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
