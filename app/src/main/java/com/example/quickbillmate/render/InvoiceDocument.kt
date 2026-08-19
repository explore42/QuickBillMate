package com.example.quickbillmate.render

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.quickbillmate.util.Money
import kotlinx.coroutines.delay

data class RenderItem(
    val name: String = "",
    val spec: String = "",
    val unit: String = "桶",
    val qty: Double = 1.0,
    val price: Double = 0.0,
    val pack: String = "",
    val note: String = "",
) {
    fun amount(): Double = if (qty <= 0) 0.0 else Money.round2(qty * price)
}

data class RenderInvoice(
    val customerName: String = "",
    val customerPhone: String = "",
    val companyName: String = "",
    val contactPhone: String = "",
    val salesManager: String = "",
    val docCode: String = "XS",
    val docSerial: String = "",
    val docDate: String = "",
    val discount: Double = 0.0,
    val remark: String = "",
    val titleSuffix: String = "单据",
    val adText: String = "",
    val showManager: Boolean = true,
    val showRemark: Boolean = true,
    val showAd: Boolean = false,
    val showContactPhone: Boolean = false,
    val showCustomerPhone: Boolean = true,
    val watermarkText: String = "",
    val showWatermark: Boolean = true,
    /** 全局微信二维码（设置页上传并裁剪后的方形位图）；null 表示单据不显示二维码。 */
    val qrBitmap: Bitmap? = null,
    val items: List<RenderItem> = emptyList(),
) {
    fun total(): Double = items.sumOf { it.amount() }

    fun receivable(): Double = Math.max(0.0, Money.round2(total() - discount))
}

/** 表格列定义：id 稳定，order/weights 可由样式预设覆盖。 */
internal data class ColumnSpec(
    val id: Int,
    val label: String,
    val defaultWeight: Float,
    val dataAlign: TextAlign,
)

internal val DEFAULT_COLUMNS: List<ColumnSpec> = listOf(
    ColumnSpec(0, "序号", 1.0f, TextAlign.Center),
    ColumnSpec(1, "名称", 2.7f, TextAlign.Left),
    ColumnSpec(2, "规格", 1.8f, TextAlign.Left),
    ColumnSpec(3, "单位", 1.0f, TextAlign.Center),
    ColumnSpec(4, "数量", 1.3f, TextAlign.Center),
    ColumnSpec(5, "单价", 1.6f, TextAlign.Center),
    ColumnSpec(8, "金额", 2.0f, TextAlign.Center),
    ColumnSpec(7, "备注", 2.0f, TextAlign.Left),
)

internal val DEFAULT_ORDER: List<Int> = DEFAULT_COLUMNS.map { it.id }
internal val DEFAULT_WEIGHTS: List<Float> = DEFAULT_COLUMNS.map { it.defaultWeight }

internal val COLUMNS_BY_ID: Map<Int, ColumnSpec> = DEFAULT_COLUMNS.associateBy { it.id }

/** 已下架列 id：v1 的“包装”列——数据仍随单据保留，仅表格不再展示。 */
private val RETIRED_COLUMN_IDS = setOf(6)

/** 单据左上角二维码边长与标题预留间距。 */
private val QR_SIZE_DP = 96.dp
private val QR_GAP_DP = 16.dp

/** 根据样式预设解析生效的列定义；配置非法时回退默认。 */
internal fun effectiveColumns(params: StyleParams): List<ColumnSpec> {
    val rawOrder = params.columnOrder.ifEmpty { DEFAULT_ORDER }
    val rawWeights = params.columnWeights
    // 旧预设可能包含已下架列（如“包装”）：剔除后再按 id 集合校验
    val order = rawOrder.filter { it !in RETIRED_COLUMN_IDS }
    if (order.size != DEFAULT_COLUMNS.size || order.toSet() != COLUMNS_BY_ID.keys) {
        return DEFAULT_COLUMNS
    }
    // 权重与原列序按位置配对剔除；数量不匹配时忽略（沿用 v1 行为：仅权重不生效）
    val weights =
        if (rawWeights.size == rawOrder.size) {
            rawOrder.mapIndexed { index, id -> if (id in RETIRED_COLUMN_IDS) null else rawWeights[index] }
                .filterNotNull()
        } else {
            emptyList()
        }
    return if (weights.size == order.size && weights.all { it > 0f }) {
        order.mapIndexed { index, id ->
            COLUMNS_BY_ID.getValue(id).copy(defaultWeight = weights[index])
        }
    } else {
        order.map { COLUMNS_BY_ID.getValue(it) }
    }
}

