package com.elseeker.android.feature.study.ui.content

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elseeker.android.R
import kotlin.math.abs

/**
 * "7일 창조 체험" 몰입형 화면(웹 파리티, docs/view/genesis-7days-1·2·3·last.png).
 *
 * 세로 페이저로 인트로(창세기 1:1–2) → 첫째~일곱째 날 → 마무리(창세기 1:31 + CTA)를 한 화면씩 넘긴다.
 * 각 날은 고유 그라데이션 테마·거대 워터마크 숫자·중앙 정렬 타이포(날 라벨 → 말씀 제목 →
 * 해설 → 저녁/아침 후렴) + 하단 진행바/페이지 표시로 구성한다.
 * 텍스트 데이터는 기존 [creationContent](StudyStaticContent) 를 그대로 재사용한다.
 */
@Composable
fun CreationExperienceScreen(
    onBack: () -> Unit,
    onReadBible: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = StudyStaticContent.byKey("creation")
    val days = content?.cards.orEmpty()
    // 인트로 1장 + 날짜 카드 수(7) + 마무리 1장. 데이터가 비면 인트로/마무리만 노출.
    val pageCount = 2 + days.size
    val outroPage = pageCount - 1
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            // 정착 위치로부터의 거리(-1..1)로 페이드/패럴럭스를 줘 전환에 깊이를 준다.
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val settle = 1f - abs(pageOffset).coerceIn(0f, 1f)
            val contentModifier = Modifier.graphicsLayer {
                alpha = 0.25f + 0.75f * settle
                translationY = pageOffset * 90f
            }

            when {
                page == 0 -> IntroPage(
                    title = content?.paragraphs?.firstOrNull().orEmpty(),
                    description = content?.paragraphs?.drop(1).orEmpty(),
                    contentModifier = contentModifier,
                )

                page == outroPage -> OutroPage(
                    blessing = content?.history?.firstOrNull()?.body.orEmpty(),
                    onReadBible = onReadBible,
                    onMoreStudy = onBack,
                    contentModifier = contentModifier,
                )

                else -> {
                    val dayNumber = page
                    DayPage(
                        dayNumber = dayNumber,
                        label = days[page - 1].tag,
                        title = days[page - 1].title,
                        meaning = days[page - 1].meaning,
                        refrain = days[page - 1].quote,
                        theme = dayThemes[(page - 1).coerceIn(0, dayThemes.lastIndex)],
                        contentModifier = contentModifier,
                    )
                }
            }
        }

        // 몰입을 해치지 않는 반투명 뒤로가기(항상 최상단 고정).
        // NavHost 가 시스템 인셋(padding(inner))을 이미 소비하므로 여기서는 추가로 인셋을 넣지 않는다.
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

/** 인트로: 검은 배경 + "창세기 1장" 라벨/대제목/해설/참조 + 하단 스크롤 힌트. */
@Composable
private fun IntroPage(
    title: String,
    description: List<String>,
    contentModifier: Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black, Color(0xFF080808), Color.Black),
                ),
            ),
    ) {
        Column(
            modifier = contentModifier
                .align(Alignment.Center)
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = INTRO_LABEL,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                letterSpacing = 4.sp,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            description.forEach { line ->
                Text(
                    text = line,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = INTRO_REFERENCE,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp,
                letterSpacing = 3.sp,
            )
        }

        ScrollHint(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        )
    }
}

/** 하단 스크롤 유도 — "scroll" + 위아래로 살짝 움직이는 아래 화살표. */
@Composable
private fun ScrollHint(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scroll-hint")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "bob",
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "scroll",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            letterSpacing = 4.sp,
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.offset(y = bob.dp),
        )
    }
}

