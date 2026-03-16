package com.elseeker.android

import android.app.Application
import android.webkit.WebView

class ElSeekerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
