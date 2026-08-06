package com.example.quickbillmate.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

private const val SLIDE_DURATION_MS = 320
private const val TAB_SLIDE_DURATION_MS = 240
private const val TAB_FADE_DURATION_MS = 150

/** 底部导航标签页之间切换时使用轻量动画，二级页面保持全屏滑动。 */
private fun isTabRoute(route: String?): Boolean = route in Routes.tabRoutes

@Composable
fun QuickBillMateAppNavHost(
    navController: NavHostController,
    onThemeModeChange: (String) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var homeSelectionActive by remember { mutableStateOf(false) }

    Scaffold(
        // 顶部边距由各页面自己的标题栏处理，外层不再额外让出状态栏高度
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in Routes.tabRoutes && !(currentRoute == Routes.HOME && homeSelectionActive)) {
                NavigationBar {
                    TabItem(Icons.Default.Home, "首页", currentRoute == Routes.HOME) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    TabItem(Icons.Default.List, "商品", currentRoute == Routes.PRODUCTS) {
                        navController.navigate(Routes.PRODUCTS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    TabItem(Icons.Default.Person, "客户", currentRoute == Routes.CUSTOMERS) {
                        navController.navigate(Routes.CUSTOMERS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    TabItem(Icons.Default.Settings, "设置", currentRoute == Routes.SETTINGS) {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = {
                if (isTabRoute(initialState.destination.route) && isTabRoute(targetState.destination.route)) {
                    slideInHorizontally(
                        initialOffsetX = { it / 3 },
                        animationSpec = tween(TAB_SLIDE_DURATION_MS, easing = FastOutSlowInEasing),
                    )
                } else {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(SLIDE_DURATION_MS, easing = FastOutSlowInEasing),
                    )
                }
            },
            exitTransition = {
                if (isTabRoute(initialState.destination.route) && isTabRoute(targetState.destination.route)) {
                    fadeOut(animationSpec = tween(TAB_FADE_DURATION_MS))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(SLIDE_DURATION_MS, easing = FastOutSlowInEasing),
                    )
                }
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
            composable(Routes.HOME) {
                HomeScreen(
                    onNewBill = { navController.navigate(Routes.editor(0)) },
                    onOpenBill = { billId -> navController.navigate(Routes.view(billId)) },
                    onSelectionModeChange = { active -> homeSelectionActive = active },
                )
            }

            composable(Routes.PRODUCTS) {
                ProductsScreen()
            }

            composable(Routes.CUSTOMERS) {
                CustomersScreen(
                    onImportContacts = { navController.navigate(Routes.CONTACTS_IMPORT) },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onThemeModeChange = onThemeModeChange)
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

@Composable
private fun RowScope.TabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // 选中项图标轻微放大
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
        label = { Text(label) },
    )
}
