package com.elseeker.android.feature.bible.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 성경 Retrofit 계약.
 *
 * ⚠️ 주의:
 * - 목록/본문은 base `/api/v1/bibles` (복수형, public).
 * - 읽기 진도는 base `/api/v1/bible/reading` (단수형, auth) — 혼동 금지.
 * - navigate 의 direction 은 대문자 PREV/NEXT 만 허용(소문자는 400).
 */
interface BibleApi {

    @GET("api/v1/bibles/translations")
    suspend fun translations(): List<TranslationDto>

    @GET("api/v1/bibles/translations/{translationId}/books")
    suspend fun books(@Path("translationId") translationId: Long): List<BookDto>

    @GET("api/v1/bibles/translations/{translationId}/books/{bookOrder}")
    suspend fun bookDetail(
        @Path("translationId") translationId: Long,
        @Path("bookOrder") bookOrder: Int,
    ): BookDetailDto

    @GET("api/v1/bibles/translations/{translationId}/books/{bookOrder}/chapters")
    suspend fun chapters(
        @Path("translationId") translationId: Long,
        @Path("bookOrder") bookOrder: Int,
    ): ChaptersDto

    @GET("api/v1/bibles/translations/{translationId}/books/{bookOrder}/chapters/{chapterNumber}/verses")
    suspend fun verses(
        @Path("translationId") translationId: Long,
        @Path("bookOrder") bookOrder: Int,
        @Path("chapterNumber") chapterNumber: Int,
    ): VersesDto

    /** direction 은 반드시 "PREV" 또는 "NEXT" (대문자). */
    @GET("api/v1/bibles/translations/{translationId}/books/{bookOrder}/chapters/{chapterNumber}/navigate")
    suspend fun navigate(
        @Path("translationId") translationId: Long,
        @Path("bookOrder") bookOrder: Int,
        @Path("chapterNumber") chapterNumber: Int,
        @Query("direction") direction: String,
    ): VersesDto

    @GET("api/v1/bibles/translations/{translationId}/search")
    suspend fun search(
        @Path("translationId") translationId: Long,
        @Query("keyword") keyword: String,
        @Query("bookOrder") bookOrder: Int? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("track") track: Boolean = true,
    ): BibleSearchSliceDto

    @GET("api/v1/bibles/search-keywords/ranking")
    suspend fun searchKeywordRanking(@Query("limit") limit: Int = 10): KeywordRankingDto

    @GET("api/v1/bibles/daily")
    suspend fun dailyVerse(@Query("translationType") translationType: String = "KRV"): DailyVerseDto

    // --- 인증 필요: 장 상태/하이라이트/메모 -------------------------------

    @GET("api/v1/bibles/translations/{translationId}/books/{bookOrder}/chapters/{chapterNumber}/state")
    suspend fun chapterState(
        @Path("translationId") translationId: Long,
        @Path("bookOrder") bookOrder: Int,
        @Path("chapterNumber") chapterNumber: Int,
    ): ChapterStateDto

    @PUT("api/v1/bibles/translations/{translationId}/books/{bookOrder}/chapters/{chapterNumber}/verses/{verseNumber}/highlight")
    suspend fun putHighlight(
        @Path("translationId") translationId: Long,
        @Path("bookOrder") bookOrder: Int,
        @Path("chapterNumber") chapterNumber: Int,
        @Path("verseNumber") verseNumber: Int,
        @Body body: HighlightRequest,
    ): HighlightItemDto

    @DELETE("api/v1/bibles/translations/{translationId}/books/{bookOrder}/chapters/{chapterNumber}/verses/{verseNumber}/highlight")
    suspend fun deleteHighlight(
        @Path("translationId") translationId: Long,
        @Path("bookOrder") bookOrder: Int,
        @Path("chapterNumber") chapterNumber: Int,
        @Path("verseNumber") verseNumber: Int,
    ): Response<Unit>

    @PUT("api/v1/bibles/translations/{translationId}/books/{bookOrder}/chapters/{chapterNumber}/verses/{verseNumber}/memo")
    suspend fun putVerseMemo(
        @Path("translationId") translationId: Long,
        @Path("bookOrder") bookOrder: Int,
        @Path("chapterNumber") chapterNumber: Int,
        @Path("verseNumber") verseNumber: Int,
        @Body body: MemoRequest,
    ): MemoItemDto

    @DELETE("api/v1/bibles/translations/{translationId}/books/{bookOrder}/chapters/{chapterNumber}/verses/{verseNumber}/memo")
    suspend fun deleteVerseMemo(
        @Path("translationId") translationId: Long,
        @Path("bookOrder") bookOrder: Int,
        @Path("chapterNumber") chapterNumber: Int,
        @Path("verseNumber") verseNumber: Int,
    ): Response<Unit>

    // --- 인증 필요: 읽기 진도 (단수형 base) -------------------------------

    @POST("api/v1/bible/reading/chapters/read")
    suspend fun markChapterRead(@Body body: ReadingProgressRequest): Response<Unit>

    @GET("api/v1/bible/reading/chapters/read")
    suspend fun readChapters(
        @Query("translationId") translationId: Long,
        @Query("bookOrder") bookOrder: Int,
    ): ReadingProgressResponse
}
