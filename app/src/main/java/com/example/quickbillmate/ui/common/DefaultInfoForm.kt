package com.example.quickbillmate.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.Ds
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quickbillmate.data.repository.DefaultInfoValues
import com.example.quickbillmate.util.InputLimits
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text

/** 默认信息表单：顺序与单据表单一致（标题栏 → 客户信息 → 公司信息 → 其他信息），分组展示。 */
@Composable
fun DefaultInfoForm(
    values: DefaultInfoValues,
    onChange: (DefaultInfoValues) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Ds.lg),
    ) {
        FormSection("标题栏") {
            LabeledField(
                "标题后缀",
                values.titleSuffix,
                { if (it.length <= InputLimits.CODE) onChange(values.copy(titleSuffix = it)) },
            )
            LabeledField(
                "编号代码",
                values.docCode,
                { if (it.length <= InputLimits.CODE) onChange(values.copy(docCode = it)) },
            )
        }

        FormSection("客户信息") {
            LabeledSwitch("显示客户电话", values.showCustomerPhone) {
                onChange(values.copy(showCustomerPhone = it))
            }
            if (values.showCustomerPhone) {
                LabeledSwitch("显示多个客户电话", values.showMultiPhones) {
                    onChange(values.copy(showMultiPhones = it))
                }
            }
        }

        FormSection("公司信息") {
            LabeledField(
                "公司名称",
                values.companyName,
                { if (it.length <= InputLimits.COMPANY) onChange(values.copy(companyName = it)) },
            )
            InlineFieldRow(
                label = "客户经理",
                value = values.manager,
                onValueChange = { if (it.length <= InputLimits.MANAGER) onChange(values.copy(manager = it)) },
                checked = values.showManager,
                onCheckedChange = { onChange(values.copy(showManager = it)) },
            )
            InlineFieldRow(
                label = "联系电话",
                value = values.contactPhone,
                onValueChange = { onChange(values.copy(contactPhone = it)) },
                checked = values.showContactPhone,
                onCheckedChange = { onChange(values.copy(showContactPhone = it)) },
                keyboardType = KeyboardType.Phone,
            )
        }

        FormSection("其他信息") {
            InlineFieldRow(
                label = "备注",
                value = values.remark,
                onValueChange = { onChange(values.copy(remark = it.take(InputLimits.REMARK))) },
                checked = values.showRemark,
                onCheckedChange = { onChange(values.copy(showRemark = it)) },
            )
            AdTextEditor(
                value = values.adText,
                onValueChange = { onChange(values.copy(adText = it.take(InputLimits.AD))) },
                checked = values.showAd,
                onCheckedChange = { onChange(values.copy(showAd = it)) },
            )
            InlineFieldRow(
                label = "水印文案",
                value = values.watermarkText,
                onValueChange = { onChange(values.copy(watermarkText = it.take(InputLimits.WATERMARK))) },
                checked = values.showWatermark,
                onCheckedChange = { onChange(values.copy(showWatermark = it)) },
            )
        }
    }
}

/** 表单分组：小标题与内容间距 Ds.md（对齐 SectionCard 标题间距规范），组内元素间距 Ds.md，组间间距由外层 Ds.lg 控制。 */
@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Ds.md)) {
        SectionTitle(title)
        Column(verticalArrangement = Arrangement.spacedBy(Ds.md)) {
            content()
        }
    }
}

/** 输入框与显示开关同行。 */
@Composable
private fun InlineFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabeledField(
            label = label,
            value = value,
            onChange = onValueChange,
            modifier = Modifier.weight(1f),
            keyboardType = keyboardType,
        )
        Spacer(Modifier.width(Ds.sm))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * 广告文案：标签与显示开关一行，间隔后为多行输入框与单据页脚实时预览。
 * 预览与实际单据一致：左对齐、13sp、超长内容在固定高度内滚动。
 */
@Composable
private fun AdTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Ds.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("广告文案", style = AppThemeTypography.bodyMedium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        LabeledField(
            label = "",
            placeholder = "支持多行，换行后在单据上分段显示",
            value = value,
            onChange = onValueChange,
            singleLine = false,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
        )
        Surface(
            shape = RoundedCornerShape(Ds.md),
            color = AppThemeColors.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(Ds.md)) {
                Text(
                    "单据页脚预览",
                    style = AppThemeTypography.labelMedium,
                    color = AppThemeColors.onSurfaceVariant,
                )
                Spacer(Modifier.height(Ds.sm))
                if (value.isBlank()) {
                    Text(
                        "未填写广告文案",
                        style = AppThemeTypography.bodySmall,
                        color = AppThemeColors.onSurfaceVariant,
                    )
                } else {
                    Text(
                        value,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = AppThemeTypography.titleSmall,
        color = AppThemeColors.primary,
    )
}
