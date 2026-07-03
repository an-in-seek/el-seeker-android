package com.elseeker.android.ui.theme

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 테마 모드(웹 계정 메뉴의 라이트/다크/시스템). */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** 테마 모드 선택을 로컬(SharedPreferences)에 저장하고 전역 관찰 가능하게 노출한다. */
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(loadMode())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        prefs.edit { putString(KEY_MODE, mode.name) }
    }

    private fun loadMode(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString(KEY_MODE, ThemeMode.SYSTEM.name)!!)
    }.getOrDefault(ThemeMode.SYSTEM)

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_MODE = "theme_mode"
    }
}
