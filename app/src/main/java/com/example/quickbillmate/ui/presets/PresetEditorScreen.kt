package com.example.quickbillmate.ui.presets

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.render.DEFAULT_COLUMNS
import com.example.quickbillmate.render.DEFAULT_ORDER
import com.example.quickbillmate.render.DEFAULT_WEIGHTS
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.DialogButtons
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LabeledSwitch
import com.example.quickbillmate.ui.common.SectionCard
import com.example.quickbillmate.ui.common.SelectionChip
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.Ds
import java.util.Locale
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore

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
                title = if (presetId == 0L) "新建图片样式" else "编辑图片样式",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
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
            val p = viewModel.params
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Ds.screen, vertical = Ds.sm)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(Ds.md),
            ) {
                SectionCard("基本信息") {
                    LabeledField("预设名称", viewModel.name, viewModel::updateName, Modifier.fillMaxWidth())
                }

                SectionCard("布局与字体") {
                    Column(verticalArrangement = Arrangement.spacedBy(Ds.sm)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Ds.sm)) {
                            IntField(
                                label = "纸张宽度 (dp)",
                                value = p.paperWidthDp,
                                onChange = { v -> viewModel.updateParams { it.copy(paperWidthDp = v) } },
                                modifier = Modifier.weight(1f),
                            )
                            IntField(
                                label = "标签列宽 (px)",
                                value = p.infoLabelWidthPx,
                                onChange = { v -> viewModel.updateParams { it.copy(infoLabelWidthPx = v) } },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Ds.sm)) {
                            SelectionChip(
                                text = "宋体（衬线）",
                                selected = p.fontFamily == "system_serif",
                                onClick = { viewModel.updateParams { it.copy(fontFamily = "system_serif") } },
                            )
                            SelectionChip(
                                text = "黑体（无衬线）",
                                selected = p.fontFamily == "system_sans",
                                onClick = { viewModel.updateParams { it.copy(fontFamily = "system_sans") } },
                            )
                        }
                    }
                }

                SectionCard(
                    title = "表格列",
                    action = {
                        Text(
                            text = "重置",
                            style = AppThemeTypography.labelMedium,
                            color = AppThemeColors.primary,
                            modifier = Modifier.clickable {
                                viewModel.updateParams {
                                    it.copy(columnOrder = emptyList(), columnWeights = emptyList())
                                }
                            },
                        )
                    },
                ) {
                    val order = p.columnOrder.ifEmpty { DEFAULT_ORDER }
                    val weights = p.columnWeights.ifEmpty { DEFAULT_WEIGHTS }
                    Column(verticalArrangement = Arrangement.spacedBy(Ds.xs)) {
                        order.forEachIndexed { index, id ->
                            ColumnOrderRow(
                                label = DEFAULT_COLUMNS[id].label,
                                upEnabled = index > 0,
                                downEnabled = index < order.lastIndex,
                                weightValue = weights[index],
                                onMove = { delta ->
                                    viewModel.updateParams {
                                        it.copy(
                                            columnOrder = move(order, index, index + delta),
                                            columnWeights = move(weights, index, index + delta),
                                        )
                                    }
                                },
                                onWeight = { value ->
                                    viewModel.updateParams { params ->
                                        val list = params.columnWeights.ifEmpty { DEFAULT_WEIGHTS }.toMutableList()
                                        list[index] = value
                                        params.copy(columnWeights = list)
                                    }
                                },
                            )
                        }
                    }
                }

                SectionCard("标题样式") {
                    Column(verticalArrangement = Arrangement.spacedBy(Ds.sm)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Ds.sm)) {
                            IntField(
                                label = "字号 (sp)",
                                value = p.titleFontSizeSp,
                                onChange = { v -> viewModel.updateParams { it.copy(titleFontSizeSp = v) } },
                                modifier = Modifier.weight(1f),
                            )
                            IntField(
                                label = "字间距 (px)",
                                value = p.titleLetterSpacing,
                                onChange = { v -> viewModel.updateParams { it.copy(titleLetterSpacing = v) } },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        LabeledSwitch("加粗", p.titleBold) { v ->
                            viewModel.updateParams { it.copy(titleBold = v) }
                        }
                        LabeledSwitch("下划线", p.titleUnderline) { v ->
                            viewModel.updateParams { it.copy(titleUnderline = v) }
                        }
                        if (p.titleUnderline) {
                            ColorField(
                                label = "下划线颜色",
                                value = p.titleUnderlineColor,
                                onChange = { v -> viewModel.updateParams { it.copy(titleUnderlineColor = v) } },
                            )
                        }
                    }
                }

                SectionCard("表格样式") {
                    Column(verticalArrangement = Arrangement.spacedBy(Ds.sm)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Ds.sm)) {
                            IntField(
                                label = "表头字号 (sp)",
                                value = p.headerFontSizeSp,
                                onChange = { v -> viewModel.updateParams { it.copy(headerFontSizeSp = v) } },
                                modifier = Modifier.weight(1f),
                            )
                            IntField(
                                label = "正文字号 (sp)",
                                value = p.bodyFontSizeSp,
                                onChange = { v -> viewModel.updateParams { it.copy(bodyFontSizeSp = v) } },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Ds.sm)) {
                            IntField(
                                label = "边框宽度 (px)",
                                value = p.tableBorderWidthPx,
                                onChange = { v -> viewModel.updateParams { it.copy(tableBorderWidthPx = v) } },
                                modifier = Modifier.weight(1f),
                            )
                            // 占位对齐，保持两列布局节奏
                            Spacer(Modifier.weight(1f))
                        }
                        ColorField(
                            label = "边框颜色",
                            value = p.tableBorderColor,
                            onChange = { v -> viewModel.updateParams { it.copy(tableBorderColor = v) } },
                        )
                        ColorField(
                            label = "表头底色",
                            value = p.headerBgColor,
                            onChange = { v -> viewModel.updateParams { it.copy(headerBgColor = v) } },
                        )
                        ColorField(
                            label = "表头文字颜色",
                            value = p.headerTextColor,
                            onChange = { v -> viewModel.updateParams { it.copy(headerTextColor = v) } },
                        )
                        ColorField(
                            label = "合计行底色",
                            value = p.totalRowBgColor,
                            onChange = { v -> viewModel.updateParams { it.copy(totalRowBgColor = v) } },
                        )
                        LabeledSwitch("金额列加粗", p.amountBold) { v ->
                            viewModel.updateParams { it.copy(amountBold = v) }
                        }
                    }
                }

                SectionCard("水印") {
                    Column(verticalArrangement = Arrangement.spacedBy(Ds.sm)) {
                        LabeledSwitch("显示水印", p.watermarkEnabled) { v ->
                            viewModel.updateParams { it.copy(watermarkEnabled = v) }
                        }
                        if (p.watermarkEnabled) {
                            LabeledField(
                                label = "水印文案",
                                value = p.watermarkText,
                                onChange = { v -> viewModel.updateParams { it.copy(watermarkText = v) } },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(Ds.sm)) {
                                IntField(
                                    label = "字号 (sp)",
                                    value = p.watermarkFontSizeSp,
                                    onChange = { v -> viewModel.updateParams { it.copy(watermarkFontSizeSp = v) } },
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.weight(1f))
                            }
                            ColorField(
                                label = "水印颜色",
                                value = p.watermarkColor,
                                onChange = { v -> viewModel.updateParams { it.copy(watermarkColor = v) } },
                            )
                        }
                    }
                }

                SectionCard("页脚") {
                    IntField(
                        label = "页脚字号 (sp)",
                        value = p.footerTextSizeSp,
                        onChange = { v -> viewModel.updateParams { it.copy(footerTextSizeSp = v) } },
                    )
                }

                DialogButtons(
                    confirmText = "保存",
                    cancelText = "恢复默认值",
                    onCancel = { viewModel.restoreDefaults() },
                    onConfirm = { viewModel.save(onBack) },
                )
                Spacer(Modifier.size(Ds.sm))
            }
        }
    }
}

