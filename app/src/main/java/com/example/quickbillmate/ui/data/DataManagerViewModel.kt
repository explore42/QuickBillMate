package com.example.quickbillmate.ui.data

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.importexport.BillJsonCodec
import com.example.quickbillmate.importexport.BillWithItems
import com.example.quickbillmate.importexport.CustomerJsonCodec
import com.example.quickbillmate.importexport.DataCategory
import com.example.quickbillmate.importexport.DataImportException
import com.example.quickbillmate.importexport.DefaultsGroup
import com.example.quickbillmate.importexport.DefaultsJsonCodec
import com.example.quickbillmate.importexport.ProductJsonCodec
import com.example.quickbillmate.ui.report.ReportAggregator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 数据管理页四类数量概览。 */
data class DataCounts(
    val bills: Int,
    val products: Int,
    val customers: Int,
)

/** 导入前预览：识别出的分类与条数。 */
data class ImportPreview(
    val category: DataCategory,
    val count: Int,
    val text: String,
)

/** 导入结果弹窗数据。 */
data class ImportResultUi(
    val success: Int,
    val skipped: Int,
    val failures: List<Pair<Int, String>>,
)

class DataManagerViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {

    var counts by mutableStateOf(DataCounts(0, 0, 0))
        private set
    var busy by mutableStateOf(false)
        private set
    var toast by mutableStateOf<String?>(null)
        private set
    var shareUri by mutableStateOf<Uri?>(null)
        private set
    var importPreview by mutableStateOf<ImportPreview?>(null)
        private set
    var importResult by mutableStateOf<ImportResultUi?>(null)
        private set
    var importError by mutableStateOf<String?>(null)
        private set

    init {
        refreshCounts()
    }

    fun refreshCounts() {
        viewModelScope.launch {
            counts = withContext(Dispatchers.Default) {
                DataCounts(repo.billCount(), repo.productCount(), repo.customerCount())
            }
        }
    }

    // ---------- 导出 ----------

    fun exportAll(category: DataCategory) {
        viewModelScope.launch {
            busy = true
            try {
                val (text, fileName) = withContext(Dispatchers.IO) { buildExport(category, null) }
                val uri = repo.exportJsonShareUri(app, text, fileName)
                if (uri == null) toast = "导出失败，请重试" else shareUri = uri
            } catch (_: Exception) {
                toast = "导出失败，请重试"
            } finally {
                busy = false
            }
        }
    }

    fun exportSelected(category: DataCategory, ids: Set<Long>) {
        viewModelScope.launch {
            busy = true
            try {
                val (text, fileName) = withContext(Dispatchers.IO) { buildExport(category, ids) }
                val uri = repo.exportJsonShareUri(app, text, fileName)
                if (uri == null) toast = "导出失败，请重试" else shareUri = uri
            } catch (_: Exception) {
                toast = "导出失败，请重试"
            } finally {
                busy = false
            }
        }
    }

    fun exportDefaults(groups: Set<DefaultsGroup>) {
        viewModelScope.launch {
            busy = true
            try {
                val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val text = withContext(Dispatchers.IO) {
                    DefaultsJsonCodec.export(
                        repo.settings.defaultsSnapshot(),
                        repo.settings.customUnits,
                        groups,
                    )
                }
                val uri = repo.exportJsonShareUri(app, text, "defaults_$stamp.json")
                if (uri == null) toast = "导出失败，请重试" else shareUri = uri
            } catch (_: Exception) {
                toast = "导出失败，请重试"
            } finally {
                busy = false
            }
        }
    }

