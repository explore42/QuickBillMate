package com.example.quickbillmate.ui.theme

/** 主题种子色预置：argb 为 ARGB 色值（Long），0L 表示“跟随壁纸”。 */
data class ThemeColorPreset(
    val label: String,
    val argb: Long,
)

/** “跟随壁纸”哨兵值：不传 keyColor，由系统壁纸取色。 */
const val THEME_COLOR_WALLPAPER = 0L

/** 品牌紫默认种子色。 */
const val THEME_COLOR_BRAND_PURPLE = 0xFF9C11E1L

val ThemeColorPresets = listOf(
    ThemeColorPreset("品牌紫", THEME_COLOR_BRAND_PURPLE),
    ThemeColorPreset("蓝色", 0xFF0A84FFL),
    ThemeColorPreset("翠绿", 0xFF2E7D32L),
    ThemeColorPreset("暖橙", 0xFFE65100L),
    ThemeColorPreset("玫红", 0xFFC2185BL),
    ThemeColorPreset("青蓝", 0xFF0091EAL),
    ThemeColorPreset("紫色", 0xFF6A1B9AL),
)
