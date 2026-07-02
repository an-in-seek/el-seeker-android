package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.BibleSearchSliceDto.BibleSearchItemDto
import com.elseeker.android.feature.bible.data.KeywordRankingDto.RankingItemDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 성경 절 검색 ViewModel — 노출 번역본(KRV) 내에서 검색 + 인기 검색어 랭킹. */
@HiltViewModel
class BibleSearchViewModel @Inject constructor(
    private val repository: BibleRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _ranking = MutableStateFlow<List<RankingItemDto>>(emptyList())
    val ranking: StateFlow<List<RankingItemDto>> = _ranking.asStateFlow()

    private val _results = MutableStateFlow<UiResource<List<BibleSearchItemDto>>>(
        UiResource.Success(emptyList()),
    )
    val results: StateFlow<UiResource<List<BibleSearchItemDto>>> = _results.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    // 검색 대상 번역본 id — 결과에서 본문으로 이동할 때 필요(검색 결과 DTO엔 translationId 없음).
    private val _translationId = MutableStateFlow<Long?>(null)
    val translationId: StateFlow<Long?> = _translationId.asStateFlow()

    init {
        loadDefaults()
    }

    private fun loadDefaults() {
        viewModelScope.launch {
            repository.visibleTranslations()
                .onSuccess { _translationId.value = it.firstOrNull()?.translationId }
            repository.searchKeywordRanking()
                .onSuccess { _ranking.value = it.items }
        }
    }

    fun onQueryChange(value: String) { _query.value = value }

    fun onKeywordClick(keyword: String) {
        _query.value = keyword
        search()
    }

    fun search() {
        val keyword = _query.value.trim()
        if (keyword.isBlank()) return
        _hasSearched.value = true
        _results.value = UiResource.Loading
        viewModelScope.launch {
            // 초기 로드가 실패했더라도 검색 시점에 번역본을 다시 확보해 그대로 이어서 검색한다.
            var tid = _translationId.value
            if (tid == null) {
                val translations = repository.visibleTranslations().getOrElse {
                    _results.value = it.toUiError()
                    return@launch
                }
                tid = translations.firstOrNull()?.translationId
                _translationId.value = tid
                if (tid == null) {
                    _results.value = UiResource.Error("노출된 번역본이 없습니다.", isNetwork = false)
                    return@launch
                }
            }
            repository.search(tid, keyword)
                .onSuccess { _results.value = UiResource.Success(it.content) }
                .onFailure { _results.value = it.toUiError() }
        }
    }
}
