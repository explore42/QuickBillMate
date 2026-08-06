package com.example.quickbillmate.ui.home

import com.example.quickbillmate.data.db.Bill
import org.junit.Assert.assertEquals
import org.junit.Test

class BillGroupTest {

    private fun homeBill(id: Long, date: String, favorite: Boolean = false): HomeBill {
        val bill = Bill(id = id, docDate = date, favorite = favorite, updatedAt = id)
        return HomeBill(bill = bill, itemCount = 0, receivable = 0.0)
    }

    @Test
    fun `favorite bill appears only in favorite section`() {
        val fav = homeBill(1, "2026-08-02", favorite = true)
        val normal = homeBill(2, "2026-07-15")

        val grouped = groupBills(listOf(normal, fav))

        assertEquals(listOf("♥", "2026-07"), grouped.map { it.key })
        assertEquals(listOf("收藏", "2026年7月"), grouped.map { it.title })
        assertEquals(listOf(1L), grouped[0].bills.map { it.bill.id })
        assertEquals(listOf(2L), grouped[1].bills.map { it.bill.id })
    }

    @Test
    fun `months ordered newest first and blank goes last`() {
        val a = homeBill(1, "2026-08-01")
        val b = homeBill(2, "2026-07-01")
        val c = homeBill(3, "2025-12-01")
        val blank = homeBill(4, "")

        val grouped = groupBills(listOf(blank, c, a, b))

        assertEquals(listOf("2026-08", "2026-07", "2025-12", "其他"), grouped.map { it.key })
    }

    @Test
    fun `no favorites means no favorite section`() {
        val grouped = groupBills(listOf(homeBill(1, "2026-08-01")))
        assertEquals(listOf("2026-08"), grouped.map { it.key })
    }
}
