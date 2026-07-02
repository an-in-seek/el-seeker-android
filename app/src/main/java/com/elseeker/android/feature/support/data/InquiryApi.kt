package com.elseeker.android.feature.support.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 1:1 문의 Retrofit 계약 (`/api/v1/qna/inquiries`, auth).
 * 로그인 사용자의 문의 작성/내역/상세/수정/삭제를 다룬다(PRD §4.4 / B.1).
 * 수정·삭제는 본인 문의 + RECEIVED(답변 전) 상태에서만 서버가 허용한다.
 */
interface InquiryApi {

    @POST("api/v1/qna/inquiries")
    suspend fun create(@Body body: CreateInquiryRequest): InquiryDetailDto

    @GET("api/v1/qna/inquiries")
    suspend fun myInquiries(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): InquiryPageDto

    @GET("api/v1/qna/inquiries/{id}")
    suspend fun detail(@Path("id") id: Long): InquiryDetailDto

    @PUT("api/v1/qna/inquiries/{id}")
    suspend fun update(@Path("id") id: Long, @Body body: UpdateInquiryRequest): InquiryDetailDto

    @DELETE("api/v1/qna/inquiries/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>
}

@Serializable
data class CreateInquiryRequest(
    val category: String,
    val title: String,
    val content: String,
)

@Serializable
data class UpdateInquiryRequest(
    val category: String,
    val title: String,
    val content: String,
)

@Serializable
data class InquiryPageDto(
    val content: List<InquirySummaryDto> = emptyList(),
    val page: Int = 0,
    val totalElements: Long = 0,
    val hasNext: Boolean = false,
)

@Serializable
data class InquirySummaryDto(
    val id: Long,
    val category: String,
    val title: String,
    val status: String,
    val isAnswered: Boolean = false,
    val createdAt: String? = null,
    val answeredAt: String? = null,
)

@Serializable
data class InquiryDetailDto(
    val id: Long,
    val category: String,
    val title: String,
    val content: String,
    val status: String,
    val isAuthor: Boolean = false,
    val answerContent: String? = null,
    val answeredAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * 문의 카테고리 — 백엔드 InquiryCategory enum name(wire 값).
 * 한국어 표시 라벨은 UI 레이어(InquiryUiLabels.kt)에서 stringResource 로 매핑한다.
 */
enum class InquiryCategory(val wire: String) {
    ACCOUNT("ACCOUNT"),
    CONTENT("CONTENT"),
    GAME("GAME"),
    BUG("BUG"),
    SUGGESTION("SUGGESTION"),
    ETC("ETC"),
}