/** 하루: 테마 그라데이션 배경 + 워터마크 숫자 + 중앙 타이포 + 하단 진행 표시. */
@Composable
private fun DayPage(
    dayNumber: Int,
    label: String,
    title: String,
    meaning: String,
    refrain: String?,
    theme: DayTheme,
    contentModifier: Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background),
    ) {
        // 배경 뒤로 은은하게 깔리는 거대 날짜 숫자.
        Text(
            text = "$dayNumber",
            color = Color.White.copy(alpha = 0.06f),
            fontSize = 200.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-96).dp),
        )

        Column(
            modifier = contentModifier
                .align(Alignment.Center)
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 14.sp,
                letterSpacing = 4.sp,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 26.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = meaning,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 15.sp,
                lineHeight = 25.sp,
                textAlign = TextAlign.Center,
            )
            if (!refrain.isNullOrBlank()) {
                Spacer(Modifier.height(28.dp))
                Text(
                    text = refrain,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 페이지 표시 "N / 7".
        Text(
            text = "$dayNumber / ${dayThemes.size}",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )

        // 하단 진행바(금색) — 진행도 = 현재 날 / 7.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(dayNumber / dayThemes.size.toFloat())
                    .height(3.dp)
                    .background(AccentGold),
            )
        }
    }
}

/**
 * 마무리(창세기 1:31): 따뜻한 어둠 배경 + 금빛 축복 말씀 + 참조 + [성경 읽기]/[학습 더보기] CTA.
 * (docs/view/genesis-7days-last.png)
 */
@Composable
private fun OutroPage(
    blessing: String,
    onReadBible: () -> Unit,
    onMoreStudy: () -> Unit,
    contentModifier: Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF120A04), Color(0xFF1E1206), Color(0xFF0A0602)),
                ),
            ),
    ) {
        Column(
            modifier = contentModifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = blessing,
                color = AccentGold,
                fontSize = 20.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = OUTRO_REFERENCE,
                color = AccentGold.copy(alpha = 0.55f),
                fontSize = 13.sp,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onReadBible,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = Color(0xFF241703),
                ),
            ) {
                Text(OUTRO_READ_BIBLE, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onMoreStudy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
            ) {
                Text(OUTRO_MORE_STUDY, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** 날짜별 배경 브러시 묶음(빛→궁창→뭍→광명체→물고기·새→사람→안식). */
private data class DayTheme(val background: Brush)

/** 웹 파리티 진행바/포인트 금색. */
private val AccentGold = Color(0xFFD4AF37)

private const val INTRO_LABEL = "창세기 1장"
private const val INTRO_REFERENCE = "창세기 1:1–2"
private const val OUTRO_REFERENCE = "창세기 1:31"
private const val OUTRO_READ_BIBLE = "성경 읽기"
private const val OUTRO_MORE_STUDY = "학습 더보기"

/**
 * 일곱 날의 색 테마(인덱스 0=첫째 날 … 6=일곱째 날).
 * - 1 빛: 어둠 속에서 터지는 빛 → 방사형 그라데이션
 * - 2 궁창: 물 위의 깊은 파랑
 * - 3 뭍/식물: 초록 대지
 * - 4 광명체: 별이 뜬 남색 밤
 * - 5 물고기·새: 청록 바다
 * - 6 사람·짐승: 따뜻한 흙빛
 * - 7 안식: 은은한 금빛 안식
 */
private val dayThemes: List<DayTheme> = listOf(
    DayTheme(Brush.radialGradient(listOf(Color(0xFFB7CEE6), Color(0xFF1B3049), Color(0xFF0A1421)))),
    DayTheme(Brush.verticalGradient(listOf(Color(0xFF0B2545), Color(0xFF16406B), Color(0xFF0A2140)))),
    DayTheme(Brush.verticalGradient(listOf(Color(0xFF0B3D2E), Color(0xFF16624A), Color(0xFF0A3527)))),
    DayTheme(Brush.verticalGradient(listOf(Color(0xFF10143A), Color(0xFF232A5E), Color(0xFF090D26)))),
    DayTheme(Brush.verticalGradient(listOf(Color(0xFF06323D), Color(0xFF0D5A6C), Color(0xFF052A34)))),
    DayTheme(Brush.verticalGradient(listOf(Color(0xFF3A2416), Color(0xFF6E4224), Color(0xFF281710)))),
    DayTheme(Brush.verticalGradient(listOf(Color(0xFF2E2410), Color(0xFF5F4C1E), Color(0xFF231B0B)))),
)
