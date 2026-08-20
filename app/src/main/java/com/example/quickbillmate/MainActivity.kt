package com.example.quickbillmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.navigation.QuickBillMateAppNavHost
import com.example.quickbillmate.navigation.PendingImport
import com.example.quickbillmate.navigation.Routes
import com.example.quickbillmate.render.InvoiceRenderEngine
import com.example.quickbillmate.ui.common.ProvideHaptics
import com.example.quickbillmate.ui.theme.QuickBillMateTheme
import com.example.quickbillmate.ui.theme.paletteStyleOf

class MainActivity : ComponentActivity() {
    private var navControllerRef: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全面屏适配：内容延伸到系统栏，由标题栏按标准窗口边距留出合适区域
        enableEdgeToEdge()
        if (intent?.action == Intent.ACTION_VIEW) {
            PendingImport.uri = intent.data
        }
        val settings = (application as QuickBillMateApp).repository.settings
        setContent {
            var themeMode by remember { mutableStateOf(settings.themeMode) }
            var dynamicColor by remember { mutableStateOf(settings.dynamicColor) }
            var themeKeyColor by remember { mutableStateOf(settings.themeKeyColor) }
            var themePaletteStyle by remember { mutableStateOf(settings.themePaletteStyle) }
            var hapticsEnabled by remember { mutableStateOf(settings.hapticsEnabled) }
            val darkTheme = when (themeMode) {
                SettingsStore.THEME_DARK -> true
                SettingsStore.THEME_LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            val keyColor = if (themeKeyColor == 0L) null else Color(themeKeyColor)
            QuickBillMateTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
                keyColor = keyColor,
                paletteStyle = paletteStyleOf(themePaletteStyle),
            ) {
                Box {
                    ProvideHaptics(enabled = hapticsEnabled) {
                        val repo = (application as QuickBillMateApp).repository
                        // 首启判定：仅全新安装（未完成引导且库中无任何数据）显示引导页
                        var startDestination by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(Unit) {
                            startDestination = if (!repo.settings.onboardingCompleted && !repo.hasAnyDataOnce()) {
                                Routes.ONBOARDING
                            } else {
                                Routes.TABS
                            }
                        }
                        val navController = rememberNavController()
                        navControllerRef = navController
                        // 外部应用打开 JSON：进入主界面后自动跳转到数据管理页
                        LaunchedEffect(startDestination, PendingImport.uri) {
                            if (startDestination != null && PendingImport.uri != null) {
                                navController.navigate(Routes.DATA_MANAGER) { launchSingleTop = true }
                            }
                        }
                        val destination = startDestination
                        if (destination == null) {
                            Box(modifier = Modifier.fillMaxSize())
                        } else {
                            QuickBillMateAppNavHost(
                                navController = navController,
                                startDestination = destination,
                                onThemeModeChange = { mode ->
                                    themeMode = mode
                                    settings.themeMode = mode
                                },
                                onDynamicColorChange = { enabled ->
                                    dynamicColor = enabled
                                    settings.dynamicColor = enabled
                                },
                                onThemeKeyColorChange = { argb ->
                                    themeKeyColor = argb
                                    settings.themeKeyColor = argb
                                },
                                onThemePaletteStyleChange = { style ->
                                    themePaletteStyle = style
                                    settings.themePaletteStyle = style
                                },
                                onHapticsChange = { enabled ->
                                    hapticsEnabled = enabled
                                    settings.hapticsEnabled = enabled
                                },
                            )
                        }
                        // 全局单据渲染引擎：离屏 1×1，不占布局，持续消费渲染请求
                        InvoiceRenderEngine()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            PendingImport.uri = intent.data
            navControllerRef?.navigate(Routes.DATA_MANAGER) { launchSingleTop = true }
        }
    }
}
