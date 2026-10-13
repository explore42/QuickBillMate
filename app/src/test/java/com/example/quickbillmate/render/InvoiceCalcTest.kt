package com.example.quickbillmate.render

import org.junit.Assert.assertEquals
import org.junit.Test

class InvoiceCalcTest {

    @Test
    fun `行金额等于数量乘单价`() {
        assertEquals(350.0, RenderItem(qty = 10.0, price = 35.0).amount(), 1e-9)
        assertEquals(123.46, RenderItem(qty = 3.0, price = 41.153).amount(), 1e-9)
    }

    @Test
    fun `数量为零时金额为零负数时金额为负`() {
        assertEquals(0.0, RenderItem(qty = 0.0, price = 35.0).amount(), 1e-9)
        assertEquals(-70.0, RenderItem(qty = -2.0, price = 35.0).amount(), 1e-9)
    }

    @Test
    fun `退货单合计与应收为负数`() {
        val invoice = RenderInvoice(
            items = listOf(
                RenderItem(qty = 10.0, price = 35.0),
                RenderItem(qty = -3.0, price = 35.0),
            )
        )
        assertEquals(245.0, invoice.total(), 1e-9)
        val refund = RenderInvoice(items = listOf(RenderItem(qty = -3.0, price = 35.0)))
        assertEquals(-105.0, refund.total(), 1e-9)
        assertEquals(-105.0, refund.receivable(), 1e-9)
    }

    @Test
    fun `合计金额为行金额之和`() {
        val invoice = RenderInvoice(
            items = listOf(
                RenderItem(qty = 10.0, price = 35.0),
                RenderItem(qty = 5.0, price = 28.0),
                RenderItem(qty = 2.0, price = 350.0),
            )
        )
        assertEquals(10 * 35.0 + 5 * 28.0 + 2 * 350.0, invoice.total(), 1e-9)
    }

    @Test
    fun `应收金额等于合计减优惠`() {
        val invoice = RenderInvoice(discount = 12.0, items = listOf(RenderItem(qty = 10.0, price = 35.0)))
        assertEquals(338.0, invoice.receivable(), 1e-9)
    }

    @Test
    fun `优惠大于合计时应收为负数不再兜底为零`() {
        val invoice = RenderInvoice(discount = 999.0, items = listOf(RenderItem(qty = 1.0, price = 10.0)))
        assertEquals(-989.0, invoice.receivable(), 1e-9)
    }

    @Test
    fun `空商品合计为零`() {
        val invoice = RenderInvoice()
        assertEquals(0.0, invoice.total(), 1e-9)
        assertEquals(0.0, invoice.receivable(), 1e-9)
    }
}
