package com.elseeker.android.app

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elseeker.android.BuildConfig
import com.elseeker.android.R
import com.elseeker.android.core.ui.openExternalUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elseeker.android.app.navigation.Routes
import com.elseeker.android.app.navigation.TopLevelDestination
import com.elseeker.android.core.auth.AuthState
import com.elseeker.android.core.ui.LoadingBox
import com.elseeker.android.feature.auth.ui.LoginScreen
import com.elseeker.android.feature.bible.ui.BibleBookOverviewScreen
import com.elseeker.android.feature.bible.ui.BibleBooksScreen
import com.elseeker.android.feature.bible.ui.TranslationListScreen
import com.elseeker.android.feature.bible.ui.BibleReaderScreen
import com.elseeker.android.feature.bible.ui.BookDescriptionScreen
import com.elseeker.android.feature.bible.ui.BibleSearchScreen
import com.elseeker.android.feature.bible.ui.MyMemosScreen
import com.elseeker.android.feature.home.ui.HomeScreen
import com.elseeker.android.feature.my.ui.MyScreen
import com.elseeker.android.feature.study.ui.DictionaryDetailScreen
import com.elseeker.android.feature.study.ui.DictionaryListScreen
import com.elseeker.android.feature.study.ui.StudyScreen
import com.elseeker.android.feature.study.ui.content.CreationExperienceScreen
import com.elseeker.android.feature.study.ui.content.StaticContentScreen
import com.elseeker.android.feature.support.ui.InquiryComposeScreen
import com.elseeker.android.feature.support.ui.InquiryDetailScreen
import com.elseeker.android.feature.support.ui.InquiryListScreen
import com.elseeker.android.ui.screen.OfflineScreen

/**
 * 메인 셸(게스트 포함). 하단 탭(홈/성경/학습/마이) + 내부 NavHost.
 * 웹과 동일하게 홈/성경/학습은 비로그인 탐색 가능(공개 API)하고,
 * 마이 탭은 [authState] 에 따라 로그인 화면(게스트)/오프라인 재시도/마이 화면으로 분기한다.
 * 성경 본문 뷰어는 탭 위에 push 되는 하위 라우트라 하단 탭을 숨긴다.
 */
