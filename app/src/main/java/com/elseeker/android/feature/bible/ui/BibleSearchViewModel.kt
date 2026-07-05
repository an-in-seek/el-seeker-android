package com.elseeker.android.feature.bible.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.core.ui.UiResource
import com.elseeker.android.core.ui.toUiError
import com.elseeker.android.feature.bible.data.BibleReaderPrefs
import com.elseeker.android.feature.bible.data.BibleSearchSliceDto.BibleSearchItemDto
import com.elseeker.android.feature.bible.data.KeywordRankingDto.RankingItemDto
import com.elseeker.android.feature.bible.data.TranslationDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 성경 절 검색 ViewModel — 번역본 선택(최근 선택 복원) + 검색 + 인기 검색어 랭킹. */
@HiltViewModel
class BibleSearchViewModel @Inject constructor(
    private val repository: BibleRepository,
    private val readerPrefs: BibleReaderPrefs,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // 홈 인기 검색어 등에서 전달된 초기 검색어(옵션).
    private val initialKeyword: String? = savedStateHandle.get<String>("keyword")

    /** 프리필 키워드로 진입했는지 — 진입 시 검색창 자동 포커스 여부 판단에 사용(프리필이면 결과 위주라 포커스/키보드 생략). */
    val hasPrefillKeyword: Boolean = !initialKeyword.isNullOrBlank()

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

    // 선택 가능한 번역본 목록 + 현재 선택(검색 대상). 결과에서 본문으로 이동할 때도 이 id를 쓴다.
    private val _translations = MutableStateFlow<List<TranslationDto>>(emptyList())
    val translations: StateFlow<List<TranslationDto>> = _translations.asStateFlow()

    private val _selectedTranslation = MutableStateFlow<TranslationDto?>(null)
    val selectedTranslation: StateFlow<TranslationDto?> = _selectedTranslation.asStateFlow()

    // 마지막 검색의 결과 총 건수와 그 검색어(입력이 바뀌어도 헤더는 마지막 검색 기준 유지).
    private val _resultCount = MutableStateFlow<Long?>(null)
    val resultCount: StateFlow<Long?> = _resultCount.asStateFlow()

    private val _searchedKeyword = MutableStateFlow("")
    val searchedKeyword: StateFlow<String> = _searchedKeyword.asStateFlow()

    init {
        loadDefaults()
    }

    private fun loadDefaults() {
        viewModelScope.launch {
            // 번역본 전체 목록을 확보하고, 최근 선택(로컬 저장) → 노출 기본본 → 첫 항목 순으로 기본 선택.
            val list = repository.translations().getOrNull().orEmpty()
            _translations.value = list
            _selectedTranslation.value = resolveDefaultTranslation(list)

            repository.searchKeywordRanking()
                .onSuccess { _ranking.value = it.items }

            // 프리필 키워드가 있으면 즉시 검색(웹 /web/bible/search?keyword= 과 동일).
            if (!initialKeyword.isNullOrBlank()) {
                _query.value = initialKeyword
                search()
            }
        }
    }

    /** 최근 선택한 번역본을 우선 복원하고, 없으면 노출 기본본(KRV), 그것도 없으면 목록 첫 항목. */
    private suspend fun resolveDefaultTranslation(list: List<TranslationDto>): TranslationDto? {
        val savedId = readerPrefs.loadSelectedTranslationId()
        return list.firstOrNull { it.translationId == savedId }
            ?: repository.visibleTranslations().getOrNull()?.firstOrNull()
            ?: list.firstOrNull()
    }

    fun onQueryChange(value: String) { _query.value = value }

    fun onKeywordClick(keyword: String) {
        _query.value = keyword
        search()
    }

    /** 번역본 선택 변경 — 로컬에 저장하고, 이미 검색 중이면 새 번역본으로 재검색한다. */
    fun onTranslationSelected(translation: TranslationDto) {
        if (translation.translationId == _selectedTranslation.value?.translationId) return
        _selectedTranslation.value = translation
        readerPrefs.saveSelectedTranslationId(translation.translationId)
        if (_hasSearched.value && _query.value.isNotBlank()) search()
    }

    fun search() {
        val keyword = _query.value.trim()
        if (keyword.isBlank()) return
        _hasSearched.value = true
        _results.value = UiResource.Loading
        viewModelScope.launch {
            // 초기 로드가 실패했더라도 검색 시점에 번역본을 다시 확보해 그대로 이어서 검색한다.
            var tid = _selectedTranslation.value?.translationId
            if (tid == null) {
                val list = repository.translations().getOrNull().orEmpty()
                if (list.isNotEmpty()) _translations.value = list
                val selected = resolveDefaultTranslation(list)
                _selectedTranslation.value = selected
                tid = selected?.translationId
                if (tid == null) {
                    _results.value = UiResource.Error("노출된 번역본이 없습니다.", isNetwork = false)
                    return@launch
                }
            }
            repository.search(tid, keyword)
                .onSuccess {
                    _results.value = UiResource.Success(it.content)
                    _resultCount.value = it.totalCount ?: it.content.size.toLong()
                    _searchedKeyword.value = keyword
                }
                .onFailure { _results.value = it.toUiError() }
        }
    }
}
