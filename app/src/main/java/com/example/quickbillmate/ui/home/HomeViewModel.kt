package com.example.quickbillmate.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.util.BillNumber
import com.example.quickbillmate.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeBill(
    val bill: Bill,
    val itemCount: Int,
    val receivable: Double,
    val itemNames: List<String> = emptyList(),
) {
    val docNumber: String
        get() = BillNumber.build(bill.docCode, bill.docDate, bill.docSerial)

    val receivableText: String
        get() = Money.format(receivable)
}

/** 把单据与全部商品行映射为单据列表展示数据（纯函数，便于单元测试）。 */
fun buildHomeBills(bills: List<Bill>, items: List<BillItem>): List<HomeBill> {
    val itemsByBill = items.groupBy { it.billId }
    return bills.map { bill ->
        val billItems = itemsByBill[bill.id].orEmpty()
        val total = billItems.sumOf {
            if (it.qty <= 0) 0.0 else Money.round2(it.qty * it.price)
        }
        HomeBill(
            bill = bill,
            itemCount = billItems.size,
            receivable = Math.max(0.0, Money.round2(total - bill.discount)),
            itemNames = billItems.map { it.name },
        )
    }
}

class HomeViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {
    private val _bills = MutableStateFlow<List<HomeBill>>(emptyList())
    val bills: StateFlow<List<HomeBill>> = _bills

    var searchQuery by mutableStateOf("")
        private set

    private var allBills: List<HomeBill> = emptyList()

    var selectionMode by mutableStateOf(false)
        private set
    var selectedIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var copyMessage by mutableStateOf<String?>(null)
        private set
    var exportMessage by mutableStateOf<String?>(null)
        private set
    var pendingDeleteIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    init {
        viewModelScope.launch {
            combine(repo.observeRecentBills(), repo.observeAllBillItems()) { bills, items ->
                buildHomeBills(bills, items)
            }
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default)
                .collect { list ->
                    allBills = list
                    applyFilter()
                }
        }
    }

    fun onSearchQueryChange(value: String) {
        searchQuery = value
        applyFilter()
    }

    /** 按商品名 / 客户姓名电话 / 单据日期筛选。 */
    private fun applyFilter() {
        val q = searchQuery.trim()
        _bills.value = if (q.isBlank()) {
            allBills
        } else {
            allBills.filter { home ->
                home.bill.customerName.contains(q) ||
                    home.bill.customerPhone.contains(q) ||
                    home.bill.docDate.contains(q) ||
                    home.itemNames.any { it.contains(q) }
            }
        }
    }

    // ---------- 多选 ----------

    fun enterSelection(billId: Long) {
        selectionMode = true
        selectedIds = setOf(billId)
    }

    fun toggleSelection(billId: Long) {
        selectedIds = if (billId in selectedIds) selectedIds - billId else selectedIds + billId
        if (selectedIds.isEmpty()) {
            selectionMode = false
        }
    }

    /** 全选当前可见（筛选后）列表。 */
    fun selectAll() {
        selectedIds = _bills.value.map { it.bill.id }.toSet()
    }

    /** 顶栏全选切换：全部已选则清空（留在多选模式），否则选中全部可见单据。 */
    fun toggleSelectAll() {
        val visibleIds = _bills.value.map { it.bill.id }
        val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedIds }
        selectedIds = if (allVisibleSelected) emptySet() else visibleIds.toSet()
        selectionMode = true
    }

    /** 分组全选切换：组内全部已选则取消该组，否则与已有选中合并。 */
    fun selectGroup(ids: Set<Long>) {
        selectionMode = true
        selectedIds =
            if (ids.isNotEmpty() && ids.all { it in selectedIds }) selectedIds - ids else selectedIds + ids
    }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    // ---------- 操作 ----------

    fun requestDelete() {
        if (selectedIds.isNotEmpty()) pendingDeleteIds = selectedIds
    }

    fun cancelDelete() {
        pendingDeleteIds = emptySet()
    }

    fun confirmDelete() {
        val ids = pendingDeleteIds
        viewModelScope.launch {
            ids.forEach { id ->
                repo.getBill(id)?.let { repo.deleteBill(it) }
            }
            exitSelection()
            pendingDeleteIds = emptySet()
        }
    }

    fun copySelected() {
        val ids = selectedIds
        viewModelScope.launch {
            var count = 0
            ids.forEach { id ->
                if (repo.copyBill(id) != null) count++
            }
            copyMessage = "已复制 $count 条单据"
            exitSelection()
        }
    }

    fun editSelected(onEdit: (Long) -> Unit) {
        if (selectedIds.size == 1) {
            val id = selectedIds.first()
            exitSelection()
            onEdit(id)
        }
    }

    fun exportSelected() {
        val ids = selectedIds
        viewModelScope.launch {
            var ok = 0
            var fail = 0
            withContext(Dispatchers.IO) {
                ids.forEach { id ->
                    val bill = repo.getBill(id)
                    if (bill != null) {
                        val items = repo.getItems(id)
                        if (repo.exportBillToGallery(app, bill, items)) ok++ else fail++
                    }
                }
            }
            exportMessage = if (fail > 0) {
                "导出完成：成功 $ok 条，失败 $fail 条"
            } else {
                "已导出 $ok 条到相册"
            }
            exitSelection()
        }
    }

    fun consumeCopyMessage() {
        copyMessage = null
    }

    fun consumeExportMessage() {
        exportMessage = null
    }
}
