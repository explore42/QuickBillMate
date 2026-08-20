package com.example.quickbillmate.ui.report

import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportAggregatorTest {

    private fun bill(id: Long, customer: String, date: String, discount: Double = 0.0) =
        Bill(id = id, customerName = customer, docDate = date, discount = discount)

    private fun item(billId: Long, name: String, qty: Double, price: Double) =
        BillItem(billId = billId, name = name, qty = qty, price = price)

    private val bills = listOf(
        bill(1, "张老板", "2026-08-01"),
        bill(2, "李老板", "2026-08-15", discount = 5.0),
        bill(3, "张老板", "2026-07-20"),
    )
    private val itemsByBill = mapOf(
        1L to listOf(item(1, "腻子粉", 2.0, 35.0)),
        2L to listOf(item(2, "墙锢", 1.0, 65.0), item(2, "腻子粉", 1.0, 35.0)),
        3L to listOf(item(3, "网格布", 4.0, 42.0)),
    )

    @Test
    fun `金额口径与单据详情一致`() {
        assertEquals(70.0, ReportAggregator.amount(bills[0], itemsByBill[1]!!), 0.0001)
        assertEquals(95.0, ReportAggregator.amount(bills[1], itemsByBill[2]!!), 0.0001)
        assertEquals(168.0, ReportAggregator.amount(bills[2], itemsByBill[3]!!), 0.0001)
    }

    @Test
    fun `按日期过滤`() {
        val filtered = ReportAggregator.filterByDate(bills, "2026-08-01", "2026-08-31")
        assertEquals(listOf(1L, 2L), filtered.map { it.id })
    }

    @Test
    fun `时间序列按月聚合`() {
        val points = ReportAggregator.timeSeries(bills, itemsByBill, byDay = false)
        assertEquals(2, points.size)
        assertEquals("2026-07", points[0].key)
        assertEquals(168.0, points[0].amount, 0.0001)
        assertEquals(1, points[0].billCount)
        assertEquals("2026-08", points[1].key)
        assertEquals(165.0, points[1].amount, 0.0001)
        assertEquals(2, points[1].billCount)
    }

    @Test
    fun `客户维度聚合`() {
        val stats = ReportAggregator.customerStats(bills, itemsByBill)
        assertEquals(2, stats.size)
        assertEquals("张老板", stats[0].name)
        assertEquals(238.0, stats[0].amount, 0.0001)
        assertEquals(2, stats[0].billCount)
        assertEquals("李老板", stats[1].name)
        assertEquals(95.0, stats[1].amount, 0.0001)
    }

    @Test
    fun `商品维度聚合销量与金额`() {
        val stats = ReportAggregator.productStats(bills, itemsByBill)
        val niFen = stats.first { it.name == "腻子粉" }
        assertEquals(3.0, niFen.qty, 0.0001)
        assertEquals(105.0, niFen.amount, 0.0001)
        assertEquals(2, niFen.billCount)
        val wangGe = stats.first { it.name == "网格布" }
        assertEquals(4.0, wangGe.qty, 0.0001)
        assertEquals(168.0, wangGe.amount, 0.0001)
    }

    @Test
    fun `空数据与TopN合并`() {
        assertEquals(emptyList<TimePoint>(), ReportAggregator.timeSeries(emptyList(), emptyMap(), false))
        val top = ReportAggregator.topWithOther(
            listOf(
                RankEntry("A", 1, 0.0, 100.0),
                RankEntry("B", 1, 0.0, 60.0),
                RankEntry("C", 1, 0.0, 30.0),
            ),
            topN = 2,
        )
        assertEquals(3, top.size)
        assertEquals("其他", top[2].name)
        assertEquals(30.0, top[2].amount, 0.0001)
    }
}