/** 旧预设列配置归一化：剔除已下架列（含按位置配对的权重），供预设编辑页加载时调用。 */
internal fun StyleParams.dropRetiredColumns(): StyleParams {
    val hasRetiredOrder = columnOrder.any { it in RETIRED_COLUMN_IDS }
    // v1 中“仅权重无列序”的配置从未参与渲染（effectiveColumns 一并回退默认），归一化时直接重置
    val legacyWeightsOnly = columnOrder.isEmpty() && columnWeights.size > DEFAULT_COLUMNS.size
    if (!hasRetiredOrder && !legacyWeightsOnly) return this
    val order = columnOrder.filter { it !in RETIRED_COLUMN_IDS }
    val weights =
        if (columnOrder.isNotEmpty() && columnWeights.size == columnOrder.size) {
            columnOrder.mapIndexed { index, id -> if (id in RETIRED_COLUMN_IDS) null else columnWeights[index] }
                .filterNotNull()
        } else {
            emptyList()
        }
    return copy(columnOrder = order, columnWeights = weights)
}

/**
 * 单据文档：白底黑字，版式完全由 Compose 布局引擎计算，避免手绘错位。
 * 预览与导出共用同一组件，保证输出完全一致。
 */
@Composable
internal fun InvoiceDocument(invoice: RenderInvoice, params: StyleParams) {
    val fontFamily = if (params.fontFamily == "system_serif") FontFamily.Serif else FontFamily.SansSerif
    val bodySize = params.bodyFontSizeSp.sp
    val borderColor = parseColor(params.tableBorderColor)
    val borderWidth = params.tableBorderWidthPx.dp
    val columns = remember(params) { effectiveColumns(params) }

    Column(
        modifier = Modifier
            .width(params.paperWidthDp.dp)
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        // ---------- 标题 ----------
        val titleText = buildString {
            if (invoice.companyName.isNotBlank()) append(invoice.companyName)
            append(invoice.titleSuffix)
        }
        if (titleText.isNotBlank() || invoice.qrBitmap != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // 左上角微信二维码：仅占左侧区域，标题左右对称留白避免重叠；无二维码时布局与现状一致。
                invoice.qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(QR_SIZE_DP)
                            .background(Color.White),
                    )
                }
                if (titleText.isNotBlank()) {
                    val qrPad = if (invoice.qrBitmap != null) {
                        Modifier.padding(start = QR_SIZE_DP + QR_GAP_DP, end = QR_SIZE_DP + QR_GAP_DP)
                    } else {
                        Modifier
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().then(qrPad),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = titleText,
                                fontSize = params.titleFontSizeSp.sp,
                                fontWeight = if (params.titleBold) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = params.titleLetterSpacing.sp,
                                color = Color.Black,
                                fontFamily = fontFamily,
                                maxLines = 1,
                            )
                            if (params.titleUnderline) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 7.dp)
                                        .height(1.6.dp)
                                        .fillMaxWidth()
                                        .background(parseColor(params.titleUnderlineColor)),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // ---------- 单据编号（右对齐，与表格右缘对齐） ----------
        Text(
            text = (invoice.docCode + invoice.docDate.replace("-", "") + invoice.docSerial).ifBlank { "—" },
            fontSize = bodySize,
            color = Color.Black,
            fontFamily = fontFamily,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        // ---------- 客户信息：客户/电话靠左，单据日期靠右 ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "客户：${invoice.customerName.ifBlank { "—" }}",
                fontSize = bodySize,
                color = Color.Black,
                fontFamily = fontFamily,
            )
            Spacer(Modifier.width(24.dp))
            if (invoice.showCustomerPhone) {
                Text(
                    text = "客户电话：${invoice.customerPhone.ifBlank { "—" }}",
                    fontSize = bodySize,
                    color = Color.Black,
                    fontFamily = fontFamily,
                    modifier = Modifier.weight(1f),
                )
            } else {
                // 电话隐藏时仍需撑开中段，保持单据日期右对齐
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = "单据日期：${invoice.docDate.ifBlank { "—" }}",
                fontSize = bodySize,
                color = Color.Black,
                fontFamily = fontFamily,
                textAlign = TextAlign.End,
            )
        }
        Spacer(Modifier.height(10.dp))

        // ---------- 表格 ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor) else Modifier),
        ) {
            TableRow(
                columns = columns,
                cells = columns.map { it.label },
                aligns = List(columns.size) { TextAlign.Center },
                fontSize = params.headerFontSizeSp.sp,
                bold = true,
                fontFamily = fontFamily,
                borderColor = borderColor,
                borderWidth = borderWidth,
                bg = parseColor(params.headerBgColor),
                textColor = parseColor(params.headerTextColor),
                drawBottomDivider = true,
            )
            invoice.items.forEachIndexed { index, item ->
                TableRow(
                    columns = columns,
                    cells = columns.map { spec ->
                        when (spec.id) {
                            0 -> (index + 1).toString()
                            1 -> item.name
                            2 -> item.spec
                            3 -> item.unit
                            4 -> item.qty.toLong().toString()
                            5 -> Money.format(item.price)
                            7 -> item.note
                            8 -> Money.format(item.amount())
                            else -> ""
                        }
                    },
                    aligns = columns.map { it.dataAlign },
                    fontSize = bodySize,
                    bold = false,
                    fontFamily = fontFamily,
                    borderColor = borderColor,
                    borderWidth = borderWidth,
                    bg = null,
                    textColor = Color.Black,
                    drawBottomDivider = true,
                    amountBold = params.amountBold,
                )
            }
            TotalRow(
                columns = columns,
                label = "合计",
                totalText = Money.format(invoice.total()),
                fontSize = bodySize,
                fontFamily = fontFamily,
                borderColor = borderColor,
                borderWidth = borderWidth,
                bg = parseColor(params.totalRowBgColor),
                textColor = Color.Black,
                drawBottomDivider = invoice.discount != 0.0,
            )
            if (invoice.discount != 0.0) {
                TotalRow(
                    columns = columns,
                    label = "优惠",
                    totalText = Money.format(invoice.discount),
                    fontSize = bodySize,
                    fontFamily = fontFamily,
                    borderColor = borderColor,
                    borderWidth = borderWidth,
                    bg = parseColor(params.totalRowBgColor),
                    textColor = Color.Black,
                    drawBottomDivider = false,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // ---------- 页脚：应收金额 ----------
        Text(
            text = "应收金额：${Money.format(invoice.receivable())}（${Money.toChineseAmount(invoice.receivable())}）",
            fontSize = params.footerTextSizeSp.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontFamily = fontFamily,
        )
        if (invoice.showRemark && invoice.remark.isNotBlank()) {
            Text(
                text = "备注：${invoice.remark}",
                fontSize = params.footerTextSizeSp.sp,
                color = Color.Black,
                fontFamily = fontFamily,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (invoice.showAd && invoice.adText.isNotBlank()) {
            Text(
                text = invoice.adText,
                fontSize = params.footerTextSizeSp.sp,
                color = Color.Black,
                fontFamily = fontFamily,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (invoice.showManager && invoice.salesManager.isNotBlank()) {
            val contactSuffix =
                if (invoice.showContactPhone && invoice.contactPhone.isNotBlank()) {
                    "　联系电话：${invoice.contactPhone}"
                } else {
                    ""
                }
            Text(
                text = "客户经理：${invoice.salesManager}$contactSuffix",
                fontSize = params.footerTextSizeSp.sp,
                color = Color.Black,
                fontFamily = fontFamily,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }

        if (invoice.showWatermark && params.watermarkEnabled) {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = invoice.watermarkText.ifBlank { params.watermarkText },
                    fontSize = params.watermarkFontSizeSp.sp,
                    color = parseColor(params.watermarkColor),
                    fontFamily = fontFamily,
                )
            }
        }
    }
}

/**
 * 屏上捕获器：离屏 1×1 占位（不占布局），内部按单据真实尺寸排版并绘制，
 * 通过 GraphicsLayer 截取为位图后回调。用于把 Compose 单据导出为 PNG。
 * 该组件必须在已有 Compose 组合（Activity 窗口）内使用。
 */
@Composable
fun InvoiceBitmapCapture(
    invoice: RenderInvoice,
    params: StyleParams,
    onBitmap: (Bitmap) -> Unit,
) {
    val graphicsLayer = rememberGraphicsLayer()
    Box(
        modifier = Modifier
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints())
                layout(1, 1) { placeable.place(-100000, -100000) }
            }
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .width(params.paperWidthDp.dp)
                .drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                },
        ) {
            InvoiceDocument(invoice = invoice, params = params)
        }
    }
    LaunchedEffect(invoice, params) {
        delay(250)
        withFrameNanos { }
        withFrameNanos { }
        runCatching {
            onBitmap(graphicsLayer.toImageBitmap().asAndroidBitmap())
        }
    }
}

