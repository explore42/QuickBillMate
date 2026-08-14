package com.example.quickbillmate.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text

/** 索引分区：label 为索引唯一标识，bubble 为按压时气泡显示文本。 */
data class IndexSection(val label: String, val bubble: String)

private val LetterBarWidth = 20.dp
private val TimeBarWidth = 20.dp

/** 气泡相对索引栏右侧的横向偏移（值越大越靠左），保证与索引栏留出距离。 */
private val BubbleGap = 28.dp

/** 手指离开后气泡停留时长，之后向右缩回消失。 */
private const val BubbleHoldMillis = 1000L
private const val BubbleRetractMillis = 220

/**
 * 索引栏共享核心：浅色圆角竖条、按压/拖动滚动、浮动气泡。
 * labelTransform 控制索引栏显示文本（字母栏显示字母、时间栏显示圆点）。
 */
@Composable
internal fun SectionIndexBar(
    state: LazyListState,
    sections: List<IndexSection>,
    firstIndexOf: (String) -> Int,
    modifier: Modifier = Modifier,
    barWidth: Dp = LetterBarWidth,
    endMargin: Dp = 18.dp,
    topPadding: Dp = 8.dp,
    bottomPadding: Dp = 88.dp,
    labelTransform: (String) -> String = { it },
) {
    if (sections.isEmpty()) return
    var active by remember { mutableStateOf<Int?>(null) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var hideJob by remember { mutableStateOf<Job?>(null) }
    val bubbleProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val barColor = AppThemeColors.surfaceContainerHigh
    val barTextColor = AppThemeColors.onSurfaceVariant
    val pillShape = RoundedCornerShape(percent = 50)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val overlayHeight = maxHeight
        val barHeight = overlayHeight - topPadding - bottomPadding

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = endMargin)
                .width(barWidth)
                .fillMaxHeight()
                .padding(top = topPadding, bottom = bottomPadding)
                .clip(pillShape)
                .background(barColor)
                .pointerInput(sections, firstIndexOf) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false) ?: return@awaitEachGesture
                        val pillHeight = size.height.toFloat()
                        val per = pillHeight / sections.size

                        fun activate(idx: Int) {
                            active = idx
                            hideJob?.cancel()
                            scope.launch { bubbleProgress.snapTo(1f) }
                            val itemIndex = firstIndexOf(sections[idx].label)
                            if (itemIndex >= 0) {
                                scrollJob?.cancel()
                                scrollJob = scope.launch { state.scrollToItem(itemIndex) }
                            }
                        }

                        activate(((down.position.y / per).toInt()).coerceIn(0, sections.size - 1))
                        var event = awaitPointerEvent()
                        while (event.changes.any { it.pressed }) {
                            event.changes.forEach { it.consume() }
                            val idx = ((event.changes.first().position.y / per).toInt())
                                .coerceIn(0, sections.size - 1)
                            if (idx != active) activate(idx)
                            event = awaitPointerEvent()
                        }
                        scrollJob?.cancel()
                        hideJob = scope.launch {
                            delay(BubbleHoldMillis)
                            bubbleProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(BubbleRetractMillis, easing = FastOutSlowInEasing),
                            )
                            active = null
                        }
                    }
                }
                .padding(horizontal = 3.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            sections.forEach { section ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = labelTransform(section.label),
                        style = AppThemeTypography.labelSmall,
                        color = barTextColor,
                        maxLines = 1,
                    )
                }
            }
        }

        active?.let { idx ->
            val regionHeight = barHeight / sections.size
            val centerY = topPadding + regionHeight * (idx + 0.5f)
            val offsetY = centerY - overlayHeight / 2f
            val bubble = sections[idx].bubble
            val isShort = bubble.length <= 2
            val bubbleShape = if (isShort) CircleShape else RoundedCornerShape(percent = 50)
            val progress = bubbleProgress.value
            val slideBack = 30.dp * (1f - progress)
            val scale = 0.5f + 0.5f * progress
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset {
                        IntOffset(
                            x = (-(barWidth + BubbleGap) + slideBack).roundToPx(),
                            y = offsetY.roundToPx(),
                        )
                    }
                    .graphicsLayer {
                        this.scaleX = scale
                        this.scaleY = scale
                        this.alpha = progress
                    }
                    .shadow(6.dp, bubbleShape)
                    .clip(bubbleShape)
                    .background(barColor)
                    .then(
                        if (isShort) {
                            Modifier.size(44.dp)
                        } else {
                            Modifier.height(40.dp).padding(horizontal = 10.dp)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = bubble,
                    color = barTextColor,
                    style = AppThemeTypography.bodyMedium,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 字母索引栏（商品 / 客户）：窄宽度，刚好容纳 A-Z / ★ 字母。 */
@Composable
fun LetterIndexBar(
    state: LazyListState,
    sections: List<IndexSection>,
    firstIndexOf: (String) -> Int,
    modifier: Modifier = Modifier,
) {
    SectionIndexBar(
        state = state,
        sections = sections,
        firstIndexOf = firstIndexOf,
        barWidth = LetterBarWidth,
        modifier = modifier,
    )
}

/** 时间索引栏（单据列表）：窄宽度显示圆点代表月份，气泡显示 yyyy年M月。 */
@Composable
fun TimeIndexBar(
    state: LazyListState,
    sections: List<IndexSection>,
    firstIndexOf: (String) -> Int,
    modifier: Modifier = Modifier,
) {
    SectionIndexBar(
        state = state,
        sections = sections,
        firstIndexOf = firstIndexOf,
        barWidth = TimeBarWidth,
        labelTransform = { if (it == "♥") "♥" else "•" },
        modifier = modifier,
    )
}
