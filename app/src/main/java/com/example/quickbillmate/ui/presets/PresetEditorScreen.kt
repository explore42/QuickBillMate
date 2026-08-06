package com.example.quickbillmate.ui.presets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LabeledSwitch
import com.example.quickbillmate.ui.common.SectionCard

@Composable
fun PresetEditorScreen(
    presetId: Long,
    base: String,
    onBack: () -> Unit,
    viewModel: PresetEditorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(Unit) {
        viewModel.load(presetId, base)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (presetId == 0L) "新建预设" else "编辑预设",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (!viewModel.loaded) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val p = viewModel.params

                SectionCard("基本信息") {
                    LabeledField("预设名称", viewModel.name, viewModel::updateName, Modifier.fillMaxWidth())
                }

                SectionCard("布局") {
                    IntField("纸张宽度（dp）", p.paperWidthDp) { newValue ->
                        viewModel.updateParams { params -> params.copy(paperWidthDp = newValue) }
                    }
                    IntField("客单信息标签列宽（px）", p.infoLabelWidthPx) { newValue ->
                        viewModel.updateParams { params -> params.copy(infoLabelWidthPx = newValue) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("字体", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = p.fontFamily == "system_serif",
                            onClick = { viewModel.updateParams { it.copy(fontFamily = "system_serif") } },
                            label = { Text("宋体（衬线）") },
                        )
                        FilterChip(
                            selected = p.fontFamily == "system_sans",
                            onClick = { viewModel.updateParams { it.copy(fontFamily = "system_sans") } },
                            label = { Text("黑体（无衬线）") },
                        )
                    }
                }

                SectionCard("标题") {
                    IntField("标题字号（sp）", p.titleFontSizeSp) { newValue ->
                        viewModel.updateParams { params -> params.copy(titleFontSizeSp = newValue) }
                    }
                    LabeledSwitch("标题加粗", p.titleBold) {
                        viewModel.updateParams { params -> params.copy(titleBold = it) }
                    }
                    IntField("标题字间距（px）", p.titleLetterSpacing) { newValue ->
                        viewModel.updateParams { params -> params.copy(titleLetterSpacing = newValue) }
                    }
                    LabeledSwitch("标题下划线", p.titleUnderline) {
                        viewModel.updateParams { params -> params.copy(titleUnderline = it) }
                    }
                    if (p.titleUnderline) {
                        ColorField("下划线颜色", p.titleUnderlineColor) { newValue ->
                            viewModel.updateParams { params -> params.copy(titleUnderlineColor = newValue) }
                        }
                    }
                }

                SectionCard("表格") {
                    IntField("表头字号（sp）", p.headerFontSizeSp) { newValue ->
                        viewModel.updateParams { params -> params.copy(headerFontSizeSp = newValue) }
                    }
                    IntField("正文字号（sp）", p.bodyFontSizeSp) { newValue ->
                        viewModel.updateParams { params -> params.copy(bodyFontSizeSp = newValue) }
                    }
                    IntField("边框宽度（px）", p.tableBorderWidthPx) { newValue ->
                        viewModel.updateParams { params -> params.copy(tableBorderWidthPx = newValue) }
                    }
                    ColorField("边框颜色", p.tableBorderColor) { newValue ->
                        viewModel.updateParams { params -> params.copy(tableBorderColor = newValue) }
                    }
                    ColorField("表头底色", p.headerBgColor) { newValue ->
                        viewModel.updateParams { params -> params.copy(headerBgColor = newValue) }
                    }
                    ColorField("表头文字颜色", p.headerTextColor) { newValue ->
                        viewModel.updateParams { params -> params.copy(headerTextColor = newValue) }
                    }
                    ColorField("合计行底色", p.totalRowBgColor) { newValue ->
                        viewModel.updateParams { params -> params.copy(totalRowBgColor = newValue) }
                    }
                    LabeledSwitch("金额列加粗", p.amountBold) {
                        viewModel.updateParams { params -> params.copy(amountBold = it) }
                    }
                }

                SectionCard("水印") {
                    LabeledSwitch("显示水印", p.watermarkEnabled) {
                        viewModel.updateParams { params -> params.copy(watermarkEnabled = it) }
                    }
                    LabeledField(
                        "水印文案",
                        p.watermarkText,
                        { newValue -> viewModel.updateParams { params -> params.copy(watermarkText = newValue) } },
                        Modifier.fillMaxWidth(),
                    )
                    IntField("水印字号（sp）", p.watermarkFontSizeSp) { newValue ->
                        viewModel.updateParams { params -> params.copy(watermarkFontSizeSp = newValue) }
                    }
                    ColorField("水印颜色", p.watermarkColor) { newValue ->
                        viewModel.updateParams { params -> params.copy(watermarkColor = newValue) }
                    }
                }

                SectionCard("页脚") {
                    IntField("页脚字号（sp）", p.footerTextSizeSp) { newValue ->
                        viewModel.updateParams { params -> params.copy(footerTextSizeSp = newValue) }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.restoreDefaults() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("恢复默认值")
                    }
                    Button(
                        onClick = { viewModel.save(onBack) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("保存")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun IntField(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            input.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

private val COLOR_PRESETS = listOf(
    "#000000" to Color.Black,
    "#FFFFFF" to Color.White,
    "#E8E8E8" to Color(0xFFE8E8E8),
    "#666666" to Color(0xFF666666),
    "#1F4E79" to Color(0xFF1F4E79),
    "#B8942E" to Color(0xFFB8942E),
    "#C62828" to Color(0xFFC62828),
    "#2E7D32" to Color(0xFF2E7D32),
    "#6A1B9A" to Color(0xFF6A1B9A),
    "#E65100" to Color(0xFFE65100),
)

@Composable
private fun ColorField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            COLOR_PRESETS.forEach { (hex, color) ->
                Card(
                    modifier = Modifier
                        .width(28.dp)
                        .height(28.dp)
                        .clickable { onChange(hex) },
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (value.equals(hex, ignoreCase = true)) "✓" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF333333),
                        )
                    }
                }
            }
        }
    }
}