    private suspend fun buildExport(category: DataCategory, ids: Set<Long>?): Pair<String, String> {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return when (category) {
            DataCategory.BILLS -> {
                val bills = if (ids == null) {
                    repo.getBillsOnce()
                } else {
                    repo.getBillsOnce().filter { it.id in ids }
                }
                val items = repo.allBillItemsOnce().groupBy { it.billId }
                BillJsonCodec.export(
                    bills.map { BillWithItems(it, items[it.id].orEmpty()) }
                ) to "bills_$stamp.json"
            }
            DataCategory.PRODUCTS -> {
                val list = if (ids == null) repo.getProducts() else repo.getProducts().filter { it.id in ids }
                ProductJsonCodec.export(list) to "products_$stamp.json"
            }
            DataCategory.CUSTOMERS -> {
                val list = if (ids == null) repo.getCustomers() else repo.getCustomers().filter { it.id in ids }
                CustomerJsonCodec.export(list) to "customers_$stamp.json"
            }
            DataCategory.DEFAULTS -> {
                DefaultsJsonCodec.export(
                    repo.settings.defaultsSnapshot(),
                    repo.settings.customUnits,
                ) to "defaults_$stamp.json"
            }
        }
    }

    // ---------- 选择导出的数据加载 ----------

    suspend fun loadProducts(): List<Product> = repo.getProducts()

    suspend fun loadCustomers(): List<Customer> = repo.getCustomers()

    suspend fun loadBills(start: String?, end: String?): List<Bill> =
        if (start == null || end == null) repo.getBillsOnce() else repo.getBillsByDateRangeOnce(start, end)

    suspend fun billAmounts(bills: List<Bill>): Map<Long, Double> {
        val items = repo.allBillItemsOnce().groupBy { it.billId }
        return bills.associate { it.id to ReportAggregator.amount(it, items[it.id].orEmpty()) }
    }

    // ---------- 导入 ----------

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            busy = true
            try {
                val text = repo.readExternalText(app, uri)
                val category = DataCategory.detect(text)
                if (category == null) {
                    importError = "无法识别的 JSON 文件，请选择快贝智单导出的文件"
                    return@launch
                }
                importPreview = ImportPreview(category, previewCount(category, text), text)
            } catch (e: DataImportException) {
                importError = e.message
            } catch (_: Exception) {
                importError = "读取文件失败"
            } finally {
                busy = false
            }
        }
    }

    fun confirmImport() {
        val preview = importPreview ?: return
        viewModelScope.launch {
            busy = true
            try {
                val result = withContext(Dispatchers.IO) {
                    when (preview.category) {
                        DataCategory.BILLS -> {
                            val r = repo.importBillsFromText(preview.text)
                            ImportResultUi(r.success, 0, r.failures)
                        }
                        DataCategory.PRODUCTS -> {
                            val r = repo.importProductsFromText(preview.text)
                            ImportResultUi(r.success, r.skipped, r.failures)
                        }
                        DataCategory.CUSTOMERS -> {
                            val r = repo.importCustomersFromText(preview.text)
                            ImportResultUi(r.success, r.skipped, r.failures)
                        }
                        DataCategory.DEFAULTS -> {
                            repo.importDefaultsFromText(preview.text)
                            ImportResultUi(1, 0, emptyList())
                        }
                    }
                }
                importPreview = null
                importResult = result
                refreshCounts()
            } catch (e: DataImportException) {
                importPreview = null
                importError = e.message
            } catch (_: Exception) {
                importPreview = null
                importError = "导入失败"
            } finally {
                busy = false
            }
        }
    }

    fun cancelImport() {
        importPreview = null
    }

    fun consumeShare() {
        shareUri = null
    }

    fun consumeToast() {
        toast = null
    }

    fun consumeResult() {
        importResult = null
    }

    fun consumeError() {
        importError = null
    }

    private fun previewCount(category: DataCategory, text: String): Int = when (category) {
        DataCategory.BILLS -> BillJsonCodec.parse(text).imported.size
        DataCategory.PRODUCTS -> ProductJsonCodec.parse(text, emptyList()).success
        DataCategory.CUSTOMERS -> CustomerJsonCodec.parse(text).size
        DataCategory.DEFAULTS -> 1
    }
}
