package com.example.quickbillmate.ui.home

import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeBillsTest {

    private fun bill(id: Long, discount: Double = 0.0) = Bill(id = id, discount = discount)

    private fun item(
        billId: Long,
        name: String = "商品",
        qty: Double = 1.0,
        price: Double = 0.0,
    ) = BillItem(billId = billId, name = name, qty = qty, price = price)

    @Test
    fun `empty inputs produce empty list`() {
        assertTrue(buildHomeBills(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `groups items per bill and computes count names and receivable`() {
        val bills = listOf(bill(1), bill(2))
        val items = listOf(
            item(1, "腻子粉", qty = 2.0, price = 35.0),
            item(1, "墙锢", qty = 1.0, price = 65.0),
            item(2, "网格布", qty = 10.0, price = 42.0),
        )

        val result = buildHomeBills(bills, items)

        assertEquals(2, result.size)
        val first = result.first { it.bill.id == 1L }
        assertEquals(2, first.itemCount)
        assertEquals(listOf("腻子粉", "墙锢"), first.itemNames)
        assertEquals(135.0, first.receivable, 0.001)
        val second = result.first { it.bill.id == 2L }
        assertEquals(1, second.itemCount)
        assertEquals(420.0, second.receivable, 0.001)
    }

    @Test
    fun `non-positive quantity contributes zero amount`() {
        val bills = listOf(bill(1))
        val items = listOf(
            item(1, "A", qty = 0.0, price = 100.0),
            item(1, "B", qty = -1.0, price = 50.0),
        )

        val result = buildHomeBills(bills, items)

        assertEquals(0.0, result.single().receivable, 0.001)
    }

    @Test
    fun `discount is subtracted and receivable never negative`() {
        val bills = listOf(bill(1, discount = 20.0), bill(2, discount = 999.0))
        val items = listOf(
            item(1, "A", qty = 2.0, price = 35.0),
            item(2, "B", qty = 1.0, price = 10.0),
        )

        val result = buildHomeBills(bills, items)

        assertEquals(50.0, result.first { it.bill.id == 1L }.receivable, 0.001)
        assertEquals(0.0, result.first { it.bill.id == 2L }.receivable, 0.001)
    }
}
