package com.elseeker.android.feature.bible.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 성경 로컬 설정(SharedPreferences).
 * - 절 본문 글씨 크기 단계(1~5, 기본 3=기본 크기).
 * - 가장 최근 선택한 검색 번역본 id — 검색 화면 재진입 시 기본값으로 복원한다.
 */
@Singleton
class BibleReaderPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadFontStep(): Int = prefs.getInt(KEY_FONT_STEP, DEFAULT_FONT_STEP).coerceIn(1, 5)

    fun saveFontStep(step: Int) {
        prefs.edit { putInt(KEY_FONT_STEP, step.coerceIn(1, 5)) }
    }

    /** 마지막으로 선택한 검색 번역본 id. 저장값이 없으면 [NO_TRANSLATION]. */
    fun loadSelectedTranslationId(): Long = prefs.getLong(KEY_SELECTED_TRANSLATION, NO_TRANSLATION)

    fun saveSelectedTranslationId(translationId: Long) {
        prefs.edit { putLong(KEY_SELECTED_TRANSLATION, translationId) }
    }

    companion object {
        const val DEFAULT_FONT_STEP = 3
        const val NO_TRANSLATION = -1L
        private const val PREFS_NAME = "bible_reader_prefs"
        private const val KEY_FONT_STEP = "verse_font_step"
        private const val KEY_SELECTED_TRANSLATION = "search_translation_id"
    }
}
