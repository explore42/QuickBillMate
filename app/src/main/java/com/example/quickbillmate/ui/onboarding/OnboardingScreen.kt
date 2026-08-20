package com.example.quickbillmate.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.repository.DefaultInfoValues
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppLogo
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.DefaultInfoForm
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.Ds
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Store

/** 首次安装引导页：欢迎 → 填写默认信息（可跳过）。 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var step by remember { mutableIntStateOf(0) }
    when (step) {
        0 -> WelcomeStep(
            onGo = { step = 1 },
            onSkip = {
                viewModel.skip()
                onFinish()
            },
        )
        else -> FormStep(
            values = viewModel.defaults,
            onChange = viewModel::updateDefaults,
            onSave = {
                viewModel.complete()
                onFinish()
            },
            onSkip = {
                viewModel.skip()
                onFinish()
            },
        )
    }
}

@Composable
private fun WelcomeStep(
    onGo: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Ds.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppLogo(Modifier.size(88.dp))
            Spacer(Modifier.height(Ds.lg))
            Text(
                "快贝智单",
                style = AppThemeTypography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Ds.sm))
            Text(
                "为小微商户而生的销售单据助手",
                style = AppThemeTypography.bodyMedium,
                color = AppThemeColors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Ds.lg))
            FeatureRow(MiuixIcons.Store, "开单更快", "客户、商品、默认信息一次填好，新建单据直接带入")
            Spacer(Modifier.height(Ds.md))
            FeatureRow(MiuixIcons.Report, "报表分析", "按时间、客户、商品统计单据与金额，辅助经营决策")
            Spacer(Modifier.height(Ds.md))
            FeatureRow(MiuixIcons.File, "数据自主", "单据、商品、客户、默认信息可随时导入导出，不锁数据")
            Spacer(Modifier.height(Ds.lg))
            Button(
                onClick = onGo,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("去填写默认信息")
            }
            Spacer(Modifier.height(Ds.sm))
            TextButton(
                text = "跳过",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Ds.md),
        color = AppThemeColors.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Ds.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = AppThemeColors.primary)
            Spacer(Modifier.width(Ds.md))
            Column {
                Text(title, style = AppThemeTypography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    desc,
                    style = AppThemeTypography.bodySmall,
                    color = AppThemeColors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FormStep(
    values: DefaultInfoValues,
    onChange: (DefaultInfoValues) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "填写默认信息")
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Ds.screen, vertical = Ds.lg),
            verticalArrangement = Arrangement.spacedBy(Ds.lg),
        ) {
            Text(
                "这些信息会自动带入每一张新单据，之后可在设置中随时修改。",
                style = AppThemeTypography.bodySmall,
                color = AppThemeColors.onSurfaceVariant,
            )
            DefaultInfoForm(values = values, onChange = onChange)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Ds.screen, vertical = Ds.md)
                .imePadding(),
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存并开始使用")
            }
            Spacer(Modifier.height(Ds.sm))
            TextButton(
                text = "跳过",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
