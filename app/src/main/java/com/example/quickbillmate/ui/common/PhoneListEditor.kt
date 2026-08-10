package com.example.quickbillmate.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        onChange(phones.mapIndexed { i, p -> if (i == index) newValue else p })
                    },
                    label = { Text("电话 ${index + 1}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f),
                )
                if (index > 0) {
                    IconButton(onClick = {
                        val list = phones.toMutableList()
                        val tmp = list[index - 1]
                        list[index - 1] = list[index]
                        list[index] = tmp
                        onChange(list)
                    }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
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
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                    }
                }
                if (phones.size > 1) {
                    IconButton(onClick = { onChange(phones.filterIndexed { i, _ -> i != index }) }) {
                        Text("−", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        TextButton(onClick = { onChange(phones + "") }) {
            Text("＋ 添加电话")
        }
    }
}
