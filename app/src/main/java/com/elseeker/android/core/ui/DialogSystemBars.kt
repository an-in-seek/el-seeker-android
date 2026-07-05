package com.elseeker.android.core.ui

import android.graphics.Color
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * Dialog·ModalBottomSheet 는 Activity 와 **별개의 윈도우**에 렌더된다. 그래서 MainActivity 에서
 * 맞춘 검정 시스템 바가 적용되지 않고, 이런 윈도우가 뜨면 하단 내비게이션 바가 흰색으로 보인다.
 *
 * 원인은 두 가지다:
 * 1) 별도 윈도우의 `navigationBarColor` 가 테마 기본값(흰색)이다.
 * 2) 투명 내비바에 시스템이 대비용 반투명 스크림(밝은 색)을 덧씌운다(`isNavigationBarContrastEnforced`).
 *
 * 이 효과를 Dialog/BottomSheet 콘텐츠 **안에서** 호출하면 그 윈도우의 내비게이션 바를
 * 검정 + 흰색 아이콘 + 대비 스크림 제거로 맞춰 Activity 와 통일한다. UI 를 그리지 않으므로
 * (부수효과 전용) 다이얼로그의 아무 슬롯 안에나 넣어도 안전하다.
 */
@Composable
fun ForceBlackNavigationBarInDialog() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.navigationBarColor = Color.BLACK
        // 시스템이 투명 내비바 위에 덧씌우는 반투명(밝은) 대비 스크림 제거 — 흰색으로 보이던 주원인.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        // 검정 배경 위에서 보이도록 내비바 아이콘을 밝게(흰색) 유지.
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
    }
}
