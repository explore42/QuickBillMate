package com.example.quickbillmate.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 弹窗卡片顶部/底部与屏幕边缘（或输入法上缘）之间保留的最小间距。 */
private val DialogScrollMargin = 12.dp

/** 键盘几乎占满屏幕（如横屏）时仍保留的最小滚动可视高度。 */
private val DialogScrollMinHeight = 120.dp

/**
 * 弹窗内容滚动容器（软键盘适配）。
 *
 * miuix `OverlayDialog` 在手机端（宽 <840dp）不对卡片限高：全屏容器应用
 * `imePadding()` 后底部锚定的整卡会被抬升完整键盘高度，且内容因为不限高而永不
 * 真正滚动，长表单会直接溢出屏幕上缘。本组件给内容加上动态高度上限
 * （屏幕高 − 状态栏 − 输入法高度 − 余量），键盘弹出时卡片自动收缩到可视区内，
 * 超长内容在内部滚动，聚焦输入框由 `verticalScroll` 的 bringIntoView 自动滚到可见。
 *
 * 含输入框的弹窗（新增/编辑商品、客户、默认信息、预置单位、单据设置）统一使用本容器；
 * 纯展示/按钮弹窗（关于、确认、详情等）保持原样。
 */
@Composable
fun DialogScrollColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val windowHeight = LocalWindowInfo.current.containerDpSize.height
    // 组合内响应式读取：键盘弹出/收起时自动重算高度上限。
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val maxHeight: Dp = (windowHeight - statusBarTop - imeBottom - DialogScrollMargin)
        .coerceAtLeast(DialogScrollMinHeight)

    Column(
        modifier = modifier
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        content()
    }
}
