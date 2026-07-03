package com.elseeker.android.feature.bible.domain

import com.elseeker.android.core.network.orThrow
import com.elseeker.android.core.network.safeApiCall
import com.elseeker.android.feature.bible.data.BibleApi
import com.elseeker.android.feature.bible.data.BibleSearchSliceDto
import com.elseeker.android.feature.bible.data.BookDetailDto
import com.elseeker.android.feature.bible.data.BookDto
import com.elseeker.android.feature.bible.data.BookMemoItemDto
import com.elseeker.android.feature.bible.data.ChapterMemoItemDto
import com.elseeker.android.feature.bible.data.ChapterStateDto
import com.elseeker.android.feature.bible.data.ChaptersDto
import com.elseeker.android.feature.bible.data.HighlightItemDto
import com.elseeker.android.feature.bible.data.HighlightRequest
import com.elseeker.android.feature.bible.data.KeywordRankingDto
import com.elseeker.android.feature.bible.data.MemoItemDto
import com.elseeker.android.feature.bible.data.MemoRequest
import com.elseeker.android.feature.bible.data.TranslationDto
import com.elseeker.android.feature.bible.data.VersesDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
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

    // 세션 수명 동안 유지되는 인메모리 캐시 — 성경 콘텐츠(번역본/책/장/절/책상세)는 불변이라
    // 화면 재진입·이전/다음 책 이동마다 반복되던 네트워크·역직렬화를 제거한다(디스크 캐시보다 빠름).
    // 값 캐시(성공값 보관) + 진행 중 요청 캐시(Deferred)로 동일 키 동시 요청을 1회로 합친다(stampede 방지).
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val translationsCache = ConcurrentHashMap<String, List<TranslationDto>>()
    private val translationsInFlight = ConcurrentHashMap<String, Deferred<List<TranslationDto>>>()
    private val booksCache = ConcurrentHashMap<Long, List<BookDto>>()
    private val booksInFlight = ConcurrentHashMap<Long, Deferred<List<BookDto>>>()
    private val chaptersCache = ConcurrentHashMap<String, ChaptersDto>()
    private val chaptersInFlight = ConcurrentHashMap<String, Deferred<ChaptersDto>>()
    private val bookDetailCache = ConcurrentHashMap<String, BookDetailDto>()
    private val bookDetailInFlight = ConcurrentHashMap<String, Deferred<BookDetailDto>>()
    private val versesCache = ConcurrentHashMap<String, VersesDto>()
    private val versesInFlight = ConcurrentHashMap<String, Deferred<VersesDto>>()

    /**
     * 값 캐시 히트면 즉시 반환하고, 아니면 동일 키의 진행 중 요청이 있으면 그 결과를 공유하며,
     * 없을 때만 새로 조회한다. 실패한 요청은 in-flight 에서 제거돼 다음 호출이 재시도한다.
     */
    private suspend fun <K : Any, V : Any> coalesced(
        valueCache: ConcurrentHashMap<K, V>,
        inFlight: ConcurrentHashMap<K, Deferred<V>>,
        key: K,
        fetch: suspend () -> V,
    ): V {
        valueCache[key]?.let { return it }
        val deferred = inFlight.computeIfAbsent(key) {
            cacheScope.async { fetch().also { valueCache[key] = it } }
                .also { d -> d.invokeOnCompletion { inFlight.remove(key) } }
        }
        return deferred.await()
    }

    /**
     * 번역본 전체 목록(`GET /api/v1/bibles/translations`) — 웹 translation-list 와 동일하게
     * 서버 응답을 필터 없이 그대로 반환한다. 번역본 목록 화면이 사용. 최초 1회만 네트워크.
     */
    suspend fun translations(): Result<List<TranslationDto>> = runCatching {
        coalesced(translationsCache, translationsInFlight, TRANSLATIONS_KEY) {
            safeApiCall(json) { bibleApi.translations() }
        }
    }

    /**
     * 노출 가능한 번역본만 반환(PRD §4-A.9 데이터 게이트) — 검색 기본 번역본 등
     * "단일 기본 번역본"이 필요한 흐름에서 사용. v1 은 본문 seed 가 완비된 KRV 만 허용.
     */
    suspend fun visibleTranslations(): Result<List<TranslationDto>> = runCatching {
        val all = translations().getOrThrow()
        all.filter { it.translationType in VISIBLE_TRANSLATIONS }
            .ifEmpty { all.filter { it.translationType == DEFAULT_TRANSLATION } }
    }

    suspend fun dailyVerse(): Result<com.elseeker.android.feature.bible.data.DailyVerseDto> =
        runCatching { safeApiCall(json) { bibleApi.dailyVerse(DEFAULT_TRANSLATION) } }

    suspend fun books(translationId: Long): Result<List<BookDto>> = runCatching {
        coalesced(booksCache, booksInFlight, translationId) {
            safeApiCall(json) { bibleApi.books(translationId) }
        }
    }

    /** 인메모리 캐시된 번역본 목록을 동기로 즉시 반환(없으면 null). 로딩 스피너 깜빡임 방지용. */
    fun peekTranslations(): List<TranslationDto>? = translationsCache[TRANSLATIONS_KEY]

    /** 인메모리 캐시된 책 목록을 동기로 즉시 반환(없으면 null). 로딩 스피너 깜빡임 방지용. */
    fun peekBooks(translationId: Long): List<BookDto>? = booksCache[translationId]

    suspend fun chapters(translationId: Long, bookOrder: Int): Result<ChaptersDto> = runCatching {
        coalesced(chaptersCache, chaptersInFlight, "$translationId:$bookOrder") {
            safeApiCall(json) { bibleApi.chapters(translationId, bookOrder) }
        }
    }

    /** 인메모리 캐시에 이미 있는 장 목록을 동기로 즉시 반환(없으면 null). 로딩 스피너 깜빡임 방지용. */
    fun peekChapters(translationId: Long, bookOrder: Int): ChaptersDto? =
        chaptersCache["$translationId:$bookOrder"]

    /** 읽은 장 번호 목록(인증 필요). 실패해도 목록 화면을 막지 않도록 호출부에서 빈 목록 폴백. */
    suspend fun readChapters(translationId: Long, bookOrder: Int): Result<List<Int>> =
        runCatching {
            safeApiCall(json) { bibleApi.readChapters(translationId, bookOrder) }.chapterNumbers
        }

    suspend fun verses(translationId: Long, bookOrder: Int, chapterNumber: Int): Result<VersesDto> =
        runCatching {
            coalesced(versesCache, versesInFlight, "$translationId:$bookOrder:$chapterNumber") {
                safeApiCall(json) { bibleApi.verses(translationId, bookOrder, chapterNumber) }
            }
        }

    suspend fun navigate(
        translationId: Long,
        bookOrder: Int,
        chapterNumber: Int,
        direction: String,
    ): Result<VersesDto> = runCatching {
        safeApiCall(json) { bibleApi.navigate(translationId, bookOrder, chapterNumber, direction) }
    }

    suspend fun bookDetail(translationId: Long, bookOrder: Int): Result<BookDetailDto> =
        runCatching {
            coalesced(bookDetailCache, bookDetailInFlight, "$translationId:$bookOrder") {
                safeApiCall(json) { bibleApi.bookDetail(translationId, bookOrder) }
            }
        }

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

    // --- 장/책 메모 (인증 필요) --------------------------------------------

    suspend fun putChapterMemo(
        translationId: Long,
        bookOrder: Int,
        chapterNumber: Int,
        content: String,
    ): Result<ChapterMemoItemDto> = runCatching {
        safeApiCall(json) {
            bibleApi.putChapterMemo(translationId, bookOrder, chapterNumber, MemoRequest(content))
        }
    }

    suspend fun deleteChapterMemo(
        translationId: Long,
        bookOrder: Int,
        chapterNumber: Int,
    ): Result<Unit> = runCatching {
        safeApiCall(json) {
            bibleApi.deleteChapterMemo(translationId, bookOrder, chapterNumber).orThrow()
        }
        Unit
    }

    /** 책 메모 조회. 메모가 없으면 서버가 204 → null. */
    suspend fun bookMemo(translationId: Long, bookOrder: Int): Result<BookMemoItemDto?> =
        runCatching {
            safeApiCall(json) { bibleApi.bookMemo(translationId, bookOrder).orThrow() }.body()
        }

    suspend fun putBookMemo(
        translationId: Long,
        bookOrder: Int,
        content: String,
    ): Result<BookMemoItemDto> = runCatching {
        safeApiCall(json) { bibleApi.putBookMemo(translationId, bookOrder, MemoRequest(content)) }
    }

    suspend fun deleteBookMemo(translationId: Long, bookOrder: Int): Result<Unit> = runCatching {
        safeApiCall(json) { bibleApi.deleteBookMemo(translationId, bookOrder).orThrow() }
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
        private const val TRANSLATIONS_KEY = "all"
        private const val DEFAULT_TRANSLATION = "KRV"
        private val VISIBLE_TRANSLATIONS = setOf("KRV")
    }
}
