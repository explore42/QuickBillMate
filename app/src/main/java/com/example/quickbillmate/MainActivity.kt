package com.example.quickbillmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.navigation.QuickBillMateAppNavHost
import com.example.quickbillmate.render.InvoiceRenderEngine
import com.example.quickbillmate.ui.common.ProvideHaptics
import com.example.quickbillmate.ui.theme.QuickBillMateTheme
import com.example.quickbillmate.ui.theme.paletteStyleOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全面屏适配：内容延伸到系统栏，由标题栏按标准窗口边距留出合适区域
        enableEdgeToEdge()
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
                        val navController = rememberNavController()
                        QuickBillMateAppNavHost(
                            navController = navController,
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
                        // 全局单据渲染引擎：离屏 1×1，不占布局，持续消费渲染请求
                        InvoiceRenderEngine()
                    }
                }
            }
        }
    }
}
