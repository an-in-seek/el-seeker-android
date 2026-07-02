package com.elseeker.android.feature.bible.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 내 메모 모아보기 Retrofit 계약 (auth).
 * v1 은 절 메모 목록(`/my-memos`) + 3탭 카운트(`/my-memo-counts`)를 다룬다.
 * 장/책 메모 목록(`/my-chapter-memos`, `/my-book-memos`)은 후속 확장.
 */
interface BibleMyMemoApi {

    @GET("api/v1/bibles/my-memos")
    suspend fun myVerseMemos(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("translationId") translationId: Long? = null,
        @Query("bookOrder") bookOrder: Int? = null,
    ): MemoSliceDto

    @GET("api/v1/bibles/my-memo-counts")
    suspend fun myMemoCounts(): MemoCountsDto
}

@Serializable
data class MemoSliceDto(
    val content: List<MyMemoItemDto> = emptyList(),
    val hasNext: Boolean = false,
    val size: Int = 0,
    val number: Int = 0,
    val totalCount: Long? = null,
)

/** 내 메모 목록 항목. 절 메모 편집용 [MemoItemDto] 와 달리 책/장 위치 메타를 포함한다. */
@Serializable
data class MyMemoItemDto(
    val memoId: Long,
    val translationId: Long,
    val bookOrder: Int,
    val bookName: String = "",
    val chapterNumber: Int,
    val verseNumber: Int,
    val content: String,
    val updatedAt: String? = null,
)

@Serializable
data class MemoCountsDto(
    val book: Long = 0,
    val chapter: Long = 0,
    val verse: Long = 0,
)
