package com.elseeker.android.feature.bible.domain

import com.elseeker.android.core.network.safeApiCall
import com.elseeker.android.feature.bible.data.BibleMyMemoApi
import com.elseeker.android.feature.bible.data.MemoCountsDto
import com.elseeker.android.feature.bible.data.MyMemoItemDto
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** 내 메모 모아보기 저장소. 절 메모 목록 + 카운트를 함께 로드한다. */
@Singleton
class MyMemoRepository @Inject constructor(
    private val api: BibleMyMemoApi,
    private val json: Json,
) {

    /** 절 메모 목록 + 3탭 카운트. 카운트는 실패 시 0 으로 폴백해 목록을 막지 않는다. */
    suspend fun load(): Result<MyMemos> = runCatching {
        val slice = safeApiCall(json) { api.myVerseMemos() }
        val counts = runCatching { safeApiCall(json) { api.myMemoCounts() } }
            .getOrDefault(MemoCountsDto())
        MyMemos(verseMemos = slice.content, counts = counts)
    }
}

data class MyMemos(
    val verseMemos: List<MyMemoItemDto>,
    val counts: MemoCountsDto,
)
