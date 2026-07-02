package com.elseeker.android.feature.support.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elseeker.android.R
import com.elseeker.android.feature.support.data.InquiryCategory

/**
 * 문의 카테고리/상태의 한국어 표시 라벨(UI 레이어). data 계층의 wire 값·label 은 변경하지 않고
 * 화면에서 이 함수들로 stringResource 기반 표시 문자열을 얻는다.
 */
@Composable
fun InquiryCategory.displayLabel(): String = when (this) {
    InquiryCategory.ACCOUNT -> stringResource(R.string.support_category_account)
    InquiryCategory.CONTENT -> stringResource(R.string.support_category_content)
    InquiryCategory.GAME -> stringResource(R.string.support_category_game)
    InquiryCategory.BUG -> stringResource(R.string.support_category_bug)
    InquiryCategory.SUGGESTION -> stringResource(R.string.support_category_suggestion)
    InquiryCategory.ETC -> stringResource(R.string.support_category_etc)
}

@Composable
fun inquiryCategoryDisplayLabel(wire: String): String {
    val category = InquiryCategory.entries.firstOrNull { it.wire == wire }
    return category?.displayLabel() ?: wire
}

@Composable
fun inquiryStatusDisplayLabel(status: String): String = when (status) {
    "RECEIVED" -> stringResource(R.string.support_status_received)
    "ANSWERED" -> stringResource(R.string.support_status_answered)
    "CLOSED" -> stringResource(R.string.support_status_closed)
    else -> status
}
