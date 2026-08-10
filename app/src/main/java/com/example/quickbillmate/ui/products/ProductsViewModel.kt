package com.example.quickbillmate.ui.products

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.importexport.ProductImportResult
import com.example.quickbillmate.importexport.ProductJsonCodec
import com.example.quickbillmate.importexport.ProductJsonException
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
import java.io.File

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ProductsViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    var queryText by mutableStateOf("")
        private set

    val products: StateFlow<List<Product>> = query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { repo.observeProducts(it) }
        .map { list -> Pinyin.sortByPinyinLetter(list, { it.favorite }, { it.pinyinInitial }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var importing by mutableStateOf(false)
        private set
    var importResult by mutableStateOf<ProductImportResult?>(null)
        private set
    var importError by mutableStateOf<String?>(null)
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

    var exporting by mutableStateOf(false)
        private set
    var exportFileName by mutableStateOf<String?>(null)
        private set
    var exportError by mutableStateOf<String?>(null)
        private set
    var shareJsonUri by mutableStateOf<Uri?>(null)
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

    /** 分组全选：与已有选中合并，不替换。 */
    fun selectGroup(ids: Set<Long>) {
        selectionMode = true
        selectedIds = selectedIds + ids
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

    fun exportSelected() {
        val ids = selectedIds
        viewModelScope.launch {
            val list = products.value.filter { it.id in ids }
            exportError = null
            exportMessage = null
            try {
                exportMessage = withContext(Dispatchers.IO) {
                    repo.exportProductsToDownloads(app, list)
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

    fun consumeExportMessage() {
        exportMessage = null
    }

    fun consumeExportError() {
        exportError = null
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            importing = true
            importResult = null
            importError = null
            try {
                importResult = withContext(Dispatchers.IO) {
                    repo.importProductsFromUri(app, uri)
                }
            } catch (e: ProductJsonException) {
                importError = e.message
            } finally {
                importing = false
            }
        }
    }

    fun exportProducts() {
        viewModelScope.launch {
            exporting = true
            exportFileName = null
            exportError = null
            try {
                exportFileName = withContext(Dispatchers.IO) {
                    repo.exportProductsToDownloads(app, repo.getProducts())
                }
                if (exportFileName == null) exportError = "导出失败，请检查存储空间"
            } catch (_: Exception) {
                exportError = "导出失败，请检查存储空间"
            } finally {
                exporting = false
            }
        }
    }

    /** 把导出的 JSON 写入缓存以便分享。 */
    fun shareExportedJson() {
        viewModelScope.launch {
            val products = repo.getProducts()
            val text = ProductJsonCodec.export(products)
            val fileName = "products_" + System.currentTimeMillis() + ".json"
            shareJsonUri = withContext(Dispatchers.IO) {
                val dir = File(app.cacheDir, "shared").apply { mkdirs() }
                val file = File(dir, fileName)
                file.writeText(text, Charsets.UTF_8)
                androidx.core.content.FileProvider.getUriForFile(
                    app,
                    "com.example.quickbillmate.fileprovider",
                    file,
                )
            }
        }
    }

    fun consumeImportResult() {
        importResult = null
    }

    fun consumeImportError() {
        importError = null
    }

    fun consumeExport() {
        exportFileName = null
        exportError = null
    }

    fun consumeShareJson() {
        shareJsonUri = null
    }
}
