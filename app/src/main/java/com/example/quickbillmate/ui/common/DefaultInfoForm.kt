package com.example.quickbillmate.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.Ds
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.quickbillmate.data.repository.DefaultInfoValues
import com.example.quickbillmate.util.InputLimits
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
                onValueChange = { if (it.length <= InputLimits.REMARK) onChange(values.copy(remark = it)) },
                checked = values.showRemark,
                onCheckedChange = { onChange(values.copy(showRemark = it)) },
            )
            InlineFieldRow(
                label = "广告文案",
                value = values.adText,
                onValueChange = { if (it.length <= InputLimits.AD) onChange(values.copy(adText = it)) },
                checked = values.showAd,
                onCheckedChange = { onChange(values.copy(showAd = it)) },
            )
            InlineFieldRow(
                label = "水印文案",
                value = values.watermarkText,
                onValueChange = { if (it.length <= InputLimits.AD) onChange(values.copy(watermarkText = it)) },
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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = AppThemeTypography.titleSmall,
        color = AppThemeColors.primary,
    )
}
