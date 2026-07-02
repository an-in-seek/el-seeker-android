package com.elseeker.android.feature.study.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elseeker.android.R
import com.elseeker.android.feature.study.ui.content.StudyContentItem
import com.elseeker.android.feature.study.ui.content.StudyStaticContent
import com.elseeker.android.feature.study.ui.content.StudyTrack

/**
 * 학습 탭 홈(허브). 사전 + 정적 콘텐츠 진입점을 나열한다.
 * S1 항목은 준비된 것부터 실제 화면으로, S2(족보·개요영상)는 준비중 배지로 안내한다(PRD §4-A.7).
 */
@Composable
fun StudyScreen(
    onOpenDictionary: () -> Unit,
    onOpenContent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.study_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
        }

        // 사전 (API 연동)
        item {
            EntryCard(
                title = stringResource(R.string.study_dictionary_entry_title),
                subtitle = stringResource(R.string.study_dictionary_entry_subtitle),
                badge = null,
                onClick = onOpenDictionary,
            )
        }

        item { SectionHeader(stringResource(R.string.study_section_bible_study)) }
        items(StudyStaticContent.items.filter { it.track == StudyTrack.S1 }, key = { it.key }) { c ->
            ContentEntryCard(c, onOpenContent)
        }

        item { SectionHeader(stringResource(R.string.study_section_rich_content)) }
        items(StudyStaticContent.items.filter { it.track == StudyTrack.S2 }, key = { it.key }) { c ->
            ContentEntryCard(c, onOpenContent)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ContentEntryCard(item: StudyContentItem, onClick: (String) -> Unit) {
    EntryCard(
        title = item.title,
        subtitle = item.subtitle,
        badge = if (!item.ready) stringResource(R.string.study_badge_preparing) else null,
        onClick = { onClick(item.key) },
    )
}

@Composable
private fun EntryCard(
    title: String,
    subtitle: String,
    badge: String?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
