package com.example.quickbillmate.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * M3 兼容访问层：把既有代码中的 MaterialTheme.colorScheme / typography
 * 语义映射到 Miuix 配色与文本样式，实现低成本全量切换。
 *
 * 颜色字段名沿用 Material 3 命名，取值全部来自 MiuixTheme；
 * 随着逐页迁移到 Miuix 组件，本层将逐步退化为少量自定义工具。
 */
val AppThemeColors: AppColors
    @Composable get() {
        val c = MiuixTheme.colorScheme
        return AppColors(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.onPrimaryContainer,
            secondaryContainer = c.secondaryContainer,
            onSecondaryContainer = c.onSecondaryContainer,
            error = c.error,
            background = c.background,
            onBackground = c.onBackground,
            surface = c.surface,
            onSurface = c.onSurface,
            onSurfaceVariant = c.onSurfaceSecondary,
            surfaceContainerLow = c.surfaceContainer,
            surfaceContainer = c.surfaceContainer,
            surfaceContainerHigh = c.surfaceContainerHigh,
            surfaceContainerHighest = c.surfaceContainerHighest,
            outline = c.outline,
            outlineVariant = c.dividerLine,
        )
    }

/** M3 风格颜色别名（映射到 Miuix 配色角色）。 */
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val error: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val outline: Color,
    val outlineVariant: Color,
)

/** M3 风格文本别名（映射到 Miuix TextStyles）。 */
data class AppTypography(
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)

val AppThemeTypography: AppTypography
    @Composable get() {
        val t = MiuixTheme.textStyles
        return AppTypography(
            titleLarge = t.title1,
            titleMedium = t.title2,
            titleSmall = t.title3,
            bodyLarge = t.body1,
            bodyMedium = t.body2,
            bodySmall = t.footnote1,
            labelLarge = t.body2,
            labelMedium = t.footnote1,
            labelSmall = t.footnote2,
        )
    }
