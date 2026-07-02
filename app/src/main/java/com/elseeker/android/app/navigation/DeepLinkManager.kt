package com.elseeker.android.app.navigation

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App Links(딥링크)로 들어온 URL 을 내부 라우트로 변환해 보관한다(PRD §6 ★).
 *
 * 미인증 상태에서 들어온 딥링크도 잃지 않도록 [pendingRoute] 로 보류해 두고,
 * 인증 완료 후 [MainScaffold] 가 1회 소비([consume])해 네비게이션한다.
 */
@Singleton
class DeepLinkManager @Inject constructor() {

    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    /**
     * elseeker.com 경로를 내부 라우트로 매핑한다. 지원하지 않는 경로는 무시한다.
     * 예: https://elseeker.com/bible/{translationId}/{bookOrder}/{chapter} → 성경 본문 뷰어
     */
    fun onUri(uri: Uri?) {
        val route = uri?.let(::toRoute) ?: return
        _pendingRoute.value = route
    }

    fun consume() {
        _pendingRoute.value = null
    }

    private fun toRoute(uri: Uri): String? {
        if (!uri.host.orEmpty().endsWith(HOST)) return null
        val segments = uri.pathSegments
        if (segments.isEmpty()) return null
        return when (segments[0]) {
            "bible" -> {
                // /bible/{translationId}/{bookOrder}/{chapter}
                val translationId = segments.getOrNull(1)?.toLongOrNull()
                val bookOrder = segments.getOrNull(2)?.toIntOrNull()
                val chapter = segments.getOrNull(3)?.toIntOrNull() ?: 1
                if (translationId != null && bookOrder != null) {
                    Routes.bibleReader(translationId, bookOrder, chapter)
                } else {
                    Routes.BIBLE
                }
            }
            "study" -> Routes.STUDY
            else -> null
        }
    }

    private companion object {
        const val HOST = "elseeker.com"
    }
}