/** 表格列顺序与权重：上移/下移 + 名称 + 权重滑杆 + 数值。 */
@Composable
private fun ColumnOrderRow(
    label: String,
    upEnabled: Boolean,
    downEnabled: Boolean,
    weightValue: Float,
    onMove: (Int) -> Unit,
    onWeight: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Ds.xs),
    ) {
        IconButton(onClick = { onMove(-1) }, enabled = upEnabled) {
            Icon(MiuixIcons.ExpandLess, contentDescription = "上移")
        }
        IconButton(onClick = { onMove(1) }, enabled = downEnabled) {
            Icon(MiuixIcons.ExpandMore, contentDescription = "下移")
        }
        Text(
            label,
            style = AppThemeTypography.bodyMedium,
            modifier = Modifier.width(64.dp),
        )
        Slider(
            value = weightValue,
            onValueChange = onWeight,
            valueRange = 0.5f..5f,
            steps = 44,
            modifier = Modifier.weight(1f),
        )
        Text(
            String.format(Locale.US, "%.1f", weightValue),
            style = AppThemeTypography.labelMedium,
            color = AppThemeColors.onSurfaceVariant,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End,
        )
    }
}

/** 紧凑数字输入框：非数字输入不落库。 */
@Composable
private fun IntField(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    TextField(
        value = text,
        onValueChange = { input ->
            text = input
            input.toIntOrNull()?.let(onChange)
        },
        label = label,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
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

/** 颜色输入 + 预设色板（FlowRow 自适应换行，选中项描主色边框）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Ds.xs)) {
        TextField(
            value = value,
            onValueChange = onChange,
            label = label,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Ds.sm),
            verticalArrangement = Arrangement.spacedBy(Ds.xs),
        ) {
            COLOR_PRESETS.forEach { (hex, color) ->
                val selected = value.equals(hex, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color,
                    modifier = Modifier
                        .size(30.dp)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) {
                                AppThemeColors.primary
                            } else {
                                AppThemeColors.outlineVariant
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { onChange(hex) },
                ) {
                    if (selected) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "✓",
                                style = AppThemeTypography.labelSmall,
                                color = Color(0xFF333333),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 把列表中 from 位置的元素移动到 to 位置（用于列重排）。 */
private fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
    val m = list.toMutableList()
    if (from < 0 || from >= m.size || to < 0 || to >= m.size) return m
    val item = m.removeAt(from)
    m.add(to, item)
    return m
}
