package com.elseeker.android.feature.my.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elseeker.android.R

/** 소셜 제공사 와이어 값을 한국어 레이블로 변환한다. (마이 화면 공용) */
@Composable
internal fun providerLabel(provider: String): String = when (provider.lowercase()) {
    "google" -> stringResource(R.string.my_provider_google)
    "kakao" -> stringResource(R.string.my_provider_kakao)
    "naver" -> stringResource(R.string.my_provider_naver)
    else -> provider
}
