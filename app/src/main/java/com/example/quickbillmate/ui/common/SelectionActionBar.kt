package com.example.quickbillmate.ui.common

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Share

/** 多选底部操作栏：复制 / 编辑 / 导出 / 删除（编辑仅单选可用）。 */
@Composable
fun SelectionActionBar(
    canEdit: Boolean,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionAction(MiuixIcons.Add, "复制", true, onCopy)
        SelectionAction(MiuixIcons.Edit, "编辑", canEdit, onEdit)
        SelectionAction(MiuixIcons.Share, "导出", true, onExport)
        SelectionAction(MiuixIcons.Delete, "删除", true, onDelete)
    }
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = { if (enabled) onClick() },
                onLongClick = null,
            )
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) {
                AppThemeColors.onSurface
            } else {
                AppThemeColors.outlineVariant
            },
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = AppThemeTypography.labelSmall,
            color = if (enabled) {
                AppThemeColors.onSurface
            } else {
                AppThemeColors.outlineVariant
            },
        )
    }
}
