package com.example.quickbillmate.ui.products

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.util.Pinyin

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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ProductsViewModel(
    private val repo: AppRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    var queryText by mutableStateOf("")
        private set

    /** 商品单位预设（内置 + 设置页自定义），进入商品页时刷新。 */
    var presetUnits by mutableStateOf(SettingsStore.BUILTIN_UNITS + repo.settings.customUnits)
        private set

    fun refreshUnitPresets() {
        presetUnits = SettingsStore.BUILTIN_UNITS + repo.settings.customUnits
    }

    val products: StateFlow<List<Product>> = query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { repo.observeProducts(it) }
        .map { list -> Pinyin.sortByPinyinLetter(list, { it.favorite }, { it.pinyinInitial }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectionMode by mutableStateOf(false)
        private set
    var selectedIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var pendingDeleteIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var copyMessage by mutableStateOf<String?>(null)
        private set

    fun setQuery(value: String) {
        queryText = value
        query.value = value
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch { repo.saveProduct(product) }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { repo.deleteProduct(product) }
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
        selectedIds = products.value.map { it.id }.toSet()
    }

    /** 顶栏全选切换：全部已选则清空（留在多选模式），否则选中全部可见商品。 */
    fun toggleSelectAll() {
        val visibleIds = products.value.map { it.id }
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
            repo.copyProducts(products.value.filter { it.id in ids })
            copyMessage = "已复制 ${ids.size} 条"
            exitSelection()
        }
    }

    fun editSelected(onEdit: (Product) -> Unit) {
        if (selectedIds.size == 1) {
            val item = products.value.firstOrNull { it.id == selectedIds.first() }
            if (item != null) {
                exitSelection()
                onEdit(item)
            }
        }
    }

    fun requestDelete() {
        if (selectedIds.isNotEmpty()) pendingDeleteIds = selectedIds
    }

    fun confirmDelete() {
        val ids = pendingDeleteIds
        viewModelScope.launch {
            repo.deleteProducts(products.value.filter { it.id in ids })
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
}
