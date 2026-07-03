package com.elseeker.android.feature.bible.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elseeker.android.R

/**
 * 장/절 화면 하단 고정 내비(스크린샷 파리티): [⬅ 아웃라인] [📖 라벨 파란 버튼] [➡ 아웃라인].
 * 중앙 라벨 예: "📖 창세기", "📖 창세기 1". 이전/다음 의미(책/장)는 호출 화면이 정한다.
 */
@Composable
fun BibleBottomBar(
    centerLabel: String,
    onPrev: () -> Unit,
    onCenter: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    prevEnabled: Boolean = true,
    nextEnabled: Boolean = true,
) {
    Surface(modifier = modifier, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onPrev,
                enabled = prevEnabled,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.bible_nav_prev),
                    tint = if (prevEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
            Button(
                onClick = onCenter,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1.6f)
                    .height(52.dp),
            ) {
                Text(
                    text = "📖 $centerLabel",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                onClick = onNext,
                enabled = nextEnabled,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.bible_nav_next),
                    tint = if (nextEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
        }
    }
}
