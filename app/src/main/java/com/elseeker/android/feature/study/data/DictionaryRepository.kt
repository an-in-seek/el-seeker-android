package com.elseeker.android.feature.study.data

import com.elseeker.android.core.network.safeApiCall
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** 학습 사전 저장소 — DictionaryApi 를 래핑해 Result 로 변환한다. */
@Singleton
class DictionaryRepository @Inject constructor(
    private val api: DictionaryApi,
    private val json: Json,
) {

    /** keyword 가 null 또는 공백이면 전체 목록을 반환한다. */
    suspend fun list(keyword: String?): Result<DictionarySliceDto> =
        runCatching { safeApiCall(json) { api.list(keyword = keyword) } }

    /** 사전 상세 + 참조 목록을 함께 조회한다. 상세는 필수, 참조는 실패 시 빈 목록으로 폴백한다. */
    suspend fun detail(id: Long): Result<DictionaryDetailWithRefs> = runCatching {
        val detail = safeApiCall(json) { api.detail(id) }
        val refs = runCatching { safeApiCall(json) { api.references(id) } }.getOrDefault(emptyList())
        DictionaryDetailWithRefs(detail = detail, references = refs)
    }

    /** 인기 검색어 랭킹. */
    suspend fun ranking(limit: Int = 10): Result<List<DictionaryRankingItemDto>> =
        runCatching { safeApiCall(json) { api.searchKeywordRanking(limit) }.items }
}

/** 사전 상세 화면용 묶음 모델. */
data class DictionaryDetailWithRefs(
    val detail: DictionaryDetailDto,
    val references: List<DictionaryReferenceDto>,
)
