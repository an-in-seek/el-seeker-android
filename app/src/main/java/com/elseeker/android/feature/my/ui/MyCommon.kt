package com.elseeker.android.feature.my.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elseeker.android.R

/** 소셜 제공사 와이어 값 → 브랜드 표기명(마이 화면 공용, 웹 연동 계정 카드 파리티). */
@Composable
internal fun providerBrandName(provider: String): String = when (provider.lowercase()) {
    "google" -> stringResource(R.string.my_provider_name_google)
    "kakao" -> stringResource(R.string.my_provider_name_kakao)
    "naver" -> stringResource(R.string.my_provider_name_naver)
    else -> provider
}

/** 소셜 제공사 브랜드 아이콘(로그인 버튼과 동일 자산). 미지원 제공사는 null. */
@DrawableRes
internal fun providerIconRes(provider: String): Int? = when (provider.lowercase()) {
    "google" -> R.drawable.ic_login_google
    "kakao" -> R.drawable.ic_login_kakao
    "naver" -> R.drawable.ic_login_naver
    else -> null
}
