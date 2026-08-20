package com.example.quickbillmate.ui.report

import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import com.example.quickbillmate.util.Money

/** 时间序列上的一个点（按月或按天）。 */
data class TimePoint(
    val key: String,
    val label: String,
    val billCount: Int,
    val amount: Double,
)

/** 客户 / 商品维度排名条目。 */
data class RankEntry(
    val name: String,
    val billCount: Int,
    val qty: Double,
    val amount: Double,
)

/**
 * 报表聚合纯函数（金额口径与单据详情一致：max(0, Σqty×price − discount)）。
 */
object ReportAggregator {
    fun amount(bill: Bill, items: List<BillItem>): Double {
        val total = items.sumOf { if (it.qty <= 0) 0.0 else Money.round2(it.qty * it.price) }
        return Math.max(0.0, Money.round2(total - bill.discount))
    }

    fun filterByDate(bills: List<Bill>, start: String?, end: String?): List<Bill> {
        if (start == null && end == null) return bills
        return bills.filter { b ->
            (start == null || b.docDate >= start) && (end == null || b.docDate <= end)
        }
    }

    /** 按月（或按天）聚合单据数与金额，按时间升序。 */
    fun timeSeries(
        bills: List<Bill>,
        itemsByBill: Map<Long, List<BillItem>>,
        byDay: Boolean,
    ): List<TimePoint> {
        val grouped = bills.groupBy { if (byDay) it.docDate else it.docDate.take(7) }
        return grouped.entries.map { (key, list) ->
            TimePoint(
                key = key,
                label = if (byDay) key.takeLast(5) else monthLabel(key),
                billCount = list.size,
                amount = Money.round2(list.sumOf { amount(it, itemsByBill[it.id].orEmpty()) }),
            )
        }.sortedBy { it.key }
    }

    /** 按客户聚合（单据数 + 金额），按金额降序。 */
    fun customerStats(
        bills: List<Bill>,
        itemsByBill: Map<Long, List<BillItem>>,
    ): List<RankEntry> {
        val grouped = bills.groupBy { it.customerName.trim().ifEmpty { "未填写" } }
        return grouped.map { (name, list) ->
            RankEntry(
                name = name,
                billCount = list.size,
                qty = 0.0,
                amount = Money.round2(list.sumOf { amount(it, itemsByBill[it.id].orEmpty()) }),
            )
        }.sortedWith(compareByDescending<RankEntry> { it.amount }.thenBy { it.name })
    }

    /** 按商品聚合（销量 + 金额 + 涉及单据数），按金额降序。 */
    fun productStats(
        bills: List<Bill>,
        itemsByBill: Map<Long, List<BillItem>>,
    ): List<RankEntry> {
        val byName = LinkedHashMap<String, MutableList<Pair<Bill, BillItem>>>()
        bills.forEach { bill ->
            itemsByBill[bill.id].orEmpty().forEach { item ->
                val key = item.name.trim().ifEmpty { "未填写" }
                byName.getOrPut(key) { mutableListOf() }.add(bill to item)
            }
        }
        return byName.map { (name, pairs) ->
            RankEntry(
                name = name,
                billCount = pairs.map { it.first.id }.distinct().size,
                qty = Money.round2(pairs.sumOf { if (it.second.qty <= 0) 0.0 else it.second.qty }),
                amount = Money.round2(
                    pairs.sumOf {
                        if (it.second.qty <= 0) 0.0 else Money.round2(it.second.qty * it.second.price)
                    }
                ),
            )
        }.sortedWith(compareByDescending<RankEntry> { it.amount }.thenBy { it.name })
    }

    /** 取 Top N，其余合并为“其他”（用于占比展示）。 */
    fun topWithOther(entries: List<RankEntry>, topN: Int = 5): List<RankEntry> {
        if (entries.size <= topN) return entries
        val top = entries.take(topN)
        val rest = entries.drop(topN)
        return top + RankEntry(
            name = "其他",
            billCount = rest.sumOf { it.billCount },
            qty = Money.round2(rest.sumOf { it.qty }),
            amount = Money.round2(rest.sumOf { it.amount }),
        )
    }

    fun monthLabel(key: String): String {
        val parts = key.split("-")
        return if (parts.size >= 2) "${parts[0]}年${parts[1].toIntOrNull() ?: parts[1]}月" else key
    }
}
