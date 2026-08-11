package com.example.quickbillmate.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.quickbillmate.ui.contacts.ContactsImportScreen
import com.example.quickbillmate.ui.customers.CustomersScreen
import com.example.quickbillmate.ui.editor.EditorScreen
import com.example.quickbillmate.ui.home.HomeScreen
import com.example.quickbillmate.ui.presets.PresetEditorScreen
import com.example.quickbillmate.ui.presets.PresetsScreen
import com.example.quickbillmate.ui.products.ProductsScreen
import com.example.quickbillmate.ui.settings.SettingsScreen
import com.example.quickbillmate.ui.view.BillViewScreen
import kotlinx.coroutines.launch

private const val TABS = "tabs"
private const val SLIDE_DURATION_MS = 320

@Composable
fun QuickBillMateAppNavHost(
    navController: NavHostController,
    onThemeModeChange: (String) -> Unit,
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute == TABS && !selectionActive) {
                NavigationBar {
                    TabItem(Icons.Default.Home, "单据", pagerState.currentPage == 0) {
                        if (pagerState.currentPage == 0) homeScrollTicks++ else scope.launch { pagerState.animateScrollToPage(0) }
                    }
                    TabItem(Icons.Default.List, "商品", pagerState.currentPage == 1) {
                        if (pagerState.currentPage == 1) productsScrollTicks++ else scope.launch { pagerState.animateScrollToPage(1) }
                    }
                    TabItem(Icons.Default.Person, "客户", pagerState.currentPage == 2) {
                        if (pagerState.currentPage == 2) customersScrollTicks++ else scope.launch { pagerState.animateScrollToPage(2) }
                    }
                    TabItem(Icons.Default.Settings, "设置", pagerState.currentPage == 3) {
                        if (pagerState.currentPage != 3) scope.launch { pagerState.animateScrollToPage(3) }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TABS,
            modifier = Modifier.padding(padding),
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
            composable(TABS) {
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
                    onManagePresets = { navController.navigate(Routes.PRESETS) },
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
    onManagePresets: () -> Unit,
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
                onManagePresets = onManagePresets,
            )
        }
    }
}

@Composable
private fun RowScope.TabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "navIconScale",
    )
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.scale(iconScale),
            )
        },
        label = if (selected) {
            { Text(label) }
        } else {
            null
        },
    )
}
