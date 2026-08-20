package com.example.quickbillmate.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.quickbillmate.ui.changelog.ChangelogScreen
import com.example.quickbillmate.ui.contacts.ContactsImportScreen
import com.example.quickbillmate.ui.customers.CustomersScreen
import com.example.quickbillmate.ui.data.DataManagerScreen
import com.example.quickbillmate.ui.editor.EditorScreen
import com.example.quickbillmate.ui.home.HomeScreen
import com.example.quickbillmate.ui.onboarding.OnboardingScreen
import com.example.quickbillmate.ui.presets.PresetEditorScreen
import com.example.quickbillmate.ui.presets.PresetsScreen
import com.example.quickbillmate.ui.products.ProductsScreen
import com.example.quickbillmate.ui.report.ReportScreen
import com.example.quickbillmate.ui.settings.SettingsScreen
import com.example.quickbillmate.ui.view.BillViewScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val SLIDE_DURATION_MS = 320

@Composable
fun QuickBillMateAppNavHost(
    navController: NavHostController,
    startDestination: String = Routes.TABS,
    onThemeModeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeKeyColorChange: (Long) -> Unit,
    onThemePaletteStyleChange: (String) -> Unit = {},
    onHapticsChange: (Boolean) -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    var homeSelectionActive by remember { mutableStateOf(false) }
    var productsSelectionActive by remember { mutableStateOf(false) }
    var customersSelectionActive by remember { mutableStateOf(false) }
    var homeScrollTicks by remember { mutableIntStateOf(0) }
    var productsScrollTicks by remember { mutableIntStateOf(0) }
    var customersScrollTicks by remember { mutableIntStateOf(0) }

    val selectionActive = when (pagerState.currentPage) {
        0 -> homeSelectionActive
        1 -> productsSelectionActive
        2 -> customersSelectionActive
        else -> false
    }

    val backgroundColor = MiuixTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        // 捕获列表内容前先铺一层不透明背景色，避免透明像素扩散成色块
        drawRect(backgroundColor)
        drawContent()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                // 诊断二分 B：恢复 layerBackdrop，暂时不消费（无 textureBlur 覆盖层）
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(SLIDE_DURATION_MS, easing = FastOutSlowInEasing),
                    )
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(SLIDE_DURATION_MS, easing = FastOutSlowInEasing),
                    )
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(SLIDE_DURATION_MS, easing = FastOutSlowInEasing),
                    )
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(SLIDE_DURATION_MS, easing = FastOutSlowInEasing),
                    )
                },
            ) {
                composable(Routes.TABS) {
                    // 外部应用打开 JSON：主界面就绪后再进入导入确认（引导/更新说明页不提前弹走）
                    LaunchedEffect(PendingImport.uri) {
                        if (PendingImport.uri != null) {
                            navController.navigate(Routes.DATA_MANAGER) { launchSingleTop = true }
                        }
                    }
                    TabPagerHost(
                        pagerState = pagerState,
                        userScrollEnabled = !selectionActive,
                        homeScrollTicks = homeScrollTicks,
                        productsScrollTicks = productsScrollTicks,
                        customersScrollTicks = customersScrollTicks,
                        onSelectionModeChange = { tab, active ->
                            when (tab) {
                                0 -> homeSelectionActive = active
                                1 -> productsSelectionActive = active
                                2 -> customersSelectionActive = active
                            }
                        },
                        onNewBill = { navController.navigate(Routes.editor(0)) },
                        onOpenBill = { billId -> navController.navigate(Routes.view(billId)) },
                        onImportContacts = { navController.navigate(Routes.CONTACTS_IMPORT) },
                        onThemeModeChange = onThemeModeChange,
                        onDynamicColorChange = onDynamicColorChange,
                        onThemeKeyColorChange = onThemeKeyColorChange,
                        onThemePaletteStyleChange = onThemePaletteStyleChange,
                        onHapticsChange = onHapticsChange,
                        onManagePresets = { navController.navigate(Routes.PRESETS) },
                        onManageData = { navController.navigate(Routes.DATA_MANAGER) },
                        onOpenReport = { navController.navigate(Routes.REPORT) },
                    )
                }

                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onFinish = {
                            navController.navigate(Routes.TABS) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.CHANGELOG) {
                    ChangelogScreen(
                        onFinish = {
                            navController.navigate(Routes.TABS) {
                                popUpTo(Routes.CHANGELOG) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.DATA_MANAGER) {
                    DataManagerScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.REPORT) {
                    ReportScreen(
                        onBack = { navController.popBackStack() },
                        onOpenBill = { billId -> navController.navigate(Routes.view(billId)) },
                    )
                }

            composable(
                route = Routes.EDITOR,
                arguments = listOf(
                    navArgument(Routes.EDITOR_ARG_BILL_ID) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                ),
            ) { entry ->
                EditorScreen(
                    billId = entry.arguments?.getLong(Routes.EDITOR_ARG_BILL_ID) ?: 0L,
                    onBack = { navController.popBackStack() },
                    onManagePresets = { navController.navigate(Routes.PRESETS) },
                )
            }

            composable(
                route = Routes.VIEW,
                arguments = listOf(
                    navArgument(Routes.VIEW_ARG_BILL_ID) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                ),
            ) { entry ->
                BillViewScreen(
                    billId = entry.arguments?.getLong(Routes.VIEW_ARG_BILL_ID) ?: 0L,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.editor(id)) },
                )
            }

            composable(Routes.PRESETS) {
                PresetsScreen(
                    onBack = { navController.popBackStack() },
                    onNewPreset = { navController.navigate(Routes.presetEditor(0, "classic")) },
                    onEditPreset = { id -> navController.navigate(Routes.presetEditor(id, "classic")) },
                )
            }

            composable(
                route = Routes.PRESET_EDITOR,
                arguments = listOf(
                    navArgument(Routes.PRESET_EDITOR_ARG_ID) {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument(Routes.PRESET_EDITOR_ARG_BASE) {
                        type = NavType.StringType
                        defaultValue = "classic"
                    },
                ),
            ) { entry ->
                PresetEditorScreen(
                    presetId = entry.arguments?.getLong(Routes.PRESET_EDITOR_ARG_ID) ?: 0L,
                    base = entry.arguments?.getString(Routes.PRESET_EDITOR_ARG_BASE) ?: "classic",
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.CONTACTS_IMPORT) {
                ContactsImportScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }

            // 底部导航栏覆盖层：内容从栏下滑过时透出模糊（半透明材质）
            // textureBlur 直接挂在 NavigationBar 上（不额外包 Box），与 layerBackdrop 共存验证
            if (currentRoute == Routes.TABS && !selectionActive) {
                NavigationBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .textureBlur(
                            backdrop = backdrop,
                            shape = RectangleShape,
                            blurRadius = 24f,
                        ),
                    color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                    mode = NavigationBarDisplayMode.IconOnly,
                    showDivider = false,
                ) {
                    NavItem(MiuixIcons.Demibold.File, "单据", pagerState.currentPage == 0) {
                        if (pagerState.currentPage == 0) homeScrollTicks++ else scope.launch { pagerState.animateScrollToPage(0) }
                    }
                    NavItem(MiuixIcons.Demibold.ListView, "商品", pagerState.currentPage == 1) {
                        if (pagerState.currentPage == 1) productsScrollTicks++ else scope.launch { pagerState.animateScrollToPage(1) }
                    }
                    NavItem(MiuixIcons.Demibold.Contacts, "客户", pagerState.currentPage == 2) {
                        if (pagerState.currentPage == 2) customersScrollTicks++ else scope.launch { pagerState.animateScrollToPage(2) }
                    }
                    NavItem(MiuixIcons.Demibold.Settings, "设置", pagerState.currentPage == 3) {
                        if (pagerState.currentPage != 3) scope.launch { pagerState.animateScrollToPage(3) }
                    }
                }
            }
        }
    }
}

