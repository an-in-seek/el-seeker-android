package com.elseeker.android.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

object CustomTabsHelper {

    private const val TOOLBAR_COLOR = 0xFF4A6FA5.toInt()

    fun launch(context: Context, uri: Uri) {
        val colorScheme = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(TOOLBAR_COLOR)
            .build()

        try {
            CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colorScheme)
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .build()
                .launchUrl(context, uri)
        } catch (_: ActivityNotFoundException) {
            // Custom Tabs 미지원 시 시스템 브라우저로 fallback
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: ActivityNotFoundException) {
                // 브라우저 앱이 없는 경우 무시
            }
        }
    }
}
