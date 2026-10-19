package com.example.quickbillmate.render

/** 单据表格的打印行：商品行（指向 [RenderInvoice.items] 的下标）或退货分区标题行。 */
sealed interface PrintRow {
    /**
     * 商品行；[index] 为原单据行列表中的下标，[serial] 为按打印顺序连续分配的序号（从 1 开始）。
     */
    data class Item(val index: Int, val serial: Int) : PrintRow

    /** 退货分区标题行，仅在有退货行（数量 < 0）时出现。 */
    data object RefundTitle : PrintRow
}

/**
 * 单据表格打印行序：正常行（数量 ≥ 0）在上、退货行（数量 < 0）在下，两组各自保持原相对顺序；
 * 两组之间插入一行「退货」标题，方便把退货和正常销售区分开。
 *
 * 没有退货行时输出与原始顺序完全一致；编辑页/详情页的排序不受影响（只作用于单据图片与导出）。
 */
object InvoiceTableLayout {
    fun printRows(items: List<RenderItem>): List<PrintRow> {
        val saleIndices = ArrayList<Int>(items.size)
        val refundIndices = ArrayList<Int>()
        items.forEachIndexed { index, item ->
            if (item.qty < 0.0) refundIndices.add(index) else saleIndices.add(index)
        }
        val rows = ArrayList<PrintRow>(items.size + 1)
        var serial = 0
        saleIndices.forEach { rows.add(PrintRow.Item(it, ++serial)) }
        if (refundIndices.isNotEmpty()) {
            rows.add(PrintRow.RefundTitle)
            refundIndices.forEach { rows.add(PrintRow.Item(it, ++serial)) }
        }
        return rows
    }
}
