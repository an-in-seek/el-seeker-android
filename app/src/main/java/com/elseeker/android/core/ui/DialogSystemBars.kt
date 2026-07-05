package com.elseeker.android.core.ui

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * Dialog·ModalBottomSheet 는 Activity 와 **별개의 윈도우**에 렌더된다. 그래서 MainActivity 에서
 * 맞춘 검정 시스템 바가 적용되지 않고, 이런 윈도우가 뜨면 하단 내비게이션 바가 테마 기본값(흰색)으로
 * 보인다. 이 효과를 Dialog/BottomSheet 콘텐츠 **안에서** 호출하면 그 윈도우의 내비게이션 바를
 * 검정 + 흰색 아이콘으로 맞춰 Activity 와 통일한다.
 *
 * UI 를 그리지 않으므로(부수효과 전용) 다이얼로그의 아무 슬롯 안에나 넣어도 안전하다.
 */
@Composable
fun ForceBlackNavigationBarInDialog() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.navigationBarColor = Color.BLACK
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
    }
}
