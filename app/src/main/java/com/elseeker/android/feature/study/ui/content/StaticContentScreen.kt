package com.elseeker.android.feature.study.ui.content

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elseeker.android.R
import com.elseeker.android.core.ui.openExternalUrl

/**
 * 학습 정적 콘텐츠 공통 렌더러. [contentKey] 로 카탈로그를 조회해
 * 전문 → 유래·역사 → 묵상 카드 → 외부 링크 순으로(원본 웹 페이지와 동일한 구성) 렌더링하고,
 * 아직 원본 데이터가 없는 항목(ready=false)은 "준비 중" 안내를 표시한다.
 * 외부 링크(유튜브·주석 사이트)는 Custom Tabs 로 위임한다(PRD 외부 위임).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaticContentScreen(
    contentKey: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = StudyStaticContent.byKey(contentKey)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(content?.title ?: stringResource(R.string.static_content_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { inner ->
        val body = Modifier
            .fillMaxSize()
            .padding(inner)
        when {
            content == null -> PreparingBox(body, stringResource(R.string.static_content_not_found))
            !content.ready -> PreparingBox(body, stringResource(R.string.static_content_preparing, content.title))
            else -> LazyColumn(
                modifier = body,
                contentPadding = PaddingValues(20.dp),
            ) {
                item {
                    Text(
                        text = content.subtitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(16.dp))
                }
                // 전문 — 원본의 full-text 블록(링크형 콘텐츠 등 전문이 없는 항목은 생략).
                if (content.paragraphs.isNotEmpty()) {
                    item { FullTextCard(content.paragraphs) }
                }
                // 유래·역사 해설.
                if (content.history.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        SectionHeader(content.historyTitle ?: stringResource(R.string.static_content_history_title_fallback))
                    }
                    items(content.history) { section ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = section.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // 계명별/구절별/조항별 묵상 카드.
                if (content.cards.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        SectionHeader(content.cardsTitle ?: stringResource(R.string.static_content_cards_title_fallback))
                    }
                    items(content.cards) { card ->
                        Spacer(Modifier.height(12.dp))
                        MeditationCard(card)
                    }
                }
                // 외부 위임 링크(드라마바이블·개요 영상·주석 사이트) — Custom Tabs.
                if (content.links.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        SectionHeader(content.linksTitle ?: stringResource(R.string.static_content_links_title_fallback))
                    }
                    items(content.links) { link ->
                        Spacer(Modifier.height(8.dp))
                        LinkCard(link)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkCard(link: StudyLink) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openExternalUrl(context, link.url) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!link.subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = link.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FullTextCard(paragraphs: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            paragraphs.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun MeditationCard(card: StudyCard) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = card.tag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!card.quote.isNullOrBlank() && card.quote != card.title) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.static_content_quote, card.quote),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = card.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!card.relatedVerse.isNullOrBlank() && !card.relatedVerseText.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.static_content_related_verse, card.relatedVerse),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = card.relatedVerseText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PreparingBox(modifier: Modifier, message: String) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🛠️", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
