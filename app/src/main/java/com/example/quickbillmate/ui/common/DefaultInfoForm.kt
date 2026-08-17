package com.example.quickbillmate.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.quickbillmate.util.InputLimits
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text

/** “默认信息”表单的数据集合：设置页（全局默认）与单据编辑页（仅当前单据）共用。 */
data class DefaultInfoValues(
    val titleSuffix: String = "单据",
    val docCode: String = "PH",
    val showMultiPhones: Boolean = false,
    val companyName: String = "",
    val manager: String = "",
    val showManager: Boolean = true,
    val contactPhone: String = "",
    val showContactPhone: Boolean = true,
    val remark: String = "",
    val showRemark: Boolean = true,
    val showAd: Boolean = false,
    val adText: String = "",
    val watermarkText: String = "",
    val showWatermark: Boolean = false,
)

/** 默认信息表单：顺序与单据表单一致（标题栏 → 客户信息 → 公司信息 → 其他信息），分组展示。 */
@Composable
fun DefaultInfoForm(
    values: DefaultInfoValues,
    onChange: (DefaultInfoValues) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("标题栏")
        LabeledField(
            "标题后缀",
            values.titleSuffix,
            { if (it.length <= InputLimits.CODE) onChange(values.copy(titleSuffix = it)) },
        )
        Spacer(Modifier.height(12.dp))
        LabeledField(
            "编号代码",
            values.docCode,
            { if (it.length <= InputLimits.CODE) onChange(values.copy(docCode = it)) },
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle("客户信息")
        LabeledSwitch("显示多个客户电话", values.showMultiPhones) {
            onChange(values.copy(showMultiPhones = it))
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("公司信息")
        LabeledField(
            "公司名称",
            values.companyName,
            { if (it.length <= InputLimits.COMPANY) onChange(values.copy(companyName = it)) },
        )
        Spacer(Modifier.height(12.dp))
        InlineFieldRow(
            label = "客户经理",
            value = values.manager,
            onValueChange = { if (it.length <= InputLimits.MANAGER) onChange(values.copy(manager = it)) },
            checked = values.showManager,
            onCheckedChange = { onChange(values.copy(showManager = it)) },
        )
        Spacer(Modifier.height(12.dp))
        InlineFieldRow(
            label = "联系电话",
            value = values.contactPhone,
            onValueChange = { onChange(values.copy(contactPhone = it)) },
            checked = values.showContactPhone,
            onCheckedChange = { onChange(values.copy(showContactPhone = it)) },
            keyboardType = KeyboardType.Phone,
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle("其他信息")
        InlineFieldRow(
            label = "备注",
            value = values.remark,
            onValueChange = { if (it.length <= InputLimits.REMARK) onChange(values.copy(remark = it)) },
            checked = values.showRemark,
            onCheckedChange = { onChange(values.copy(showRemark = it)) },
        )
        Spacer(Modifier.height(12.dp))
        InlineFieldRow(
            label = "广告文案",
            value = values.adText,
            onValueChange = { if (it.length <= InputLimits.AD) onChange(values.copy(adText = it)) },
            checked = values.showAd,
            onCheckedChange = { onChange(values.copy(showAd = it)) },
        )
        Spacer(Modifier.height(12.dp))
        InlineFieldRow(
            label = "水印文案",
            value = values.watermarkText,
            onValueChange = { if (it.length <= InputLimits.AD) onChange(values.copy(watermarkText = it)) },
            checked = values.showWatermark,
            onCheckedChange = { onChange(values.copy(showWatermark = it)) },
        )
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
        Spacer(Modifier.width(8.dp))
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
