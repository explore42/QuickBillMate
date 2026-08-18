package com.example.quickbillmate.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** 三档触觉语义：tick 轻点确认、confirm 操作成功、longPress 长按/破坏性警示；可用开关统一关闭。 */
class Haptics(
    private val feedback: HapticFeedback,
    private val enabled: () -> Boolean,
) {
    fun tick() {
        if (enabled()) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    fun confirm() {
        if (enabled()) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    fun longPress() {
        if (enabled()) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

val LocalHaptics = staticCompositionLocalOf<Haptics> {
    error("Haptics not provided")
}

/** 在 MainActivity 根部包一层，全 App 通过 LocalHaptics.current 使用统一触觉；enabled 由设置驱动。 */
@Composable
fun ProvideHaptics(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val feedback = LocalHapticFeedback.current
    val haptics = remember(enabled) { Haptics(feedback) { enabled } }
    CompositionLocalProvider(LocalHaptics provides haptics) {
        content()
    }
}
