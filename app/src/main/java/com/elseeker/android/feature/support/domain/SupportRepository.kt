package com.elseeker.android.feature.support.domain

import com.elseeker.android.core.network.orThrow
import com.elseeker.android.core.network.safeApiCall
import com.elseeker.android.feature.support.data.CreateInquiryRequest
import com.elseeker.android.feature.support.data.InquiryApi
import com.elseeker.android.feature.support.data.InquiryDetailDto
import com.elseeker.android.feature.support.data.InquiryPageDto
import com.elseeker.android.feature.support.data.UpdateInquiryRequest
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** 1:1 문의 저장소 — InquiryApi 를 래핑해 Result 로 변환한다. */
@Singleton
class SupportRepository @Inject constructor(
    private val api: InquiryApi,
    private val json: Json,
) {

    suspend fun myInquiries(): Result<InquiryPageDto> =
        runCatching { safeApiCall(json) { api.myInquiries() } }

    suspend fun detail(id: Long): Result<InquiryDetailDto> =
        runCatching { safeApiCall(json) { api.detail(id) } }

    suspend fun create(category: String, title: String, content: String): Result<InquiryDetailDto> =
        runCatching {
            safeApiCall(json) { api.create(CreateInquiryRequest(category, title, content)) }
        }

    suspend fun update(
        id: Long,
        category: String,
        title: String,
        content: String,
    ): Result<InquiryDetailDto> = runCatching {
        safeApiCall(json) { api.update(id, UpdateInquiryRequest(category, title, content)) }
    }

    suspend fun delete(id: Long): Result<Unit> =
        runCatching { safeApiCall(json) { api.delete(id).orThrow() } }
}