@Composable
fun MainScaffold(
    authState: AuthState,
    loginBusy: Boolean,
    onSocialLogin: (provider: String) -> Unit,
    onRetrySession: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    pendingDeepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevelRoute = TopLevelDestination.entries.any { it.route == currentRoute }

    // 프로필 아이콘 → 계정 메뉴 바텀시트(웹 account-menu 파리티).
    var showAccountSheet by remember { mutableStateOf(false) }
    val themeViewModel: ThemeViewModel = hiltViewModel()

    // 성경 개요/본문 화면은 하단 탭을 기본 노출하되 스크롤 방향에 따라 숨김/표시한다(웹 bottom-tab-hidden 파리티).
    var bibleChromeVisible by remember { mutableStateOf(true) }
    LaunchedEffect(currentRoute) { bibleChromeVisible = true }
    val bibleChromeRoutes = setOf(
        Routes.BIBLE_BOOK_OVERVIEW,
        Routes.BIBLE_BOOK_DESCRIPTION,
        Routes.BIBLE_READER,
    )
    // 성경 브라우징·검색 진입부(번역본 목록·책 목록·구절 검색)는 몰입형이라 하단 탭을 항상 숨긴다.
    val hideBottomBarRoutes = setOf(
        Routes.BIBLE,
        Routes.BIBLE_BOOKS,
        Routes.BIBLE_SEARCH,
    )
    // 마이 탭이지만 게스트라 로그인 화면이 뜨는 경우엔 하단 탭을 숨긴다(웹 로그인 파리티).
    val isGuestLoginScreen = currentRoute == Routes.MY &&
        authState != AuthState.Authenticated && authState != AuthState.Offline
    val showBottomBar = (isTopLevelRoute ||
        (currentRoute in bibleChromeRoutes && bibleChromeVisible)) &&
        !isGuestLoginScreen &&
        currentRoute !in hideBottomBarRoutes

    // App Links 로 보류된 라우트를 인증 완료(이 화면 진입) 후 1회 네비게이션한다.
    LaunchedEffect(pendingDeepLink) {
        val route = pendingDeepLink ?: return@LaunchedEffect
        navController.navigate(route)
        onDeepLinkConsumed()
    }

    // 시스템 뒤로가기(PRD §6 ★): 최상위 탭 중 홈이 아니면 홈으로, 홈에서 한 번 더 누르면 종료.
    val context = LocalContext.current
    val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
    var backPressedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = isTopLevelRoute) {
        if (currentRoute != Routes.HOME) {
            navController.navigate(Routes.HOME) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else if (backPressedOnce) {
            (context as? Activity)?.finish()
        } else {
            backPressedOnce = true
            Toast.makeText(context, context.getString(R.string.back_press_exit), Toast.LENGTH_SHORT).show()
            scope.launch {
                delay(2000)
                backPressedOnce = false
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    AppBottomTabBar(
                        tabs = bottomTabs,
                        // 하위 화면(bible/books/…, study/…)은 평면 라우트라
                        // hierarchy 매칭으로는 소속 탭이 활성되지 않는다 — 라우트 소유 탭으로 판정한다.
                        isSelected = { dest -> dest.ownsRoute(currentRoute) },
                        onNativeClick = { dest ->
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        // 게임·커뮤니티는 v1 네이티브 범위 밖 — 웹 화면을 Custom Tabs 로 위임한다.
                        onExternalClick = { path -> openExternalUrl(context, "$baseUrl$path") },
                    )
                }
                // 탭이 숨겨져도(0 높이 placeable) 제스처 내비 인셋만큼은 확보한다 —
                // Scaffold 는 bottomBar 슬롯이 비어있지 않으면 인셋 폴백을 적용하지 않기 때문.
                if (!showBottomBar) {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        },
    ) { inner ->
        // 최상위 탭이면 탭 전환(상태 저장/복원), 아니면 일반 push.
        val navigateRoute: (String) -> Unit = { route ->
            val isTopLevel = TopLevelDestination.entries.any { it.route == route }
            navController.navigate(route) {
                if (isTopLevel) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        // 성경 4단계 화면 공용 상단바 콜백(스크린샷 파리티 — docs/view/*.jpg).
        val openBibleSearch: () -> Unit = { navController.navigate(Routes.bibleSearch()) }
        // 프로필 아이콘 → 계정 메뉴 바텀시트(웹 account-menu 파리티).
        val openAccountSheet: () -> Unit = { showAccountSheet = true }
        // KRV ▼ 칩: 번역본 목록을 현재 화면 위에 push 한다(백스택 보존).
        // 스택을 걷어내면 back 이 직전 성경 화면이 아닌 홈으로 붕괴되므로, 역순 내비게이션(모범사례)을 위해 push 한다.
        val openTranslationList: () -> Unit = {
            navController.navigate(Routes.BIBLE) { launchSingleTop = true }
        }

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            composable(Routes.HOME) {
                HomeScreen(onNavigate = navigateRoute, onProfileClick = openAccountSheet)
            }
            // 성경 탭 루트 = 번역본 목록(웹 /web/bible/translation 과 동일).
            composable(Routes.BIBLE) {
                TranslationListScreen(
                    onTranslationClick = { translationId ->
                        navController.navigate(Routes.bibleBooks(translationId))
                    },
                    // back 은 백스택을 한 단계 되돌려 직전 화면으로 복귀한다(모범사례 Up 내비게이션).
                    onBack = { navController.navigateUp() },
                    onProfileClick = openAccountSheet,
                )
            }
            composable(
                route = Routes.BIBLE_BOOKS,
                arguments = listOf(navArgument("translationId") { type = NavType.StringType }),
            ) {
                BibleBooksScreen(
                    onBookClick = { translationId, bookOrder ->
                        navController.navigate(Routes.bibleBookOverview(translationId, bookOrder))
                    },
                    onBack = { navController.popBackStack() },
                    onChangeTranslation = openTranslationList,
                    onSearchClick = openBibleSearch,
                    onProfileClick = openAccountSheet,
                    // 화면 내 스크롤 방향 → 하단 탭 숨김/표시 연동.
                    onChromeVisibleChange = { bibleChromeVisible = it },
                )
            }
            composable(Routes.STUDY) {
                StudyScreen(
                    onOpenDictionary = { navController.navigate(Routes.studyDictionary()) },
                    onOpenContent = { key -> navController.navigate(Routes.studyContent(key)) },
                    onProfileClick = openAccountSheet,
                )
            }
            composable(Routes.MY) {
                // 게스트로 로그인 화면에 진입한 뒤 로그인 성공(→Authenticated)하면
                // 이전 화면으로 복귀한다(백스택에 없으면 홈). MyScreen 은 표시하지 않는다.
                val cameForLogin = remember { authState != AuthState.Authenticated }
                LaunchedEffect(authState) {
                    if (cameForLogin && authState == AuthState.Authenticated) {
                        if (!navController.popBackStack()) navigateRoute(Routes.HOME)
                    }
                }
                when (authState) {
                    // 게스트 로그인 성공 직후: 복귀 처리 중이라 MyScreen 깜빡임 없이 로딩만 표시.
                    AuthState.Authenticated ->
                        if (cameForLogin) {
                            LoadingBox()
                        } else {
                            MyScreen(
                                onLoggedOut = onLoggedOut,
                                onOpenInquiries = { navController.navigate(Routes.SUPPORT_INQUIRIES) },
                                onOpenMyMemos = { navController.navigate(Routes.MY_MEMOS) },
                            )
                        }
                    // 토큰은 있으나 세션 복원이 네트워크로 보류된 상태 — 로그인 화면 대신 재시도.
                    AuthState.Offline -> OfflineScreen(onRetry = onRetrySession)
                    // 게스트: 웹 /web/auth/login 과 동일한 로그인 화면. '둘러보기' → 홈 탭.
                    else -> LoginScreen(
                        busy = loginBusy,
                        onSocialLogin = onSocialLogin,
                        onBrowse = { navigateRoute(Routes.HOME) },
                    )
                }
            }

            // 마이 하위 화면(프로필 수정·연동 계정은 마이 화면에 인라인 통합 — 웹 mypage 파리티)
            composable(Routes.MY_MEMOS) {
                MyMemosScreen(onBack = { navController.popBackStack() })
            }

            // 지원(1:1 문의)
            composable(Routes.SUPPORT_INQUIRIES) {
                InquiryListScreen(
                    onBack = { navController.popBackStack() },
                    onCompose = { navController.navigate(Routes.SUPPORT_INQUIRY_NEW) },
                    onItemClick = { id -> navController.navigate(Routes.supportInquiryDetail(id)) },
                )
            }
            composable(Routes.SUPPORT_INQUIRY_NEW) {
                InquiryComposeScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.SUPPORT_INQUIRY_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                InquiryComposeScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.SUPPORT_INQUIRY_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                InquiryDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.supportInquiryEdit(id)) },
                )
            }

            composable(
                route = Routes.STUDY_DICTIONARY,
                arguments = listOf(
                    navArgument("keyword") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                DictionaryListScreen(
                    onItemClick = { id -> navController.navigate(Routes.studyDictionaryDetail(id)) },
                    onBack = { navController.popBackStack() },
                    onProfileClick = openAccountSheet,
                )
            }
            composable(
                route = Routes.STUDY_DICTIONARY_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                DictionaryDetailScreen(
                    onBack = { navController.popBackStack() },
                    onProfileClick = openAccountSheet,
                )
            }
            composable(
                route = Routes.STUDY_CONTENT,
                arguments = listOf(navArgument("contentKey") { type = NavType.StringType }),
            ) { entry ->
                val key = entry.arguments?.getString("contentKey").orEmpty()
                // "7일 창조 체험"(creation)은 전용 몰입형 화면, 나머지는 범용 렌더러.
                if (key == "creation") {
                    CreationExperienceScreen(
                        onBack = { navController.popBackStack() },
                        onReadBible = { navigateRoute(Routes.BIBLE) },
                    )
                } else {
                    StaticContentScreen(
                        contentKey = key,
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            composable(
                route = Routes.BIBLE_BOOK_OVERVIEW,
                arguments = listOf(
                    navArgument("translationId") { type = NavType.StringType },
                    navArgument("bookOrder") { type = NavType.StringType },
                ),
            ) { entry ->
                val tid = entry.arguments?.getString("translationId")?.toLongOrNull() ?: return@composable
                val book = entry.arguments?.getString("bookOrder")?.toIntOrNull() ?: return@composable
                BibleBookOverviewScreen(
                    onBack = { navController.popBackStack() },
                    onChapterClick = { chapter ->
                        navController.navigate(Routes.bibleReader(tid, book, chapter))
                    },
                    // 하단 내비 중앙(📖 책 선택) → 책 목록.
                    onSelectBook = { navController.navigate(Routes.bibleBooks(tid)) },
                    // 이전/다음 책 — 현재 장 목록을 새 책으로 교체(replace).
                    onSwitchBook = { newBookOrder ->
                        navController.navigate(Routes.bibleBookOverview(tid, newBookOrder)) {
                            popUpTo(Routes.BIBLE_BOOK_OVERVIEW) { inclusive = true }
                        }
                    },
                    onOpenContent = { key -> navController.navigate(Routes.studyContent(key)) },
                    // 📘 요약 행 → 책 개요 전체 화면(웹 book-description).
                    onOpenDescription = { navController.navigate(Routes.bibleBookDescription(tid, book)) },
                    onChangeTranslation = openTranslationList,
                    onSearchClick = openBibleSearch,
                    onProfileClick = openAccountSheet,
                    // 화면 내 스크롤 방향 → 하단 탭 숨김/표시 연동.
                    onChromeVisibleChange = { bibleChromeVisible = it },
                )
            }
            composable(
                route = Routes.BIBLE_BOOK_DESCRIPTION,
                arguments = listOf(
                    navArgument("translationId") { type = NavType.StringType },
                    navArgument("bookOrder") { type = NavType.StringType },
                ),
            ) { entry ->
                val tid = entry.arguments?.getString("translationId")?.toLongOrNull() ?: return@composable
                BookDescriptionScreen(
                    onBack = { navController.popBackStack() },
                    // 하단 내비 중앙(📖 책 이름) → 해당 책의 장 목록.
                    onOpenChapterList = { t, book ->
                        navController.navigate(Routes.bibleBookOverview(t, book))
                    },
                    // 이전/다음 책 — 현재 개요를 새 책 개요로 교체(replace).
                    onSwitchBook = { newBookOrder ->
                        navController.navigate(Routes.bibleBookDescription(tid, newBookOrder)) {
                            popUpTo(Routes.BIBLE_BOOK_DESCRIPTION) { inclusive = true }
                        }
                    },
                    onChangeTranslation = openTranslationList,
                    onSearchClick = openBibleSearch,
                    onProfileClick = openAccountSheet,
                    onChromeVisibleChange = { bibleChromeVisible = it },
                )
            }
            composable(
                route = Routes.BIBLE_SEARCH,
                arguments = listOf(
                    navArgument("keyword") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                BibleSearchScreen(
                    onBack = { navController.popBackStack() },
                    onResultClick = { tid, book, chapter ->
                        navController.navigate(Routes.bibleReader(tid, book, chapter))
                    },
                )
            }

            composable(
                route = Routes.BIBLE_READER,
                arguments = listOf(
                    navArgument("translationId") { type = NavType.StringType },
                    navArgument("bookOrder") { type = NavType.StringType },
                    navArgument("chapterNumber") { type = NavType.StringType },
                ),
            ) {
                BibleReaderScreen(
                    onBack = { navController.popBackStack() },
                    // 하단 내비 중앙(📖 장 선택) → 해당 책의 장 목록.
                    onOpenChapterList = { tid, book ->
                        navController.navigate(Routes.bibleBookOverview(tid, book))
                    },
                    onChangeTranslation = openTranslationList,
                    onSearchClick = openBibleSearch,
                    onProfileClick = openAccountSheet,
                    // 화면 내 스크롤 방향 → 하단 탭 숨김/표시 연동.
                    onChromeVisibleChange = { bibleChromeVisible = it },
                )
            }
        }

        if (showAccountSheet) {
            val themeMode by themeViewModel.mode.collectAsStateWithLifecycle()
            AccountMenuSheet(
                loggedIn = authState == AuthState.Authenticated,
                themeMode = themeMode,
                onSelectTheme = themeViewModel::setMode,
                // 로그인은 현재 화면 위에 push(이전 화면 보존) → 로그인 성공 시 그 화면으로 복귀한다.
                onLogin = { showAccountSheet = false; navController.navigate(Routes.MY) { launchSingleTop = true } },
                onMyPage = { showAccountSheet = false; navigateRoute(Routes.MY) },
                onMyMemos = { showAccountSheet = false; navController.navigate(Routes.MY_MEMOS) },
                onInquiries = { showAccountSheet = false; navController.navigate(Routes.SUPPORT_INQUIRIES) },
                onLogout = { showAccountSheet = false; onLoggedOut() },
                onDismiss = { showAccountSheet = false },
            )
        }
    }
}

/**
 * 컴팩트 하단 탭바(웹 하단 탭 파리티, 높이 56dp + 제스처 인셋).
 * M3 NavigationBar 는 80dp 규격이라 56dp 로 줄이면 활성 인디케이터(알약)가 상하로 잘리고
 * 라벨의 선택/비선택 색 대비도 약해 아이콘만 활성처럼 보였다.
 * 웹과 동일하게 "아이콘 뒤 연한 프라이머리 필 + 아이콘·라벨 동시 강조"로 직접 그린다.
 */
@Composable
private fun AppBottomTabBar(
    tabs: List<BottomTab>,
    isSelected: (TopLevelDestination) -> Boolean,
    onNativeClick: (TopLevelDestination) -> Unit,
    onExternalClick: (String) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectableGroup(),
            ) {
                tabs.forEach { tab ->
                    // 외부(웹 위임) 탭은 화면에 머무르지 않으므로 항상 비활성 표시.
                    val selected = tab is BottomTab.Native && isSelected(tab.dest)
                    val accent by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "tabAccent",
                    )
                    val pill by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            Color.Transparent
                        },
                        label = "tabPill",
                    )
                    val label = stringResource(tab.labelRes)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = {
                                    when (tab) {
                                        is BottomTab.Native -> onNativeClick(tab.dest)
                                        is BottomTab.External -> onExternalClick(tab.path)
                                    }
                                },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 26.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(pill),
                            contentAlignment = Alignment.Center,
                        ) {
                            // 라벨 Text 가 탭 이름을 읽어주므로 아이콘 중복 낭독은 피한다.
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = accent,
                        )
                    }
                }
            }
            // 제스처 내비게이션 인셋(safe area) — 웹 env(safe-area-inset-bottom) 파리티.
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

