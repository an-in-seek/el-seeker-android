package com.elseeker.android.app

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.elseeker.android.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elseeker.android.app.navigation.Routes
import com.elseeker.android.app.navigation.TopLevelDestination
import com.elseeker.android.feature.bible.ui.BibleBookOverviewScreen
import com.elseeker.android.feature.bible.ui.BibleBooksScreen
import com.elseeker.android.feature.bible.ui.BibleReaderScreen
import com.elseeker.android.feature.bible.ui.BibleSearchScreen
import com.elseeker.android.feature.bible.ui.MyMemosScreen
import com.elseeker.android.feature.home.ui.HomeScreen
import com.elseeker.android.feature.my.ui.LinkedAccountsScreen
import com.elseeker.android.feature.my.ui.MyScreen
import com.elseeker.android.feature.my.ui.ProfileEditScreen
import com.elseeker.android.feature.study.ui.DictionaryDetailScreen
import com.elseeker.android.feature.study.ui.DictionaryListScreen
import com.elseeker.android.feature.study.ui.StudyScreen
import com.elseeker.android.feature.study.ui.content.StaticContentScreen
import com.elseeker.android.feature.support.ui.InquiryComposeScreen
import com.elseeker.android.feature.support.ui.InquiryDetailScreen
import com.elseeker.android.feature.support.ui.InquiryListScreen

/**
 * 인증 완료 상태의 메인 셸. 하단 탭(홈/성경/학습/마이) + 내부 NavHost.
 * 성경 본문 뷰어는 탭 위에 push 되는 하위 라우트라 하단 탭을 숨긴다.
 */
@Composable
fun MainScaffold(
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    pendingDeepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }

    // App Links 로 보류된 라우트를 인증 완료(이 화면 진입) 후 1회 네비게이션한다.
    androidx.compose.runtime.LaunchedEffect(pendingDeepLink) {
        val route = pendingDeepLink ?: return@LaunchedEffect
        navController.navigate(route)
        onDeepLinkConsumed()
    }

    // 시스템 뒤로가기(PRD §6 ★): 최상위 탭 중 홈이 아니면 홈으로, 홈에서 한 번 더 누르면 종료.
    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = showBottomBar) {
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
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { dest ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                            label = { Text(stringResource(dest.labelRes)) },
                        )
                    }
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

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            composable(Routes.HOME) { HomeScreen(onNavigate = navigateRoute) }
            composable(Routes.BIBLE) {
                BibleBooksScreen(
                    onBookClick = { translationId, bookOrder ->
                        navController.navigate(Routes.bibleBookOverview(translationId, bookOrder))
                    },
                )
            }
            composable(Routes.STUDY) {
                StudyScreen(
                    onOpenDictionary = { navController.navigate(Routes.STUDY_DICTIONARY) },
                    onOpenContent = { key -> navController.navigate(Routes.studyContent(key)) },
                )
            }
            composable(Routes.MY) {
                MyScreen(
                    onLoggedOut = onLoggedOut,
                    onOpenProfileEdit = { navController.navigate(Routes.MY_PROFILE_EDIT) },
                    onOpenLinkedAccounts = { navController.navigate(Routes.MY_LINKED_ACCOUNTS) },
                    onOpenInquiries = { navController.navigate(Routes.SUPPORT_INQUIRIES) },
                    onOpenMyMemos = { navController.navigate(Routes.MY_MEMOS) },
                )
            }

            // 마이 하위 화면
            composable(Routes.MY_PROFILE_EDIT) {
                ProfileEditScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.MY_LINKED_ACCOUNTS) {
                LinkedAccountsScreen(onBack = { navController.popBackStack() })
            }
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

            composable(Routes.STUDY_DICTIONARY) {
                DictionaryListScreen(
                    onItemClick = { id -> navController.navigate(Routes.studyDictionaryDetail(id)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.STUDY_DICTIONARY_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                DictionaryDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.STUDY_CONTENT,
                arguments = listOf(navArgument("contentKey") { type = NavType.StringType }),
            ) { entry ->
                StaticContentScreen(
                    contentKey = entry.arguments?.getString("contentKey").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
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
                )
            }
            composable(Routes.BIBLE_SEARCH) {
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
                BibleReaderScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
