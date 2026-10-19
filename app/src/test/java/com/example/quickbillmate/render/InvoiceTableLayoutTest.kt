package com.example.quickbillmate.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 单据图片打印行序：正常行在上、退货行集中到表尾，中间插「退货」标题行。
 */
class InvoiceTableLayoutTest {

    private fun sale(name: String = "销售") = RenderItem(name = name, qty = 1.0, price = 10.0)

    private fun refund(name: String = "退货") = RenderItem(name = name, qty = -1.0, price = 10.0)

    /** 用紧凑字符串描述行序：I<原下标>#<打印序号>，T 表示退货标题行。 */
    private fun describe(rows: List<PrintRow>): String = rows.joinToString(",") { row ->
        when (row) {
            is PrintRow.Item -> "I${row.index}#${row.serial}"
            PrintRow.RefundTitle -> "T"
        }
    }

    @Test
    fun noRefundKeepsOriginalOrderAndSerials() {
        val rows = InvoiceTableLayout.printRows(listOf(sale(), sale(), sale()))

        assertEquals("I0#1,I1#2,I2#3", describe(rows))
    }

    @Test
    fun mixedRowsPutRefundsAfterRefundTitle() {
        val rows = InvoiceTableLayout.printRows(listOf(sale(), refund(), sale(), refund()))

        assertEquals("I0#1,I2#2,T,I1#3,I3#4", describe(rows))
    }

    @Test
    fun allRefundsPrintOneRefundSection() {
        val rows = InvoiceTableLayout.printRows(listOf(refund(), refund(), refund()))

        assertEquals("T,I0#1,I1#2,I2#3", describe(rows))
        assertEquals(1, rows.count { it is PrintRow.RefundTitle })
    }

    @Test
    fun emptyBillProducesNoRows() {
        assertTrue(InvoiceTableLayout.printRows(emptyList()).isEmpty())
    }

    @Test
    fun zeroQtyCountsAsSaleRow() {
        val zero = RenderItem(name = "零数量", qty = 0.0, price = 10.0)
        val rows = InvoiceTableLayout.printRows(listOf(zero, refund()))

        assertEquals("I0#1,T,I1#2", describe(rows))
    }

    @Test
    fun eachGroupKeepsOriginalRelativeOrder() {
        val items = listOf(
            sale("销售甲"),
            refund("退货甲"),
            sale("销售乙"),
            refund("退货乙"),
            sale("销售丙"),
        )
        val rows = InvoiceTableLayout.printRows(items)

        val printedNames = rows.mapNotNull { row ->
            (row as? PrintRow.Item)?.let { items[it.index].name }
        }
        assertEquals(listOf("销售甲", "销售乙", "销售丙", "退货甲", "退货乙"), printedNames)
    }

    @Test
    fun serialsAreContinuousAcrossBothGroups() {
        val items = listOf(refund(), sale(), refund(), sale(), refund(), sale())
        val rows = InvoiceTableLayout.printRows(items)

        val serials = rows.filterIsInstance<PrintRow.Item>().map { it.serial }
        assertEquals(listOf(1, 2, 3, 4, 5, 6), serials)
        assertEquals(items.size, serials.size)
    }
}
