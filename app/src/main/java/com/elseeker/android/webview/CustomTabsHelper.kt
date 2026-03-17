package com.elseeker.android.webview

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

object CustomTabsHelper {

    private const val TOOLBAR_COLOR = 0xFF4A6FA5.toInt()

    fun launch(context: Context, uri: Uri) {
        val colorScheme = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(TOOLBAR_COLOR)
            .build()

        CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorScheme)
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .build()
            .launchUrl(context, uri)
    }
}
