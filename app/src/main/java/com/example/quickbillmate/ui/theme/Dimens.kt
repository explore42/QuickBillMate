package com.example.quickbillmate.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 全局间距规范：所有页面统一使用这里的档位，禁止散落的硬编码 dp。
 * 4 的倍数为主，保证不同页面节奏一致。
 */
object Ds {
    /** 元素内小间距（图标与文字、标签与输入框）。 */
    val xs = 4.dp

    /** 相关元素间距（同一组字段的行间距、图标与文本）。 */
    val sm = 8.dp

    /** 组内元素间距（表单字段之间、卡片标题与内容）。 */
    val md = 12.dp

    /** 卡片内边距、区块间距。 */
    val lg = 16.dp

    /** 屏幕左右边距 / 大区块分隔。 */
    val screen = 16.dp

    /** 列表行上下内边距。 */
    val rowVertical = 12.dp

    /** 主按钮高度。 */
    val buttonHeight = 44.dp
}
