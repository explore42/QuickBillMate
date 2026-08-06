package com.example.quickbillmate.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.util.BillNumber
import com.example.quickbillmate.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeBill(val bill: Bill, val itemCount: Int, val receivable: Double) {
    val docNumber: String
        get() = BillNumber.build(bill.docCode, bill.docDate, bill.docSerial)

    val receivableText: String
        get() = Money.format(receivable)
}

class HomeViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {
    private val _bills = MutableStateFlow<List<HomeBill>>(emptyList())
    val bills: StateFlow<List<HomeBill>> = _bills

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
            repo.observeRecentBills().collect { list ->
                _bills.value = list.map { bill ->
                    val items = repo.getItems(bill.id)
                    val total = items.sumOf {
                        if (it.qty <= 0) 0.0 else Money.round2(it.qty * it.price)
                    }
                    HomeBill(
                        bill = bill,
                        itemCount = items.size,
                        receivable = Math.max(0.0, Money.round2(total - bill.discount)),
                    )
                }
                // 列表刷新后清理已不存在的选中项
                val valid = selectedIds.intersect(list.map { it.id }.toSet())
                if (valid != selectedIds) {
                    selectedIds = valid
                    if (valid.isEmpty()) selectionMode = false
                }
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

    fun selectAll() {
        selectedIds = _bills.value.map { it.bill.id }.toSet()
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
