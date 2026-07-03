package com.elseeker.android.app.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.graphics.vector.ImageVector
import com.elseeker.android.R

/** 라우트 상수. 딥링크(App Links) 도입 시 이 상수를 기준으로 매핑한다. */
object Routes {
    const val HOME = "home"
    const val BIBLE = "bible"
    const val STUDY = "study"
    const val MY = "my"

    // Bible 하위 라우트 — 웹과 동일한 4단계: 번역본(BIBLE 탭 루트) → 책 → 장 → 절.
    const val BIBLE_BOOKS = "bible/books/{translationId}"
    const val BIBLE_READER = "bible/reader/{translationId}/{bookOrder}/{chapterNumber}"
    const val BIBLE_BOOK_OVERVIEW = "bible/book/{translationId}/{bookOrder}"
    // 책 개요(요약·저자·년도·시대·배경·내용) 전용 화면 — 웹 book-description.html.
    const val BIBLE_BOOK_DESCRIPTION = "bible/description/{translationId}/{bookOrder}"

    fun bibleBooks(translationId: Long) = "bible/books/$translationId"
    fun bibleBookDescription(translationId: Long, bookOrder: Int) =
        "bible/description/$translationId/$bookOrder"
    // keyword 는 홈 인기 검색어 → 검색 화면 프리필용 옵션 인자.
    const val BIBLE_SEARCH = "bible/search?keyword={keyword}"

    fun bibleReader(translationId: Long, bookOrder: Int, chapterNumber: Int) =
        "bible/reader/$translationId/$bookOrder/$chapterNumber"

    fun bibleBookOverview(translationId: Long, bookOrder: Int) =
        "bible/book/$translationId/$bookOrder"

    fun bibleSearch(keyword: String? = null) =
        if (keyword.isNullOrBlank()) "bible/search"
        else "bible/search?keyword=${Uri.encode(keyword)}"

    // 학습 하위 라우트 (학습 탭에서 push)
    const val STUDY_DICTIONARY = "study/dictionary?keyword={keyword}"
    const val STUDY_DICTIONARY_DETAIL = "study/dictionary/{id}"
    const val STUDY_CONTENT = "study/content/{contentKey}"

    fun studyDictionary(keyword: String? = null) =
        if (keyword.isNullOrBlank()) "study/dictionary"
        else "study/dictionary?keyword=${Uri.encode(keyword)}"

    fun studyDictionaryDetail(id: Long) = "study/dictionary/$id"
    fun studyContent(contentKey: String) = "study/content/$contentKey"

    // 지원(1:1 문의) 하위 라우트 (마이 탭에서 push)
    const val SUPPORT_INQUIRIES = "support/inquiries"
    // {id}(숫자) 라우트와의 접두 충돌을 피하려 별도 경로 사용.
    const val SUPPORT_INQUIRY_NEW = "support/inquiry-new"
    const val SUPPORT_INQUIRY_EDIT = "support/inquiry-edit/{id}"
    const val SUPPORT_INQUIRY_DETAIL = "support/inquiries/{id}"

    fun supportInquiryDetail(id: Long) = "support/inquiries/$id"
    fun supportInquiryEdit(id: Long) = "support/inquiry-edit/$id"

    // 마이 하위 라우트 (마이 탭에서 push)
    const val MY_PROFILE_EDIT = "my/profile-edit"
    const val MY_LINKED_ACCOUNTS = "my/linked-accounts"
    const val MY_MEMOS = "my/memos"
}

/** 하단 탭 최상위 목적지. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, R.string.tab_home, Icons.Outlined.Home),
    BIBLE(Routes.BIBLE, R.string.tab_bible, Icons.AutoMirrored.Outlined.MenuBook),
    STUDY(Routes.STUDY, R.string.tab_study, Icons.Outlined.School),
    MY(Routes.MY, R.string.tab_my, Icons.Outlined.Person),
    ;

    /**
     * 현재 라우트가 이 탭 소속인지 — 하단 탭 활성 표시에 사용.
     * 탭 루트(정확히 일치)뿐 아니라 그 탭에서 push 된 평면 하위 라우트(접두사 일치)도 소속으로 본다.
     * 마이 탭은 별도 경로의 1:1 문의(support/…)까지 포함한다.
     */
    fun ownsRoute(route: String?): Boolean {
        if (route == null) return false
        if (route == this.route) return true
        return when (this) {
            HOME -> false
            BIBLE -> route.startsWith("bible/")
            STUDY -> route.startsWith("study/")
            MY -> route.startsWith("my/") || route.startsWith("support/")
        }
    }
}
