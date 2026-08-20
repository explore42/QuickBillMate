package com.example.quickbillmate.ui.report

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class ReportTab(val label: String) {
    TIME("按时间"),
    CUSTOMER("按客户"),
    PRODUCT("按商品"),
}

enum class RangePreset(val label: String) {
    ALL("全部"),
    THIS_MONTH("本月"),
    LAST_MONTH("上月"),
    LAST_3M("近3月"),
    THIS_YEAR("今年"),
    CUSTOM("自定义"),
}

data class ReportSummary(
    val billCount: Int,
    val totalAmount: Double,
    val avgPerBill: Double,
    val customerCount: Int,
)

class ReportViewModel(
    private val repo: AppRepository,
) : ViewModel() {

    var loading by mutableStateOf(true)
        private set
    var preset by mutableStateOf(RangePreset.ALL)
        private set
    var customStart by mutableStateOf<String?>(null)
        private set
    var customEnd by mutableStateOf<String?>(null)
        private set
    var tab by mutableStateOf(ReportTab.TIME)
        private set
    var summary by mutableStateOf<ReportSummary?>(null)
        private set
    var timePoints by mutableStateOf<List<TimePoint>>(emptyList())
        private set
    var customerRanks by mutableStateOf<List<RankEntry>>(emptyList())
        private set
    var productRanks by mutableStateOf<List<RankEntry>>(emptyList())
        private set
    var filteredBills by mutableStateOf<List<Bill>>(emptyList())
        private set

    private var allBills: List<Bill> = emptyList()
    private var itemsByBill: Map<Long, List<BillItem>> = emptyMap()
    private var computeJob: Job? = null

    init {
        viewModelScope.launch {
            val bills = repo.getBillsOnce()
            val items = repo.allBillItemsOnce()
            allBills = bills
            itemsByBill = items.groupBy { it.billId }
            recompute()
            loading = false
        }
    }

    fun selectPreset(value: RangePreset) {
        preset = value
        if (value != RangePreset.CUSTOM) {
            customStart = null
            customEnd = null
        }
        recompute()
    }

    fun selectCustom(start: String, end: String) {
        preset = RangePreset.CUSTOM
        customStart = start
        customEnd = end
        recompute()
    }

    fun selectTab(value: ReportTab) {
        tab = value
    }

    private fun recompute() {
        computeJob?.cancel()
        computeJob = viewModelScope.launch(Dispatchers.Default) {
            val (start, end) = range()
            val bills = ReportAggregator.filterByDate(allBills, start, end)
            val byDay = start != null && end != null &&
                ChronoUnit.DAYS.between(LocalDate.parse(start), LocalDate.parse(end)) <= 31
            val amounts = bills.map { it.id to ReportAggregator.amount(it, itemsByBill[it.id].orEmpty()) }.toMap()
            val total = Money.round2(amounts.values.sum())
            val summaryValue = ReportSummary(
                billCount = bills.size,
                totalAmount = total,
                avgPerBill = if (bills.isEmpty()) 0.0 else Money.round2(total / bills.size),
                customerCount = bills.map { it.customerName.trim() }.filter { it.isNotEmpty() }.distinct().size,
            )
            val timeValue = ReportAggregator.timeSeries(bills, itemsByBill, byDay)
            val customerValue = ReportAggregator.customerStats(bills, itemsByBill)
            val productValue = ReportAggregator.productStats(bills, itemsByBill)
            filteredBills = bills
            summary = summaryValue
            timePoints = timeValue
            customerRanks = customerValue
            productRanks = productValue
        }
    }

    private fun range(): Pair<String?, String?> {
        val today = LocalDate.now()
        return when (preset) {
            RangePreset.ALL -> null to null
            RangePreset.THIS_MONTH -> YearMonth.now().atDay(1).toString() to today.toString()
            RangePreset.LAST_MONTH -> {
                val ym = YearMonth.now().minusMonths(1)
                ym.atDay(1).toString() to ym.atEndOfMonth().toString()
            }
            RangePreset.LAST_3M -> YearMonth.now().minusMonths(2).atDay(1).toString() to today.toString()
            RangePreset.THIS_YEAR -> today.withDayOfYear(1).toString() to today.toString()
            RangePreset.CUSTOM -> customStart to customEnd
        }
    }

    /** 明细单据列表（按维度过滤后的金额映射）。 */
    fun billAmountMap(bills: List<Bill>): Map<Long, Double> =
        bills.associate { it.id to ReportAggregator.amount(it, itemsByBill[it.id].orEmpty()) }

    /** 命中指定商品名的单据（按当前筛选范围）。 */
    fun billsContainingProduct(name: String): List<Bill> =
        filteredBills.filter { bill ->
            itemsByBill[bill.id].orEmpty().any {
                it.name.trim().ifEmpty { "未填写" } == name
            }
        }
}
