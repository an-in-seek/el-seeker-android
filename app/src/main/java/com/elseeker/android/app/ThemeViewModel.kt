package com.elseeker.android.app

import androidx.lifecycle.ViewModel
import com.elseeker.android.ui.theme.ThemeManager
import com.elseeker.android.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** 계정 메뉴의 테마 전환용 — @Singleton ThemeManager 를 Compose 에서 관찰/변경한다. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeManager: ThemeManager,
) : ViewModel() {
    val mode: StateFlow<ThemeMode> = themeManager.mode
    fun setMode(mode: ThemeMode) = themeManager.setMode(mode)
}
