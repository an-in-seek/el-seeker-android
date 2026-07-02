package com.elseeker.android.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * 외부 웹 문서(약관·개인정보처리방침, 외부 링크 등)를 Chrome Custom Tabs 로 열고,
 * Custom Tabs 미지원 기기는 시스템 브라우저로 폴백한다(PRD §4-A 외부 위임 / §6).
 */
fun openExternalUrl(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            // 브라우저가 전혀 없는 기기 — 열 수 없음(극히 드묾, 무시).
        }
    }
}
