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
        .map { list -> Pinyin.sortByPinyinLetter(list, { it.favorite }, { Pinyin.firstLetter(it.name) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var importing by mutableStateOf(false)
        private set
    var importResult by mutableStateOf<ProductImportResult?>(null)
        private set
    var importError by mutableStateOf<String?>(null)
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
                    repo.exportProductsToDownloads(app)
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
