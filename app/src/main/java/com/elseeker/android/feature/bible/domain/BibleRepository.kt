package com.elseeker.android.feature.bible.domain

import com.elseeker.android.core.network.orThrow
import com.elseeker.android.core.network.safeApiCall
import com.elseeker.android.feature.bible.data.BibleApi
import com.elseeker.android.feature.bible.data.BibleSearchSliceDto
import com.elseeker.android.feature.bible.data.BookDetailDto
import com.elseeker.android.feature.bible.data.BookDto
import com.elseeker.android.feature.bible.data.ChapterStateDto
import com.elseeker.android.feature.bible.data.ChaptersDto
import com.elseeker.android.feature.bible.data.HighlightItemDto
import com.elseeker.android.feature.bible.data.HighlightRequest
import com.elseeker.android.feature.bible.data.KeywordRankingDto
import com.elseeker.android.feature.bible.data.MemoItemDto
import com.elseeker.android.feature.bible.data.MemoRequest
import com.elseeker.android.feature.bible.data.TranslationDto
import com.elseeker.android.feature.bible.data.VersesDto
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

object BibleNav {
    const val PREV = "PREV"
    const val NEXT = "NEXT"
}

@Singleton
class BibleRepository @Inject constructor(
    private val bibleApi: BibleApi,
    private val json: Json,
) {

    /**
     * 노출 가능한 번역본만 반환(PRD §4-A.9 데이터 게이트).
     * v1 은 본문 seed 가 완비된 KRV 만 허용한다. NKRV/KJV 는 백엔드 visibility 정책 확정 전까지 숨김.
     */
    suspend fun visibleTranslations(): Result<List<TranslationDto>> = runCatching {
        val all = safeApiCall(json) { bibleApi.translations() }
        all.filter { it.translationType in VISIBLE_TRANSLATIONS }
            .ifEmpty { all.filter { it.translationType == DEFAULT_TRANSLATION } }
    }

    suspend fun dailyVerse(): Result<com.elseeker.android.feature.bible.data.DailyVerseDto> =
        runCatching { safeApiCall(json) { bibleApi.dailyVerse(DEFAULT_TRANSLATION) } }

    suspend fun books(translationId: Long): Result<List<BookDto>> =
        runCatching { safeApiCall(json) { bibleApi.books(translationId) } }

    suspend fun chapters(translationId: Long, bookOrder: Int): Result<ChaptersDto> =
        runCatching { safeApiCall(json) { bibleApi.chapters(translationId, bookOrder) } }

    /** 읽은 장 번호 목록(인증 필요). 실패해도 목록 화면을 막지 않도록 호출부에서 빈 목록 폴백. */
    suspend fun readChapters(translationId: Long, bookOrder: Int): Result<List<Int>> =
        runCatching {
            safeApiCall(json) { bibleApi.readChapters(translationId, bookOrder) }.chapterNumbers
        }

    suspend fun verses(translationId: Long, bookOrder: Int, chapterNumber: Int): Result<VersesDto> =
        runCatching { safeApiCall(json) { bibleApi.verses(translationId, bookOrder, chapterNumber) } }

    suspend fun navigate(
        translationId: Long,
        bookOrder: Int,
        chapterNumber: Int,
        direction: String,
    ): Result<VersesDto> = runCatching {
        safeApiCall(json) { bibleApi.navigate(translationId, bookOrder, chapterNumber, direction) }
    }

    suspend fun bookDetail(translationId: Long, bookOrder: Int): Result<BookDetailDto> =
        runCatching { safeApiCall(json) { bibleApi.bookDetail(translationId, bookOrder) } }

    /** 절 검색(public). keyword 공백이면 호출자가 막는다. */
    suspend fun search(
        translationId: Long,
        keyword: String,
        bookOrder: Int? = null,
    ): Result<BibleSearchSliceDto> = runCatching {
        safeApiCall(json) { bibleApi.search(translationId, keyword, bookOrder) }
    }

    suspend fun searchKeywordRanking(): Result<KeywordRankingDto> =
        runCatching { safeApiCall(json) { bibleApi.searchKeywordRanking() } }

    /** 장 상태(메모·하이라이트·읽음, 인증 필요). */
    suspend fun chapterState(
        translationId: Long,
        bookOrder: Int,
        chapterNumber: Int,
    ): Result<ChapterStateDto> = runCatching {
        safeApiCall(json) { bibleApi.chapterState(translationId, bookOrder, chapterNumber) }
    }

    suspend fun putHighlight(
        translationId: Long,
        bookOrder: Int,
        chapterNumber: Int,
        verseNumber: Int,
        color: String,
    ): Result<HighlightItemDto> = runCatching {
        safeApiCall(json) {
            bibleApi.putHighlight(translationId, bookOrder, chapterNumber, verseNumber, HighlightRequest(color))
        }
    }

    suspend fun deleteHighlight(
        translationId: Long,
        bookOrder: Int,
        chapterNumber: Int,
        verseNumber: Int,
    ): Result<Unit> = runCatching {
        safeApiCall(json) {
            bibleApi.deleteHighlight(translationId, bookOrder, chapterNumber, verseNumber).orThrow()
        }
        Unit
    }

    suspend fun putVerseMemo(
        translationId: Long,
        bookOrder: Int,
        chapterNumber: Int,
        verseNumber: Int,
        content: String,
    ): Result<MemoItemDto> = runCatching {
        safeApiCall(json) {
            bibleApi.putVerseMemo(translationId, bookOrder, chapterNumber, verseNumber, MemoRequest(content))
        }
    }

    suspend fun deleteVerseMemo(
        translationId: Long,
        bookOrder: Int,
        chapterNumber: Int,
        verseNumber: Int,
    ): Result<Unit> = runCatching {
        safeApiCall(json) {
            bibleApi.deleteVerseMemo(translationId, bookOrder, chapterNumber, verseNumber).orThrow()
        }
        Unit
    }

    /** 읽기 진도 기록(인증 필요). 실패해도 본문 읽기를 막지 않도록 호출자가 무시 가능. */
    suspend fun markChapterRead(translationId: Long, bookOrder: Int, chapterNumber: Int): Result<Unit> =
        runCatching {
            safeApiCall(json) {
                bibleApi.markChapterRead(
                    com.elseeker.android.feature.bible.data.ReadingProgressRequest(
                        translationId, bookOrder, chapterNumber,
                    )
                )
            }
            Unit
        }

    companion object {
        private const val DEFAULT_TRANSLATION = "KRV"
        private val VISIBLE_TRANSLATIONS = setOf("KRV")
    }
}
