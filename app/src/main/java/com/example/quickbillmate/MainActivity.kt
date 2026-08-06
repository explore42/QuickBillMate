package com.example.quickbillmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.navigation.QuickBillMateAppNavHost
import com.example.quickbillmate.ui.theme.QuickBillMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全面屏适配：内容延伸到系统栏，由标题栏按标准窗口边距留出合适区域
        enableEdgeToEdge()
        val settings = (application as QuickBillMateApp).repository.settings
        setContent {
            var themeMode by remember { mutableStateOf(settings.themeMode) }
            val darkTheme = when (themeMode) {
                SettingsStore.THEME_DARK -> true
                SettingsStore.THEME_LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            QuickBillMateTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                QuickBillMateAppNavHost(
                    navController = navController,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        settings.themeMode = mode
                    },
                )
            }
        }
    }
}