/** 하단 탭 항목 — 네이티브 라우트 탭 또는 외부(웹) 위임 탭. */
private sealed interface BottomTab {
    val labelRes: Int
    val icon: ImageVector

    /** 네이티브 최상위 목적지 탭(홈/성경/학습). */
    data class Native(val dest: TopLevelDestination) : BottomTab {
        override val labelRes: Int get() = dest.labelRes
        override val icon: ImageVector get() = dest.icon
    }

    /** 외부 웹 화면 위임 탭(게임/커뮤니티) — Custom Tabs 로 [path] 를 연다. */
    data class External(
        override val labelRes: Int,
        override val icon: ImageVector,
        val path: String,
    ) : BottomTab
}

/**
 * 웹 하단 메뉴와 동일한 종류·순서: 성경 · 학습 · 홈(중앙) · 게임 · 커뮤니티.
 * 마이는 웹처럼 상단 프로필 아이콘(계정 메뉴)으로 이동하므로 하단 탭에서 제외한다.
 * 게임·커뮤니티는 v1 네이티브 범위 밖이라 웹 화면을 Custom Tabs 로 위임한다(PRD §4-A).
 */
private val bottomTabs: List<BottomTab> = listOf(
    BottomTab.Native(TopLevelDestination.BIBLE),
    BottomTab.Native(TopLevelDestination.STUDY),
    BottomTab.Native(TopLevelDestination.HOME),
    BottomTab.External(R.string.tab_game, Icons.Outlined.SportsEsports, "/web/game"),
    BottomTab.External(R.string.tab_community, Icons.AutoMirrored.Outlined.Chat, "/web/community"),
)
