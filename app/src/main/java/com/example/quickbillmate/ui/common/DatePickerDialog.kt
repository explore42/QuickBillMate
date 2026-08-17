package com.example.quickbillmate.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import java.time.LocalDate
import java.time.YearMonth
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/** 年份范围：当前年份前后各 10 年。 */
private val YEAR_RANGE = (LocalDate.now().year - 10)..(LocalDate.now().year + 10)

/**
 * Miuix 风格日期选择器：年 / 月 / 日三列 NumberPicker。
 * 月份变化时自动把“日”收回到当月合法范围内。
 */
@Composable
fun DatePickerDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var year by remember { mutableIntStateOf(initial.year.coerceIn(YEAR_RANGE)) }
    var month by remember { mutableIntStateOf(initial.monthValue) }
    var day by remember { mutableIntStateOf(initial.dayOfMonth) }
    val maxDay = remember(year, month) { YearMonth.of(year, month).lengthOfMonth() }
    if (day > maxDay) day = maxDay

    OverlayDialog(
        title = "选择日期",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 三列必须各自 weight 分配宽度，否则会相互重叠（Miuix NumberPicker 官方示例同款布局）
                PickerColumn(year, YEAR_RANGE, { year = it }, "%04d", Modifier.weight(1.1f))
                PickerLabel("年")
                PickerColumn(month, 1..12, { month = it }, "%02d", Modifier.weight(1f), wrapAround = true)
                PickerLabel("月")
                PickerColumn(day, 1..maxDay, { day = it }, "%02d", Modifier.weight(1f), wrapAround = true)
                PickerLabel("日")
            }
            Spacer(Modifier.height(4.dp))
            DialogButtons(
                confirmText = "确定",
                onCancel = onDismiss,
                onConfirm = {
                    onConfirm(LocalDate.of(year, month, day.coerceAtMost(maxDay)))
                },
            )
        }
    }
}

@Composable
private fun PickerColumn(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    format: String,
    modifier: Modifier = Modifier,
    wrapAround: Boolean = false,
) {
    NumberPicker(
        value = value.coerceIn(range),
        onValueChange = onChange,
        range = range,
        label = { String.format(format, it) },
        wrapAround = wrapAround,
        modifier = modifier,
    )
}

@Composable
private fun PickerLabel(text: String) {
    Text(
        text = text,
        style = AppThemeTypography.titleSmall,
        color = AppThemeColors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}