@Composable
private fun TableRow(
    columns: List<ColumnSpec>,
    cells: List<String>,
    aligns: List<TextAlign>,
    fontSize: TextUnit,
    bold: Boolean,
    fontFamily: FontFamily,
    borderColor: Color,
    borderWidth: Dp,
    bg: Color?,
    textColor: Color,
    drawBottomDivider: Boolean,
    amountBold: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        cells.forEachIndexed { index, text ->
            val spec = columns[index]
            TableCell(
                weight = spec.defaultWeight,
                text = text,
                align = aligns[index],
                fontSize = fontSize,
                bold = bold || (spec.id == 8 && amountBold),
                fontFamily = fontFamily,
                borderColor = borderColor,
                borderWidth = borderWidth,
                bg = bg,
                textColor = textColor,
                drawLeftDivider = index > 0,
                drawBottomDivider = drawBottomDivider,
            )
        }
    }
}

/** 汇总行：序号列写标签，中间列合并，金额列写数值（合计 / 优惠）。 */
@Composable
private fun TotalRow(
    columns: List<ColumnSpec>,
    label: String,
    totalText: String,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    borderColor: Color,
    borderWidth: Dp,
    bg: Color?,
    textColor: Color,
    drawBottomDivider: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        columns.forEachIndexed { index, spec ->
            val text = when (spec.id) {
                0 -> label
                8 -> totalText
                else -> ""
            }
            // 合并边界：序号列之后、金额列之前各一条竖线；备注列保留独立单元格（空内容）贯穿到最后一行
            val drawLeft =
                index > 0 && (columns[index - 1].id == 0 || spec.id == 8 || spec.id == 7)
            TableCell(
                weight = spec.defaultWeight,
                text = text,
                align = TextAlign.Center,
                fontSize = fontSize,
                bold = true,
                fontFamily = fontFamily,
                borderColor = borderColor,
                borderWidth = borderWidth,
                bg = bg,
                textColor = textColor,
                drawLeftDivider = drawLeft,
                drawBottomDivider = drawBottomDivider,
            )
        }
    }
}

@Composable
private fun RowScope.TableCell(
    weight: Float,
    text: String,
    align: TextAlign,
    fontSize: TextUnit,
    bold: Boolean,
    fontFamily: FontFamily,
    borderColor: Color,
    borderWidth: Dp,
    bg: Color?,
    textColor: Color,
    drawLeftDivider: Boolean,
    drawBottomDivider: Boolean,
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .heightIn(min = 30.dp)
            .then(if (bg != null) Modifier.background(bg) else Modifier)
            .drawWithContent {
                drawContent()
                if (borderWidth > 0.dp) {
                    val stroke = borderWidth.toPx()
                    if (drawLeftDivider) {
                        drawLine(borderColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = stroke)
                    }
                    if (drawBottomDivider) {
                        drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = stroke)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            fontFamily = fontFamily,
            textAlign = align,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
    }
}

private fun parseColor(hex: String): Color =
    try {
        Color(hex.toColorInt())
    } catch (_: Exception) {
        Color.Black
    }
