package com.example.quickbillmate.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

/** 分组小标题：组与组之间用一条细分割线隔开；多选模式下可传入 onSelectGroup 显示"全选"。 */
@Composable
fun GroupSectionHeader(
    title: String,
    showTopDivider: Boolean,
    onSelectGroup: (() -> Unit)? = null,
) {
    if (showTopDivider) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 10.dp),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        if (onSelectGroup != null) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSelectGroup) { Text("全选") }
        }
    }
}
