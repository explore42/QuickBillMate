package com.example.quickbillmate.ui.changelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.Ds
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text

/** 升级更新说明页：累积展示 (上次已读, 当前] 版本的新功能与变动。 */
@Composable
fun ChangelogScreen(
    onFinish: () -> Unit,
    viewModel: ChangelogViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    Scaffold(
        topBar = {
            AppTopBar(title = "更新说明")
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Ds.screen, vertical = Ds.sm),
                verticalArrangement = Arrangement.spacedBy(Ds.md),
            ) {
                Text(
                    "本次升级带来了以下新功能与优化",
                    style = AppThemeTypography.bodySmall,
                    color = AppThemeColors.onSurfaceVariant,
                )
                viewModel.sections.forEach { section ->
                    VersionCard(section)
                }
                Spacer(Modifier.height(Ds.sm))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Ds.screen, vertical = Ds.md),
            ) {
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("开始使用")
                }
            }
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
