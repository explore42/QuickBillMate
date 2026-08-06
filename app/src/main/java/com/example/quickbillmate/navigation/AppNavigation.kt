package com.example.quickbillmate.navigation

import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.ui.contacts.ContactsImportScreen
import com.example.quickbillmate.ui.customers.CustomersScreen
import com.example.quickbillmate.ui.editor.EditorScreen
import com.example.quickbillmate.ui.home.HomeScreen
import com.example.quickbillmate.ui.presets.PresetEditorScreen
import com.example.quickbillmate.ui.presets.PresetsScreen
import com.example.quickbillmate.ui.products.ProductsScreen
import com.example.quickbillmate.ui.settings.SettingsScreen

@Composable
fun QuickBillMateAppNavHost(
    navController: NavHostController,
    themeMode: String,
    darkTheme: Boolean,
    onThemeModeChange: (String) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in Routes.tabRoutes) {
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
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNewBill = { navController.navigate(Routes.editor(0)) },
                    onOpenBill = { billId -> navController.navigate(Routes.editor(billId)) },
                    onToggleTheme = {
                        onThemeModeChange(
                            if (themeMode == SettingsStore.THEME_DARK) {
                                SettingsStore.THEME_LIGHT
                            } else {
                                SettingsStore.THEME_DARK
                            }
                        )
                    },
                    darkTheme = darkTheme,
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
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
    )
}
