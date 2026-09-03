package com.example.quickbillmate.ui.changelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.DialogButtons
import com.example.quickbillmate.ui.common.DialogScrollColumn
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.Ds
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 升级更新说明对话框：升级后首次打开时展示 (上次已读, 当前] 版本的新功能与变动。
 * 已读策略保持"显示即标记"：ViewModel 创建时即写入 lastSeenVersionCode。
 */
@Composable
fun ChangelogDialog(
    onFinish: () -> Unit,
    viewModel: ChangelogViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    OverlayDialog(
        title = "更新说明",
        show = true,
        onDismissRequest = onFinish,
    ) {
        DialogScrollColumn(verticalArrangement = Arrangement.spacedBy(Ds.md)) {
            Text(
                "本次升级带来了以下新功能与变动",
                style = AppThemeTypography.bodySmall,
                color = AppThemeColors.onSurfaceVariant,
            )
            viewModel.sections.forEach { section ->
                VersionCard(section)
            }
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("开始使用")
            }
        }
    }
}

/**
 * 历史升级说明对话框：设置-关于入口，按版本倒序列出全部已发布版本的说明。
 */
@Composable
fun ChangelogHistoryDialog(onDismiss: () -> Unit) {
    OverlayDialog(
        title = "升级说明",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        DialogScrollColumn(verticalArrangement = Arrangement.spacedBy(Ds.md)) {
            val entries = VersionChangelog.entries.sortedByDescending { it.versionCode }
            if (entries.isEmpty()) {
                Text(
                    "暂无版本说明",
                    style = AppThemeTypography.bodyMedium,
                    color = AppThemeColors.onSurfaceVariant,
                )
            } else {
                entries.forEach { section ->
                    VersionCard(section)
                }
            }
            DialogButtons(
                confirmText = "关闭",
                cancelText = null,
                onConfirm = onDismiss,
            )
        }
    }
}

@Composable
private fun VersionCard(section: VersionChange) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = AppThemeColors.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(Ds.md)) {
            Text(
                section.title,
                style = AppThemeTypography.titleSmall,
                color = AppThemeColors.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Ds.sm))
            if (section.changes.isEmpty()) {
                Text(
                    "欢迎体验新版本",
                    style = AppThemeTypography.bodyMedium,
                    color = AppThemeColors.onSurfaceVariant,
                )
            } else {
                section.changes.forEach { change ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text(
                            "•",
                            style = AppThemeTypography.bodyMedium,
                            color = AppThemeColors.primary,
                        )
                        Spacer(Modifier.width(Ds.sm))
                        Text(
                            change,
                            style = AppThemeTypography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
