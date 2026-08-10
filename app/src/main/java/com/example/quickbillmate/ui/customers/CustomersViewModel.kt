package com.example.quickbillmate.ui.customers

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.util.Pinyin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CustomersViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    var queryText by mutableStateOf("")
        private set

    var selectionMode by mutableStateOf(false)
        private set
    var selectedIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var pendingDeleteIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var copyMessage by mutableStateOf<String?>(null)
        private set
    var exportMessage by mutableStateOf<String?>(null)
        private set
    var exportError by mutableStateOf<String?>(null)
        private set

    val customers: StateFlow<List<Customer>> = query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { repo.observeCustomers(it) }
        .map { list -> Pinyin.sortByPinyinLetter(list, { it.favorite }, { it.pinyinInitial }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(value: String) {
        queryText = value
        query.value = value
    }

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch { repo.saveCustomer(customer) }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch { repo.deleteCustomer(customer) }
    }

    fun enterSelection(id: Long) {
        selectionMode = true
        selectedIds = setOf(id)
    }

    fun toggleSelection(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        if (selectedIds.isEmpty()) selectionMode = false
    }

    /** 全选当前可见（筛选后）列表。 */
    fun selectAll() {
        selectedIds = customers.value.map { it.id }.toSet()
    }

    /** 顶栏全选切换：全部已选则清空（留在多选模式），否则选中全部可见客户。 */
    fun toggleSelectAll() {
        val visibleIds = customers.value.map { it.id }
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

    fun copySelected() {
        val ids = selectedIds
        viewModelScope.launch {
            repo.copyCustomers(customers.value.filter { it.id in ids })
            copyMessage = "已复制 ${ids.size} 条"
            exitSelection()
        }
    }

    fun editSelected(onEdit: (Customer) -> Unit) {
        if (selectedIds.size == 1) {
            val item = customers.value.firstOrNull { it.id == selectedIds.first() }
            if (item != null) {
                exitSelection()
                onEdit(item)
            }
        }
    }

    fun exportSelected() {
        val ids = selectedIds
        viewModelScope.launch {
            val list = customers.value.filter { it.id in ids }
            exportError = null
            exportMessage = null
            try {
                exportMessage = withContext(Dispatchers.IO) {
                    repo.exportCustomersToDownloads(app, list)
                }
                if (exportMessage == null) exportError = "导出失败，请检查存储空间"
            } catch (_: Exception) {
                exportError = "导出失败，请检查存储空间"
            }
            exitSelection()
        }
    }

    fun requestDelete() {
        if (selectedIds.isNotEmpty()) pendingDeleteIds = selectedIds
    }

    fun confirmDelete() {
        val ids = pendingDeleteIds
        viewModelScope.launch {
            repo.deleteCustomers(customers.value.filter { it.id in ids })
            exitSelection()
            pendingDeleteIds = emptySet()
        }
    }

    fun cancelDelete() {
        pendingDeleteIds = emptySet()
    }

    fun consumeCopyMessage() {
        copyMessage = null
    }

    fun consumeExportMessage() {
        exportMessage = null
    }

    fun consumeExportError() {
        exportError = null
    }
}
