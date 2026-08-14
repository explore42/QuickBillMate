package com.example.quickbillmate.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore

/** 多电话编辑器：每行一个输入框，支持上下移动与删除，底部可追加。 */
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
                if (index > 0) {
                    IconButton(onClick = {
                        val list = phones.toMutableList()
                        val tmp = list[index - 1]
                        list[index - 1] = list[index]
                        list[index] = tmp
                        onChange(list)
                    }) {
                        Icon(MiuixIcons.ExpandLess, contentDescription = "上移")
                    }
                }
                if (index < phones.lastIndex) {
                    IconButton(onClick = {
                        val list = phones.toMutableList()
                        val tmp = list[index + 1]
                        list[index + 1] = list[index]
                        list[index] = tmp
                        onChange(list)
                    }) {
                        Icon(MiuixIcons.ExpandMore, contentDescription = "下移")
                    }
                }
                if (phones.size > 1) {
                    IconButton(onClick = { onChange(phones.filterIndexed { i, _ -> i != index }) }) {
                        Text("−", style = AppThemeTypography.titleMedium)
                    }
                }
            }
        }
        TextButton(
            text = "＋ 添加电话",
            onClick = { onChange(phones + "") },
        )
    }
}
