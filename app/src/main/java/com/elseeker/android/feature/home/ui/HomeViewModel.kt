package com.elseeker.android.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.feature.bible.data.KeywordRankingDto.RankingItemDto
import com.elseeker.android.feature.bible.domain.BibleRepository
import com.elseeker.android.feature.study.data.DictionaryRankingItemDto
import com.elseeker.android.feature.study.data.DictionaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 홈 탭 ViewModel — 웹 index.html 과 동일하게 인기 검색어 2종(구절/사전)을 로드한다.
 * 웹이 로드 전 카드를 hidden 처리하듯, 실패/빈 목록이면 카드 자체를 숨긴다(홈을 막지 않음).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bibleRepository: BibleRepository,
    private val dictionaryRepository: DictionaryRepository,
) : ViewModel() {

    private val _bibleRanking = MutableStateFlow<List<RankingItemDto>>(emptyList())
    val bibleRanking: StateFlow<List<RankingItemDto>> = _bibleRanking.asStateFlow()

    private val _dictionaryRanking = MutableStateFlow<List<DictionaryRankingItemDto>>(emptyList())
    val dictionaryRanking: StateFlow<List<DictionaryRankingItemDto>> = _dictionaryRanking.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            bibleRepository.searchKeywordRanking()
                .onSuccess { _bibleRanking.value = it.items }
        }
        viewModelScope.launch {
            dictionaryRepository.ranking()
                .onSuccess { _dictionaryRanking.value = it }
        }
    }
}
