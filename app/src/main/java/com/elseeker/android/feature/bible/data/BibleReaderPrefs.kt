package com.elseeker.android.feature.bible.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 절 본문 글씨 크기 단계(1~5, 기본 3=기본 크기)를 로컬(SharedPreferences)에 저장한다. */
@Singleton
class BibleReaderPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadFontStep(): Int = prefs.getInt(KEY_FONT_STEP, DEFAULT_FONT_STEP).coerceIn(1, 5)

    fun saveFontStep(step: Int) {
        prefs.edit { putInt(KEY_FONT_STEP, step.coerceIn(1, 5)) }
    }

    companion object {
        const val DEFAULT_FONT_STEP = 3
        private const val PREFS_NAME = "bible_reader_prefs"
        private const val KEY_FONT_STEP = "verse_font_step"
    }
}
