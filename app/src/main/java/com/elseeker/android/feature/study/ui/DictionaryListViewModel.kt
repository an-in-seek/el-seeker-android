package com.elseeker.android.feature.study.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.study.data.DictionaryItemDto
import com.elseeker.android.feature.study.data.DictionaryRankingItemDto
import com.elseeker.android.feature.study.data.DictionaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 사전 목록 ViewModel — 검색어 관리, 목록 로드, 인기 검색어 랭킹 로드. */
@HiltViewModel
class DictionaryListViewModel @Inject constructor(
    private val repository: DictionaryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // 홈 인기 검색어 등에서 전달된 초기 검색어(옵션).
    private val initialKeyword: String? = savedStateHandle.get<String>("keyword")

    private val _state = MutableStateFlow<UiResource<List<DictionaryItemDto>>>(UiResource.Loading)
    val state: StateFlow<UiResource<List<DictionaryItemDto>>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // 인기 검색어 — 실패해도 목록 화면을 막지 않도록 별도 상태로 관리한다.
    private val _ranking = MutableStateFlow<List<DictionaryRankingItemDto>>(emptyList())
    val ranking: StateFlow<List<DictionaryRankingItemDto>> = _ranking.asStateFlow()

    // 전체 건수("성경 사전 N건")와 페이지네이션 상태.
    private val _totalCount = MutableStateFlow<Long?>(null)
    val totalCount: StateFlow<Long?> = _totalCount.asStateFlow()

    private var currentKeyword: String? = null
    private var page = 0
    private var hasNext = false
    private var loadingMore = false

    init {
        // 프리필 키워드가 있으면 그 검색어로 시작(웹 /web/study/dictionary?keyword= 과 동일).
        if (!initialKeyword.isNullOrBlank()) _query.value = initialKeyword
        search()
        loadRanking()
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** query 로 인기 검색어를 대입하고 즉시 검색한다. */
    fun onKeywordClick(keyword: String) {
        _query.value = keyword
        search()
    }

    /** 현재 query 로 사전 목록 첫 페이지를 로드한다. 공백이면 null 을 전달해 전체를 조회한다. */
    fun search() {
        _state.value = UiResource.Loading
        page = 0
        currentKeyword = _query.value.trim().ifBlank { null }
        viewModelScope.launch {
            repository.list(keyword = currentKeyword, page = 0)
                .onSuccess {
                    _totalCount.value = it.totalCount
                    hasNext = it.hasNext
                    _state.value = UiResource.Success(it.content)
                }
                .onFailure { _state.value = it.toUiError() }
        }
    }

    /** 스크롤이 목록 끝에 가까워지면 다음 페이지를 이어 붙인다(무한 스크롤). */
    fun loadMore() {
        val current = _state.value as? UiResource.Success ?: return
        if (!hasNext || loadingMore) return
        loadingMore = true
        viewModelScope.launch {
            repository.list(keyword = currentKeyword, page = page + 1)
                .onSuccess {
                    page += 1
                    hasNext = it.hasNext
                    _state.value = UiResource.Success(current.data + it.content)
                }
            loadingMore = false
        }
    }

    private fun loadRanking() {
        viewModelScope.launch {
            repository.ranking()
                .onSuccess { _ranking.value = it }
                .onFailure { /* 랭킹은 부가 정보 — 실패해도 무시 */ }
        }
    }
}
