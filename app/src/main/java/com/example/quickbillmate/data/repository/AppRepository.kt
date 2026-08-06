package com.example.quickbillmate.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.room.withTransaction
import com.example.quickbillmate.data.db.AppDatabase
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.importexport.ContactsImporter
import com.example.quickbillmate.importexport.GalleryWriter
import com.example.quickbillmate.importexport.ProductImportResult
import com.example.quickbillmate.importexport.ProductJsonCodec
import com.example.quickbillmate.importexport.ProductJsonException
import com.example.quickbillmate.render.InvoiceRenderer
import com.example.quickbillmate.render.RenderInvoice
import com.example.quickbillmate.render.StyleParams
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.util.BillNumber
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppRepository(
    private val database: AppDatabase,
    val settings: SettingsStore,
) {
    private val billDao = database.billDao()
    private val itemDao = database.billItemDao()
    private val productDao = database.productDao()
    private val customerDao = database.customerDao()
    private val presetDao = database.stylePresetDao()
    private val renderer = InvoiceRenderer()

    // ---------- 单据 ----------

    fun observeRecentBills(): Flow<List<Bill>> = billDao.observeRecent()

    fun observeBill(id: Long): Flow<Bill?> = billDao.observeBill(id)

    fun observeItems(billId: Long): Flow<List<BillItem>> = itemDao.observeItems(billId)

    suspend fun getBill(id: Long): Bill? = billDao.getBill(id)

    suspend fun getItems(billId: Long): List<BillItem> = itemDao.getItems(billId)

    suspend fun serialConflict(code: String, date: String, serial: String, excludeId: Long): Boolean =
        billDao.countSerialConflict(code, date, serial, excludeId) > 0

    suspend fun generateUniqueSerial(code: String, date: String): String {
        var serial = BillNumber.randomSerial()
        var guard = 0
        while (billDao.countSerialConflict(code, date, serial, 0) > 0 && guard < 1000) {
            serial = BillNumber.randomSerial()
            guard++
        }
        return serial
    }

    /** 新建草稿：立即写入数据库并返回。 */
    suspend fun createDraft(
        docCode: String,
        docDate: String,
        companyName: String,
        contactPhone: String,
        salesManager: String,
    ): Bill {
        val serial = generateUniqueSerial(docCode, docDate)
        val bill = Bill(
            companyName = companyName,
            contactPhone = contactPhone,
            salesManager = salesManager,
            docCode = docCode,
            docSerial = serial,
            docDate = docDate,
            presetKey = settings.defaultPresetKey,
        )
        val id = billDao.insert(bill)
        return bill.copy(id = id)
    }

    /** 单据与其商品行在同一个事务内原子保存。 */
    suspend fun saveBill(bill: Bill, items: List<BillItem>) {
        database.withTransaction {
            billDao.update(bill.copy(updatedAt = System.currentTimeMillis()))
            itemDao.deleteByBill(bill.id)
            itemDao.insertAll(items)
        }
    }

    /** 更新单据状态（草稿 / 已导出）。 */
    suspend fun updateBillStatus(billId: Long, status: String) {
        billDao.getBill(billId)?.let { bill ->
            if (bill.status != status) {
                billDao.update(bill.copy(status = status, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun deleteBill(bill: Bill) = billDao.delete(bill)

    // ---------- 商品 ----------

    fun observeProducts(query: String): Flow<List<Product>> =
        if (query.isBlank()) productDao.observeAll() else productDao.observeSearch(query.trim())

    suspend fun getProducts(): List<Product> = productDao.getAll()

    suspend fun saveProduct(product: Product) {
        if (product.id == 0L) productDao.insert(product) else productDao.update(product)
    }

    suspend fun deleteProduct(product: Product) = productDao.delete(product)

    // ---------- 客户 ----------

    fun observeCustomers(query: String): Flow<List<Customer>> =
        if (query.isBlank()) customerDao.observeAll() else customerDao.observeSearch(query.trim())

    suspend fun getCustomers(): List<Customer> = customerDao.getAll()

    suspend fun saveCustomer(customer: Customer) {
        if (customer.id == 0L) customerDao.insert(customer) else customerDao.update(customer)
    }

    suspend fun deleteCustomer(customer: Customer) = customerDao.delete(customer)

    /** 通讯录导入客户：按 name+phone 去重，返回 (成功, 已存在跳过)。 */
    suspend fun importContactCandidates(candidates: List<ContactsImporter.Candidate>): Pair<Int, Int> {
        var success = 0
        var skipped = 0
        candidates.forEach { candidate ->
            val dup = customerDao.countDuplicate(candidate.name, candidate.phone)
            if (dup > 0) {
                skipped++
            } else {
                customerDao.insert(
                    Customer(
                        name = candidate.name,
                        phone = candidate.phone,
                        fromContacts = true,
                    )
                )
                success++
            }
        }
        return success to skipped
    }

    // ---------- 样式预设 ----------

    fun observePresets(): Flow<List<StylePreset>> = presetDao.observeAll()

    suspend fun getPresets(): List<StylePreset> = presetDao.getAll()

    suspend fun savePreset(preset: StylePreset): Long =
        if (preset.id == 0L) {
            presetDao.insert(preset)
        } else {
            presetDao.update(preset)
            preset.id
        }

    suspend fun deletePreset(preset: StylePreset) = presetDao.delete(preset)

    // ---------- 商品 JSON 导入 / 导出 ----------

    suspend fun importProductsFromUri(context: Context, uri: Uri): ProductImportResult {
        val text = readText(context, uri)
        val existing = productDao.getAll()
        val result = ProductJsonCodec.parse(text, existing)
        if (result.imported.isNotEmpty()) {
            productDao.insertAll(result.imported)
        }
        return result
    }

    /** 导出到系统“下载”目录，返回文件名；失败返回 null。 */
    suspend fun exportProductsToDownloads(context: Context): String? {
        val products = productDao.getAll()
        val text = ProductJsonCodec.export(products)
        val fileName = "products_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportModern(context, text, fileName)
        } else {
            exportLegacy(context, text, fileName)
        }
    }

    private fun readText(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw ProductJsonException("无法读取文件")
        if (bytes.size > ProductJsonCodec.MAX_SIZE_BYTES) {
            throw ProductJsonException("文件超过 2MB，已拒绝导入")
        }
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: Exception) {
            throw ProductJsonException("文件编码不支持")
        }
    }

    private fun exportModern(context: Context, text: String, fileName: String): String? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/QuickBillMate")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
            } ?: return null
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            fileName
        } catch (_: Exception) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {
            }
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun exportLegacy(context: Context, text: String, fileName: String): String? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "QuickBillMate",
        )
        if (!dir.exists() && !dir.mkdirs()) return null
        return try {
            File(dir, fileName).writeText(text, Charsets.UTF_8)
            fileName
        } catch (_: Exception) {
            null
        }
    }

    // ---------- 图片渲染 / 导出 / 分享 ----------

    suspend fun renderInvoice(invoice: RenderInvoice, presetKey: String?, widthPx: Int): Bitmap {
        val params = resolveParams(presetKey, presetDao.getAll())
        return renderer.render(invoice, params, widthPx)
    }

    suspend fun resolveParams(presetKey: String?, presets: List<StylePreset>): StyleParams =
        StylePresets.resolve(presetKey, presets)

    fun saveInvoiceToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean =
        GalleryWriter.save(context, bitmap, fileName)

    fun invoiceShareUri(context: Context, bitmap: Bitmap, fileName: String): Uri =
        GalleryWriter.shareUri(context, bitmap, fileName)
}
