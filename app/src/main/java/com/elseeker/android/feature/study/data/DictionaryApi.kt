package com.elseeker.android.feature.study.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** 학습 사전 Retrofit 계약 (`/api/v1/study/dictionaries`, public). */
interface DictionaryApi {

    @GET("api/v1/study/dictionaries")
    suspend fun list(
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("track") track: Boolean = true,
    ): DictionarySliceDto

    @GET("api/v1/study/dictionaries/{id}")
    suspend fun detail(@Path("id") id: Long): DictionaryDetailDto

    @GET("api/v1/study/dictionaries/{id}/references")
    suspend fun references(@Path("id") id: Long): List<DictionaryReferenceDto>

    @GET("api/v1/study/dictionaries/search-keywords/ranking")
    suspend fun searchKeywordRanking(
        @Query("limit") limit: Int = 10,
    ): DictionaryRankingDto
}

@Serializable
data class DictionarySliceDto(
    val content: List<DictionaryItemDto> = emptyList(),
    val hasNext: Boolean = false,
    val totalCount: Long? = null,
)

@Serializable
data class DictionaryItemDto(
    val id: Long,
    val term: String,
    val description: String? = null,
)

@Serializable
data class DictionaryDetailDto(
    val id: Long,
    val term: String,
    val description: String? = null,
)

@Serializable
data class DictionaryReferenceDto(
    val referenceId: Long,
    val bookOrder: Int,
    val chapterNumber: Int,
    val verseNumber: Int,
    val verseLabel: String,
    val displayOrder: Int,
)

@Serializable
data class DictionaryRankingDto(
    val items: List<DictionaryRankingItemDto> = emptyList(),
    val refreshedAt: String? = null,
)

@Serializable
data class DictionaryRankingItemDto(
    val rank: Int,
    val keyword: String,
    val searchCount: Long = 0,
)
