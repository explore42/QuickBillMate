package com.example.quickbillmate.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import com.example.quickbillmate.util.Money

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
    val disclaimer: String = "收到货物当日点清，如有问题请在2日内联系：",
    val showManager: Boolean = true,
    val showRemark: Boolean = true,
    val showWatermark: Boolean = true,
    val items: List<RenderItem> = emptyList(),
) {
    fun total(): Double = items.sumOf { it.amount() }

    fun receivable(): Double = Math.max(0.0, Money.round2(total() - discount))
}

/**
 * 单据画布渲染器：预览与导出共用同一逻辑。
 * 画布背景恒为白色、文字恒为黑色系，不随 APP 深浅色主题变化。
 */
class InvoiceRenderer {

    /** 渲染位图。widthPx 为目标宽度，导出用 1600（800dp × 2）。 */
    fun render(invoice: RenderInvoice, params: StyleParams, widthPx: Int): Bitmap {
        val scale = widthPx.toFloat() / params.paperWidthDp
        val layout = Layout(invoice, params)
        val heightPx = Math.max(1, (layout.totalHeight * scale).toInt())
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)
        layout.draw(canvas)
        return bitmap
    }

    private class Layout(private val invoice: RenderInvoice, private val params: StyleParams) {
        private val padH = 20f
        private val padTop = 22f
        private val padBottom = 18f
        private val tableWidth = params.paperWidthDp - padH * 2
        private val weights = floatArrayOf(1.0f, 2.7f, 1.8f, 1.0f, 1.3f, 1.6f, 2.0f, 1.8f, 2.0f)
        private val weightSum = weights.sum()
        private val colWidths = weights.map { tableWidth * it / weightSum }

        private val titlePaint = textPaint(params.titleFontSizeSp.toFloat(), params.titleBold, Color.BLACK).apply {
            letterSpacing = if (params.titleFontSizeSp > 0) {
                params.titleLetterSpacing.toFloat() / params.titleFontSizeSp
            } else {
                0f
            }
            textAlign = Paint.Align.CENTER
        }
        private val bodyPaint = textPaint(params.bodyFontSizeSp.toFloat(), false, Color.BLACK)
        private val headerPaint = textPaint(params.headerFontSizeSp.toFloat(), true, parseColor(params.headerTextColor))
        private val footerPaint = textPaint(params.footerTextSizeSp.toFloat(), false, Color.BLACK)
        private val footerBoldPaint = textPaint(params.footerTextSizeSp.toFloat(), true, Color.BLACK)
        private val watermarkPaint = textPaint(
            params.watermarkFontSizeSp.toFloat(),
            false,
            parseColor(params.watermarkColor),
        ).apply {
            textAlign = Paint.Align.CENTER
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = params.tableBorderWidthPx.toFloat()
            color = parseColor(params.tableBorderColor)
        }

        private val headerBg = Paint().apply { color = parseColor(params.headerBgColor) }
        private val totalBg = Paint().apply { color = parseColor(params.totalRowBgColor) }

        private val headerLabels = listOf("序号", "名称", "规格", "单位", "数量", "单价", "金额", "包装", "备注")
        private val rowHeight = 30f
        private val headerHeight = 30f
        private val titleText = buildString {
            if (invoice.companyName.isNotBlank()) append(invoice.companyName)
            append(invoice.titleSuffix)
        }
        private val infoLines = listOf(
            "客户名称" to (invoice.customerName.ifBlank { "—" }),
            "客户电话" to (invoice.customerPhone.ifBlank { "—" }),
            "单据编号" to (invoice.docCode + invoice.docDate.replace("-", "") + invoice.docSerial).ifBlank { "—" },
            "单据日期" to (invoice.docDate.ifBlank { "—" }),
        )

        private val showFooterRemark = invoice.showRemark && invoice.remark.isNotBlank()
        private val showFooterManager = invoice.showManager && invoice.salesManager.isNotBlank()
        private val showDisclaimer = invoice.disclaimer.isNotBlank()
        private val footerLines = mutableListOf<Pair<Boolean, String>>().apply {
            add(false to "优惠金额：${Money.format(invoice.discount)}")
            add(true to "应收金额：${Money.format(invoice.receivable())}（${Money.toChineseAmount(invoice.receivable())}）")
            if (showFooterRemark) add(false to "备注：${invoice.remark}")
            if (showFooterManager) add(false to "业务经理：${invoice.salesManager}")
            if (showDisclaimer) {
                val phone = invoice.contactPhone.ifBlank { "" }
                add(false to (invoice.disclaimer + if (phone.isNotBlank()) " $phone（微信同号）" else ""))
            }
        }

        val totalHeight: Float = run {
            var y = padTop
            y += titleBlockHeight()
            y += infoBlockHeight()
            y += tableHeight()
            y += footerBlockHeight()
            if (showWatermark()) y += watermarkBlockHeight()
            y + padBottom
        }

        fun draw(canvas: Canvas) {
            var y = padTop
            y = drawTitle(canvas, y)
            y = drawInfo(canvas, y)
            y = drawTable(canvas, y)
            y = drawFooter(canvas, y)
            if (showWatermark()) y = drawWatermark(canvas, y)
        }

        private fun showWatermark(): Boolean =
            invoice.showWatermark && params.watermarkEnabled

        private fun titleBlockHeight(): Float =
            params.titleFontSizeSp * 1.4f + (if (params.titleUnderline) 12f else 6f)

        private fun infoBlockHeight(): Float =
            infoLines.size * (params.bodyFontSizeSp * 1.5f) + 6f

        private fun tableHeight(): Float =
            headerHeight + invoice.items.size * rowHeight + rowHeight + 8f

        private fun footerBlockHeight(): Float =
            footerLines.size * (params.footerTextSizeSp * 1.6f) + 8f

        private fun watermarkBlockHeight(): Float =
            params.watermarkFontSizeSp * 1.7f + 4f

        private fun drawTitle(canvas: Canvas, startY: Float): Float {
            if (titleText.isBlank()) return startY + 6f
            val center = params.paperWidthDp / 2f
            val titleH = params.titleFontSizeSp * 1.4f
            val baseline = startY + titleH
            canvas.drawText(titleText, center, baseline, titlePaint)
            if (params.titleUnderline) {
                val w = titlePaint.measureText(titleText)
                val underlineY = baseline + 7f
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = 1.6f
                    color = parseColor(params.titleUnderlineColor)
                }
                canvas.drawLine(center - w / 2f, underlineY, center + w / 2f, underlineY, linePaint)
            }
            return startY + titleBlockHeight()
        }

        private fun drawInfo(canvas: Canvas, startY: Float): Float {
            var y = startY
            val lineH = params.bodyFontSizeSp * 1.5f
            for ((label, value) in infoLines) {
                canvas.drawText(label, padH, y + lineH, bodyPaint)
                canvas.drawText(value, padH + params.infoLabelWidthPx, y + lineH, bodyPaint)
                y += lineH
            }
            return startY + infoBlockHeight()
        }

        private fun drawTable(canvas: Canvas, startY: Float): Float {
            val left = padH
            val right = padH + tableWidth
            var y = startY

            // 表头
            drawRow(
                canvas,
                left,
                y,
                headerHeight,
                headerBg,
                headerLabels.mapIndexed { i, label ->
                    CellText(label, colWidths[i], alignForColumn(i), headerPaint)
                },
            )
            y += headerHeight

            // 数据行
            invoice.items.forEachIndexed { index, item ->
                val cells = listOf(
                    CellText((index + 1).toString(), colWidths[0], Paint.Align.CENTER, bodyPaint),
                    CellText(item.name, colWidths[1], Paint.Align.LEFT, bodyPaint),
                    CellText(item.spec, colWidths[2], Paint.Align.LEFT, bodyPaint),
                    CellText(item.unit, colWidths[3], Paint.Align.CENTER, bodyPaint),
                    CellText(Money.format(item.qty), colWidths[4], Paint.Align.RIGHT, bodyPaint),
                    CellText(Money.format(item.price), colWidths[5], Paint.Align.RIGHT, bodyPaint),
                    CellText(Money.format(item.amount()), colWidths[6], Paint.Align.RIGHT, amountPaint()),
                    CellText(item.pack, colWidths[7], Paint.Align.LEFT, bodyPaint),
                    CellText(item.note, colWidths[8], Paint.Align.LEFT, bodyPaint),
                )
                drawRow(canvas, left, y, rowHeight, null, cells)
                y += rowHeight
            }

            // 合计行
            val totalCells = listOf(
                CellText("", colWidths[0], Paint.Align.CENTER, bodyPaint),
                CellText("合计", colWidths[1], Paint.Align.LEFT, footerBoldPaint),
                CellText("", colWidths[2], Paint.Align.LEFT, bodyPaint),
                CellText("", colWidths[3], Paint.Align.CENTER, bodyPaint),
                CellText("", colWidths[4], Paint.Align.RIGHT, bodyPaint),
                CellText("", colWidths[5], Paint.Align.RIGHT, bodyPaint),
                CellText(Money.format(invoice.total()), colWidths[6], Paint.Align.RIGHT, footerBoldPaint),
                CellText("", colWidths[7], Paint.Align.LEFT, bodyPaint),
                CellText("", colWidths[8], Paint.Align.LEFT, bodyPaint),
            )
            drawRow(canvas, left, y, rowHeight, totalBg, totalCells)
            y += rowHeight

            // 外框
            if (params.tableBorderWidthPx > 0) {
                canvas.drawRect(RectF(left, startY, right, y), borderPaint)
            }
            return startY + tableHeight()
        }

        private fun amountPaint(): TextPaint {
            val p = textPaint(params.bodyFontSizeSp.toFloat(), params.amountBold, Color.BLACK)
            p.textAlign = Paint.Align.RIGHT
            return p
        }

        private fun drawFooter(canvas: Canvas, startY: Float): Float {
            var y = startY + 4f
            val lineH = params.footerTextSizeSp * 1.6f
            for ((bold, text) in footerLines) {
                val p = if (bold) footerBoldPaint else footerPaint
                canvas.drawText(text, padH, y + lineH, p)
                y += lineH
            }
            return startY + footerBlockHeight()
        }

        private fun drawWatermark(canvas: Canvas, startY: Float): Float {
            val h = params.watermarkFontSizeSp * 1.7f
            canvas.drawText(params.watermarkText, params.paperWidthDp / 2f, startY + h, watermarkPaint)
            return startY + watermarkBlockHeight()
        }

        private fun drawRow(
            canvas: Canvas,
            left: Float,
            top: Float,
            height: Float,
            fill: Paint?,
            cells: List<CellText>,
        ) {
            if (fill != null) canvas.drawRect(left, top, left + tableWidth, top + height, fill)
            var x = left
            cells.forEach { cell ->
                val cx = x + cell.width
                if (params.tableBorderWidthPx > 0) {
                    canvas.drawRect(RectF(x, top, cx, top + height), borderPaint)
                }
                val text = ellipsize(cell.text, cell.paint, cell.width - 8f)
                val baseline = top + height * 0.5f - (cell.paint.descent() + cell.paint.ascent()) / 2f
                when (cell.align) {
                    Paint.Align.LEFT -> canvas.drawText(text, x + 4f, baseline, cell.paint)
                    Paint.Align.RIGHT -> canvas.drawText(text, cx - 4f, baseline, cell.paint)
                    Paint.Align.CENTER -> canvas.drawText(text, (x + cx) / 2f, baseline, cell.paint)
                }
                x = cx
            }
        }

        private fun alignForColumn(i: Int): Paint.Align = when (i) {
            0 -> Paint.Align.CENTER
            4, 5, 6 -> Paint.Align.RIGHT
            else -> Paint.Align.LEFT
        }

        private fun ellipsize(text: String, paint: TextPaint, maxWidth: Float): String {
            if (maxWidth <= 0) return text
            return TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END).toString()
        }

        private fun textPaint(sizeSp: Float, bold: Boolean, color: Int): TextPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = sizeSp
                this.color = color
                val family = if (params.fontFamily == "system_serif") Typeface.SERIF else Typeface.SANS_SERIF
                typeface = Typeface.create(family, if (bold) Typeface.BOLD else Typeface.NORMAL)
            }

        private fun parseColor(hex: String): Int =
            try {
                Color.parseColor(hex)
            } catch (_: Exception) {
                Color.BLACK
            }
    }

    private data class CellText(val text: String, val width: Float, val align: Paint.Align, val paint: TextPaint)
}
