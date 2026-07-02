package com.elseeker.android.feature.study.ui

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
) : ViewModel() {

    private val _state = MutableStateFlow<UiResource<List<DictionaryItemDto>>>(UiResource.Loading)
    val state: StateFlow<UiResource<List<DictionaryItemDto>>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // 인기 검색어 — 실패해도 목록 화면을 막지 않도록 별도 상태로 관리한다.
    private val _ranking = MutableStateFlow<List<DictionaryRankingItemDto>>(emptyList())
    val ranking: StateFlow<List<DictionaryRankingItemDto>> = _ranking.asStateFlow()

    init {
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

    /** 현재 query 로 사전 목록을 로드한다. 공백이면 null 을 전달해 전체를 조회한다. */
    fun search() {
        _state.value = UiResource.Loading
        val keyword = _query.value.trim().ifBlank { null }
        viewModelScope.launch {
            repository.list(keyword = keyword)
                .onSuccess { _state.value = UiResource.Success(it.content) }
                .onFailure { _state.value = it.toUiError() }
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
