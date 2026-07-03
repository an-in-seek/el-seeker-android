package com.elseeker.android.feature.study.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elseeker.android.R
import com.elseeker.android.feature.bible.ui.components.BiblePageTitle
import com.elseeker.android.feature.bible.ui.components.BibleTopBar
import com.elseeker.android.feature.study.ui.content.StudyStaticContent

/**
 * 학습 탭 홈(허브) — 웹 study-menu 파리티: 상단바(프로필) + 중앙 타이틀 "학습" +
 * 섹션 구분 없는 **평면 메뉴 리스트**. 항목마다 고유 이모지 + 라벨의 흰색 카드로 나열한다.
 * 사전은 API 목록으로, 나머지는 정적 콘텐츠 화면으로 진입한다. 아직 준비 안 된 항목(족보)은
 * "준비 중" 배지로 안내한다(빈 진입점 금지 — 탭하면 준비중 화면으로 이동, PRD §4-A.7).
 */
@Composable
fun StudyScreen(
    onOpenDictionary: () -> Unit,
    onOpenContent: (String) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        BibleTopBar(onProfileClick = onProfileClick)
        BiblePageTitle(stringResource(R.string.study_title))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(studyMenu, key = { it.key }) { entry ->
                StudyMenuCard(
                    entry = entry,
                    // 준비 안 된 정적 콘텐츠는 "준비 중" 배지. 사전(dictionary)은 항상 준비됨.
                    preparing = StudyStaticContent.byKey(entry.key)?.ready == false,
                    onClick = {
                        if (entry.key == DICTIONARY_KEY) onOpenDictionary() else onOpenContent(entry.key)
                    },
                )
            }
        }
    }
}

/** 학습 메뉴 항목 카드 — 좌측 이모지 + 라벨. 웹의 흰색 라운드 카드 파리티. */
@Composable
private fun StudyMenuCard(
    entry: StudyMenuItem,
    preparing: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = entry.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(14.dp))
            Text(
                text = entry.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (preparing) {
                PreparingBadge()
            }
        }
    }
}

/** "준비 중" 배지 — 옅은 회색 pill. */
@Composable
private fun PreparingBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.study_badge_preparing),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 사전 진입 항목의 예약 key(정적 콘텐츠 key 와 겹치지 않음). */
private const val DICTIONARY_KEY = "dictionary"

/** 학습 메뉴 항목 정의(웹 study-menu 순서·라벨·이모지 파리티). */
private data class StudyMenuItem(
    val key: String,
    val emoji: String,
    val label: String,
)

/** 웹 학습 메뉴와 동일한 순서·라벨. key 는 정적 콘텐츠 카탈로그(StudyStaticContent)와 매칭된다. */
private val studyMenu: List<StudyMenuItem> = listOf(
    StudyMenuItem("creation", "✨", "7일 창조 체험"),
    StudyMenuItem(DICTIONARY_KEY, "📖", "성경 사전"),
    StudyMenuItem("history", "📜", "성경 역사"),
    StudyMenuItem("overview-video", "▶️", "성경 개요"),
    StudyMenuItem("public-reading", "🎬", "공동체성경읽기"),
    StudyMenuItem("genealogy", "👪", "성경 족보"),
    StudyMenuItem("twelve-tribes", "🚩", "이스라엘 12지파"),
    StudyMenuItem("twelve-disciples", "👥", "예수님의 12제자"),
    StudyMenuItem("lords-prayer", "🙏", "주기도문"),
    StudyMenuItem("ten-commandments", "📋", "십계명"),
    StudyMenuItem("apostles-creed", "✝️", "사도신경"),
    StudyMenuItem("holy-week", "⛪", "성주간"),
    StudyMenuItem("commentary", "🔖", "성경 주석"),
)
