package com.elseeker.android.feature.bible.data

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------ *
 * 성경 DTO — 백엔드 BibleApi 계약 정본 기준.
 * Instant 필드(updatedAt 등)는 ISO-8601 문자열로 받는다.
 * highlight color 는 소문자 id("yellow" 등) 와이어 값.
 * ------------------------------------------------------------------ */

@Serializable
data class TranslationDto(
    val translationId: Long,
    val translationType: String,
    val translationName: String,
    val translationLanguage: String? = null,
)

@Serializable
data class BookDto(
    val bookId: Long,
    val bookOrder: Int,
    val bookName: String,
    val abbreviation: String = "",
    val testamentType: String,
    val chapterCount: Int = 0,
)

@Serializable
data class BookDescriptionDto(
    val summary: String = "",
    val author: String = "",
    val writtenYear: String = "",
    val historicalPeriod: String = "",
    val background: String = "",
    val content: String = "",
)

@Serializable
data class BookDetailDto(
    val bookId: Long,
    val bookOrder: Int,
    val bookName: String,
    val abbreviation: String = "",
    val testamentType: String,
    val description: BookDescriptionDto = BookDescriptionDto(),
)

@Serializable
data class ChaptersDto(
    val book: ChaptersBookDto,
) {
    @Serializable
    data class ChaptersBookDto(
        val bookId: Long,
        val bookName: String,
        val abbreviation: String = "",
        val descriptionSummary: String = "",
        val chapters: List<ChapterRefDto> = emptyList(),
    )

    @Serializable
    data class ChapterRefDto(
        val chapterId: Long,
        val chapterNumber: Int,
    )
}

@Serializable
data class VersesDto(
    val book: VersesBookDto,
    val hasPrev: Boolean = false,
    val hasNext: Boolean = false,
    val isFirst: Boolean = false,
    val isLast: Boolean? = null,
) {
    @Serializable
    data class VersesBookDto(
        val bookId: Long,
        val bookOrder: Int,
        val bookName: String,
        val totalChapterCount: Int = 0,
        val chapter: ChapterDetailDto,
    )

    @Serializable
    data class ChapterDetailDto(
        val chapterId: Long,
        val chapterNumber: Int,
        val verses: List<VerseDto> = emptyList(),
    )

    @Serializable
    data class VerseDto(
        val verseId: Long,
        val verseNumber: Int,
        val text: String,
    )
}

@Serializable
data class BibleSearchSliceDto(
    val content: List<BibleSearchItemDto> = emptyList(),
    val hasNext: Boolean = false,
    val totalCount: Long? = null,
) {
    @Serializable
    data class BibleSearchItemDto(
        val bookId: Long,
        val bookOrder: Int,
        val bookName: String,
        val chapterId: Long,
        val chapterNumber: Int,
        val verseId: Long,
        val verseNumber: Int,
        val text: String,
    )
}

@Serializable
data class KeywordRankingDto(
    val items: List<RankingItemDto> = emptyList(),
    val refreshedAt: String? = null,
) {
    @Serializable
    data class RankingItemDto(
        val rank: Int,
        val keyword: String,
        val searchCount: Long = 0,
    )
}

@Serializable
data class DailyVerseDto(
    val translationType: String,
    val translationName: String,
    val bookOrder: Int,
    val bookName: String,
    val chapterNumber: Int,
    val verseNumber: Int,
    val text: String,
)

@Serializable
data class HighlightItemDto(
    val highlightId: Long,
    val verseNumber: Int,
    val color: String,
    val updatedAt: String? = null,
)

@Serializable
data class MemoItemDto(
    val memoId: Long,
    val verseNumber: Int,
    val content: String,
    val updatedAt: String? = null,
)

@Serializable
data class ChapterMemoItemDto(
    val chapterMemoId: Long,
    val content: String,
    val updatedAt: String? = null,
)

@Serializable
data class BookMemoItemDto(
    val bookMemoId: Long,
    val content: String,
    val updatedAt: String? = null,
)

@Serializable
data class ChapterStateDto(
    val memos: List<MemoItemDto> = emptyList(),
    val highlights: List<HighlightItemDto> = emptyList(),
    val isRead: Boolean = false,
    val chapterMemo: ChapterMemoItemDto? = null,
)

@Serializable
data class HighlightRequest(val color: String)

@Serializable
data class MemoRequest(val content: String)

@Serializable
data class ReadingProgressRequest(
    val translationId: Long,
    val bookOrder: Int,
    val chapterNumber: Int,
)

@Serializable
data class ReadingProgressResponse(
    val chapterNumbers: List<Int> = emptyList(),
)
