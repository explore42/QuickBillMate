package com.example.quickbillmate.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete

/**
 * 多电话编辑器：每行一个输入框 + 删除。
 * 「添加电话」入口由调用方放在小标题行（见 [PhoneSectionHeader]）。
 */
@Composable
fun PhoneListEditor(
    phones: List<String>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        phones.forEachIndexed { index, phone ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = phone,
                    onValueChange = { newValue ->
                        onChange(phones.mapIndexed { i, p -> if (i == index) newValue else p })
                    },
                    label = "电话 ${index + 1}",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f).testTag("phone_input"),
                )
                if (phones.size > 1) {
                    IconButton(onClick = { onChange(phones.filterIndexed { i, _ -> i != index }) }) {
                        Icon(MiuixIcons.Delete, contentDescription = "删除电话")
                    }
                }
            }
        }
    }
}

/** 「客户电话」小标题行：标题居左，右侧「＋添加」小按钮。 */
@Composable
fun PhoneSectionHeader(
    phoneCount: Int,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("客户电话", style = AppThemeTypography.titleSmall, modifier = Modifier.weight(1f))
        Text(
            text = "＋ 添加电话",
            style = AppThemeTypography.labelMedium,
            color = AppThemeColors.primary,
            modifier = Modifier
                .clickable(onClick = onAdd)
                .padding(vertical = 2.dp),
        )
    }
}