/** 四个底部标签页容器：左右滑动切换，底部导航同步。 */
@Composable
private fun TabPagerHost(
    pagerState: PagerState,
    userScrollEnabled: Boolean,
    homeScrollTicks: Int,
    productsScrollTicks: Int,
    customersScrollTicks: Int,
    onSelectionModeChange: (Int, Boolean) -> Unit,
    onNewBill: () -> Unit,
    onOpenBill: (Long) -> Unit,
    onImportContacts: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeKeyColorChange: (Long) -> Unit,
    onThemePaletteStyleChange: (String) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onManagePresets: () -> Unit,
    onManageData: () -> Unit,
    onOpenReport: () -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = userScrollEnabled,
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                onNewBill = onNewBill,
                onOpenBill = onOpenBill,
                onOpenReport = onOpenReport,
                onSelectionModeChange = { onSelectionModeChange(0, it) },
                scrollToTopTick = homeScrollTicks,
            )
            1 -> ProductsScreen(
                onSelectionModeChange = { onSelectionModeChange(1, it) },
                scrollToTopTick = productsScrollTicks,
            )
            2 -> CustomersScreen(
                onImportContacts = onImportContacts,
                onSelectionModeChange = { onSelectionModeChange(2, it) },
                scrollToTopTick = customersScrollTicks,
            )
            3 -> SettingsScreen(
                onThemeModeChange = onThemeModeChange,
                onDynamicColorChange = onDynamicColorChange,
                onThemeKeyColorChange = onThemeKeyColorChange,
                onThemePaletteStyleChange = onThemePaletteStyleChange,
                onHapticsChange = onHapticsChange,
                onManagePresets = onManagePresets,
                onManageData = onManageData,
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = label,
    )
}
