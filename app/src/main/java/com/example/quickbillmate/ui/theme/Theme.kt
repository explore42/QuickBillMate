package com.example.quickbillmate.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 应用主题入口：基于 Miuix 的主题体系。
 *
 * - darkTheme = null：跟随系统（ColorSchemeMode.System）
 * - darkTheme = true/false：强制深色/浅色（对应设置页主题模式）
 * - dynamicColor = true：启用 Monet 动态取色；keyColor 为种子色（null = 跟随壁纸）
 *
 * 配色与文本样式使用 Miuix 默认的调色板与字号体系
 * （浅色主色为蓝色 0xFF0A84FF），如需品牌定制可在
 * ThemeController 中传入 lightColors/darkColors/textStyles。
 */
@Composable
fun QuickBillMateTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = true,
    keyColor: Color? = Color(0xFF9C11E1),
    content: @Composable () -> Unit,
) {
    val mode = when {
        dynamicColor && darkTheme == null -> ColorSchemeMode.MonetSystem
        dynamicColor && darkTheme == true -> ColorSchemeMode.MonetDark
        dynamicColor && darkTheme == false -> ColorSchemeMode.MonetLight
        darkTheme == true -> ColorSchemeMode.Dark
        darkTheme == false -> ColorSchemeMode.Light
        else -> ColorSchemeMode.System
    }
    val controller = remember(mode, dynamicColor, keyColor) {
        ThemeController(
            colorSchemeMode = mode,
            keyColor = if (dynamicColor) keyColor else null,
        )
    }
    MiuixTheme(controller = controller, content = content)
}
