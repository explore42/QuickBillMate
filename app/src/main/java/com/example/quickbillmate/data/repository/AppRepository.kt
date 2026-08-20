package com.example.quickbillmate.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.withTransaction
import com.example.quickbillmate.data.db.AppDatabase
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.importexport.ContactsImporter
import com.example.quickbillmate.importexport.CustomerJsonCodec
import com.example.quickbillmate.importexport.CustomerImportMerger
import com.example.quickbillmate.importexport.CustomerImportResult
import com.example.quickbillmate.importexport.BillJsonCodec
import com.example.quickbillmate.importexport.BillImportResult
import com.example.quickbillmate.importexport.DataImportException
import com.example.quickbillmate.importexport.DefaultsJsonCodec
import com.example.quickbillmate.importexport.DefaultsExport
import com.example.quickbillmate.importexport.GalleryWriter
import com.example.quickbillmate.importexport.ProductImportResult
import com.example.quickbillmate.importexport.ProductJsonCodec
import com.example.quickbillmate.render.RenderInvoice
import com.example.quickbillmate.render.RenderItem
import com.example.quickbillmate.util.PhoneUtil
import com.example.quickbillmate.util.Pinyin
import com.example.quickbillmate.util.BillNumber
import com.example.quickbillmate.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** 通讯录导入结果：新增 / 合并（同名同号视为合并，无需写库）。 */
data class ContactImportOutcome(
    val inserted: Int = 0,
    val merged: Int = 0,
)

/** 计算客户拼音并写入字段（入库时调用）。 */
internal fun Customer.withPinyin(): Customer =
    copy(
        pinyinInitial = Pinyin.firstLetter(name),
        pinyin = Pinyin.fullPinyin(name),
    )

/** 计算商品拼音并写入字段（入库时调用）。 */
internal fun Product.withPinyin(): Product =
    copy(
        pinyinInitial = Pinyin.firstLetter(name),
        pinyin = Pinyin.fullPinyin(name),
    )

class AppRepository(
    private val database: AppDatabase,
    val settings: SettingsStore,
    val qrImages: QrImageStore,
) {
    private val billDao = database.billDao()
    private val itemDao = database.billItemDao()
    private val productDao = database.productDao()
    private val customerDao = database.customerDao()
    private val presetDao = database.stylePresetDao()

    // ---------- 单据 ----------

    fun observeRecentBills(): Flow<List<Bill>> = billDao.observeRecent()

    suspend fun billCount(): Int = billDao.count()

    suspend fun getBillsOnce(): List<Bill> = billDao.getRecentOnce()

    suspend fun getBillsByDateRangeOnce(start: String, end: String): List<Bill> =
        billDao.getByDateRangeOnce(start, end)

    /** 一次性查询当前列表：Room 失效通知偶发丢失时的兜底刷新入口。 */
    suspend fun recentBillsOnce(): List<Bill> = billDao.getRecentOnce()

    suspend fun allBillItemsOnce(): List<BillItem> = itemDao.getAllOnce()

    fun observeBill(id: Long): Flow<Bill?> = billDao.observeBill(id)

    fun observeItems(billId: Long): Flow<List<BillItem>> = itemDao.observeItems(billId)

    fun observeAllBillItems(): Flow<List<BillItem>> = itemDao.observeAll()

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
        titleSuffix: String = "单据",
        remark: String = "",
        adText: String = "",
        showManager: Boolean = true,
        showRemark: Boolean = true,
        showAd: Boolean = false,
        showWatermark: Boolean = false,
        watermarkText: String = "",
        showContactPhone: Boolean = true,
        showMultiPhones: Boolean = false,
        showCustomerPhone: Boolean = false,
    ): Bill {
        val serial = generateUniqueSerial(docCode, docDate)
        val bill = Bill(
            companyName = companyName,
            contactPhone = contactPhone,
            salesManager = salesManager,
            docCode = docCode,
            docSerial = serial,
            docDate = docDate,
            titleSuffix = titleSuffix,
            remark = remark,
            adText = adText,
            showManager = showManager,
            showRemark = showRemark,
            showAd = showAd,
            showWatermark = showWatermark,
            watermarkText = watermarkText,
            showContactPhone = showContactPhone,
            showMultiPhones = showMultiPhones,
            showCustomerPhone = showCustomerPhone,
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

    /** 复制单据（含商品行），新流水号；返回新单据 id。 */
    suspend fun copyBill(billId: Long): Long? {
        val bill = billDao.getBill(billId) ?: return null
        val items = itemDao.getItems(billId)
        val now = System.currentTimeMillis()
        val serial = generateUniqueSerial(bill.docCode, bill.docDate)
        val copy = bill.copy(
            id = 0,
            docSerial = serial,
            createdAt = now,
            updatedAt = now,
        )
        return database.withTransaction {
            val newId = billDao.insert(copy)
            itemDao.insertAll(items.map { it.copy(id = 0, billId = newId) })
            newId
        }
    }

    suspend fun deleteBill(bill: Bill) = billDao.delete(bill)

    // ---------- 商品 ----------

    suspend fun productCount(): Int = productDao.count()

    fun observeProducts(query: String): Flow<List<Product>> =
        if (query.isBlank()) productDao.observeAll() else productDao.observeSearch(query.trim())

    suspend fun getProducts(): List<Product> = productDao.getAll()

    suspend fun copyProducts(products: List<Product>) {
        val copies = products.map { it.copy(id = 0, name = it.name + "（副本）").withPinyin() }
        productDao.insertAll(copies)
    }

    suspend fun deleteProducts(products: List<Product>) {
        products.forEach { productDao.delete(it) }
    }

    suspend fun saveProduct(product: Product) {
        if (product.id == 0L) {
            productDao.insert(product.withPinyin())
        } else {
            val existing = productDao.getById(product.id)
            val updated = if (existing == null || existing.name != product.name) {
                product.withPinyin()
            } else {
                // 名称未变：保留库中原有拼音，避免覆盖
                product.copy(pinyinInitial = existing.pinyinInitial, pinyin = existing.pinyin)
            }
            productDao.update(updated)
        }
    }

    suspend fun deleteProduct(product: Product) = productDao.delete(product)

    // ---------- 客户 ----------

    suspend fun customerCount(): Int = customerDao.count()

    fun observeCustomers(query: String): Flow<List<Customer>> =
        if (query.isBlank()) customerDao.observeAll() else customerDao.observeSearch(query.trim())

    suspend fun getCustomers(): List<Customer> = customerDao.getAll()

    suspend fun copyCustomers(customers: List<Customer>) {
        val copies = customers.map { it.copy(id = 0, name = it.name + "（副本）").withPinyin() }
        customerDao.insertAll(copies)
    }

    suspend fun deleteCustomers(customers: List<Customer>) {
        customers.forEach { customerDao.delete(it) }
    }

    suspend fun saveCustomer(customer: Customer) {
        if (customer.id == 0L) {
            customerDao.insert(customer.withPinyin())
        } else {
            val existing = customerDao.getById(customer.id)
            val updated = if (existing == null || existing.name != customer.name) {
                customer.withPinyin()
            } else {
                // 名称未变：保留库中原有拼音，避免覆盖
                customer.copy(pinyinInitial = existing.pinyinInitial, pinyin = existing.pinyin)
            }
            customerDao.update(updated)
        }
    }

    suspend fun deleteCustomer(customer: Customer) = customerDao.delete(customer)

    /** 首次安装判定：库中是否已有任何业务数据（单据/商品/客户）。 */
    suspend fun hasAnyDataOnce(): Boolean =
        billDao.count() > 0 || productDao.count() > 0 || customerDao.count() > 0

    /**
     * 通讯录导入客户，统一按“合并”语义：
     * - 同名不同号：追加号码到现有客户（逗号分隔、去重），计入“合并”
     * - 同名同号：已存在，无需写库，计入“合并”
     * - 不同名：新增客户，计入“新增”
     */
    suspend fun importContactCandidates(candidates: List<ContactsImporter.Candidate>): ContactImportOutcome {
        // 通讯录可能很多：先在后台线程统一算好拼音，避免重复计算
        val pinyinByKey = withContext(Dispatchers.Default) {
            candidates.map { it.name to (Pinyin.firstLetter(it.name) to Pinyin.fullPinyin(it.name)) }
                .toMap()
        }
        var inserted = 0
        var merged = 0
        candidates.forEach { rawCandidate ->
            // 无论调用方是否已规范化，统一再规范一次
            val candidate = rawCandidate.copy(phone = PhoneUtil.normalizePhone(rawCandidate.phone))
            val existing = customerDao.findByName(candidate.name)
            if (existing != null) {
                // 老数据可能未规范化，比较与追加统一用规范化号码
                val phones = PhoneUtil.splitPhones(existing.phone).toMutableList()
                if (candidate.phone !in phones) {
                    phones.add(candidate.phone)
                    customerDao.update(
                        existing.copy(
                            phone = phones.joinToString(","),
                        )
                    )
                }
                merged++
            } else {
                val (initial, full) = pinyinByKey[candidate.name] ?: ("#" to "")
                customerDao.insert(
                    Customer(
                        name = candidate.name,
                        phone = candidate.phone,
                        pinyinInitial = initial,
                        pinyin = full,
                    )
                )
                inserted++
            }
        }
        return ContactImportOutcome(inserted, merged)
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

    // ---------- JSON 导入 / 导出（设置页统一入口，v1.2） ----------

    suspend fun importProductsFromUri(context: Context, uri: Uri): ProductImportResult {
        val text = readText(context, uri)
        return importProductsFromText(text)
    }

    /** 从 JSON 文本导入商品（文件与剪贴板共用入口）。 */
    suspend fun importProductsFromText(text: String): ProductImportResult {
        val existing = productDao.getAll()
        val result = ProductJsonCodec.parse(text, existing)
        if (result.imported.isNotEmpty()) {
            // 批量导入在后台线程统一补全拼音后写入
            val withPinyin = withContext(Dispatchers.Default) {
                result.imported.map { it.withPinyin() }
            }
            productDao.insertAll(withPinyin)
        }
        return result
    }

    /** 导入单据 JSON：按原样新增（保留编号/日期），不查重。 */
    suspend fun importBillsFromText(text: String): BillImportResult {
        val result = BillJsonCodec.parse(text)
        if (result.imported.isNotEmpty()) {
            database.withTransaction {
                result.imported.forEach { bw ->
                    val billId = billDao.insert(bw.bill.copy(id = 0))
                    itemDao.insertAll(bw.items.map { it.copy(id = 0, billId = billId) })
                }
            }
        }
        return result
    }

    /** 导入客户 JSON：按姓名合并（电话去重追加），不存在则新增。 */
    suspend fun importCustomersFromText(text: String): CustomerImportResult {
        val parsed = CustomerJsonCodec.parse(text)
        var success = 0
        var skipped = 0
        parsed.forEach { customer ->
            val existing = customerDao.findByName(customer.name)
            if (existing != null) {
                val merged = CustomerImportMerger.mergePhones(
                    existing.phone,
                    PhoneUtil.splitPhones(customer.phone),
                )
                if (merged != existing.phone) {
                    customerDao.update(existing.copy(phone = merged))
                }
                skipped++
            } else {
                customerDao.insert(customer.withPinyin())
                success++
            }
        }
        return CustomerImportResult(success, skipped, emptyList())
    }

    /** 导入默认信息 JSON：只覆盖文件中出现的字段（含预置单位，不含二维码）。 */
    suspend fun importDefaultsFromText(text: String): DefaultsExport {
        val parsed = DefaultsJsonCodec.parse(text)
        val v = parsed.values
        val present = parsed.presentFields
        val s = settings
        if ("titleSuffix" in present) s.defaultTitleSuffix = v.titleSuffix
        if ("docCode" in present) s.defaultDocCode = v.docCode
        if ("showCustomerPhone" in present) s.defaultShowCustomerPhone = v.showCustomerPhone
        if ("showMultiPhones" in present) s.defaultShowMultiPhones = v.showMultiPhones
        if ("companyName" in present) s.defaultCompany = v.companyName
        if ("manager" in present) s.defaultManager = v.manager
        if ("showManager" in present) s.defaultShowManager = v.showManager
        if ("contactPhone" in present) s.defaultPhone = v.contactPhone
        if ("showContactPhone" in present) s.defaultShowContactPhone = v.showContactPhone
        if ("showRemark" in present) s.defaultShowRemark = v.showRemark
        if ("showAd" in present) s.defaultShowAd = v.showAd
        if ("remark" in present) s.defaultRemark = v.remark
        if ("adText" in present) s.defaultAdText = v.adText
        if ("watermarkText" in present) s.defaultWatermarkText = v.watermarkText
        if ("showWatermark" in present) s.defaultShowWatermark = v.showWatermark
        parsed.customUnits?.let { s.customUnits = it }
        return parsed
    }

    /** 将导出的 JSON 写入缓存并返回 FileProvider 分享 URI（失败返回 null）。 */
    suspend fun exportJsonShareUri(context: Context, text: String, fileName: String): Uri? =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, fileName)
            try {
                file.writeText(text, Charsets.UTF_8)
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.example.quickbillmate.fileprovider",
                    file,
                )
            } catch (_: Exception) {
                null
            }
        }

    /** 读取外部文件文本（2MB/UTF-8 校验，数据管理页导入共用）。 */
    suspend fun readExternalText(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) { readText(context, uri) }

    private fun readText(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw DataImportException("无法读取文件")
        if (bytes.size > ProductJsonCodec.MAX_SIZE_BYTES) {
            throw DataImportException("文件超过 2MB，已拒绝导入")
        }
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: Exception) {
            throw DataImportException("文件编码不支持")
        }
    }

    // ---------- 图片导出 / 分享 ----------

    /** 读取已保存的全局微信二维码位图（无则返回 null）。 */
    fun loadQrBitmap(): Bitmap? = qrImages.load()

    fun buildRenderInvoice(
        bill: Bill,
        items: List<BillItem>,
        qrBitmap: Bitmap? = null,
    ): RenderInvoice = RenderInvoice(
        customerName = bill.customerName,
        customerPhone = PhoneUtil.displayPhones(bill.customerPhone, bill.showCustomerPhone, bill.showMultiPhones),
        companyName = bill.companyName,
        contactPhone = bill.contactPhone,
        salesManager = bill.salesManager,
        docCode = bill.docCode,
        docSerial = bill.docSerial,
        docDate = bill.docDate,
        discount = bill.discount,
        remark = bill.remark,
        titleSuffix = bill.titleSuffix,
        adText = bill.adText,
        showManager = bill.showManager,
        showRemark = bill.showRemark,
        showAd = bill.showAd,
        showWatermark = bill.showWatermark,
        watermarkText = bill.watermarkText,
        showContactPhone = bill.showContactPhone,
        showCustomerPhone = bill.showCustomerPhone,
        qrBitmap = qrBitmap,
        items = items.map {
            RenderItem(
                name = it.name,
                spec = it.spec,
                unit = it.unit,
                qty = it.qty,
                price = it.price,
                pack = it.pack,
                note = it.note,
            )
        },
    )

    fun billFileName(bill: Bill): String =
        "单据_${BillNumber.build(bill.docCode, bill.docDate, bill.docSerial)}.png"

    /** 渲染并保存单据图片到相册。 */
    suspend fun exportBitmapToGallery(context: Context, bill: Bill, bitmap: Bitmap): Boolean {
        val ok = GalleryWriter.save(context, bitmap, billFileName(bill))
        return ok
    }

    suspend fun shareBill(context: Context, bill: Bill, bitmap: Bitmap): Uri =
        GalleryWriter.shareUri(context, bitmap, billFileName(bill))

    fun saveInvoiceToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean =
        GalleryWriter.save(context, bitmap, fileName)

    fun invoiceShareUri(context: Context, bitmap: Bitmap, fileName: String): Uri =
        GalleryWriter.shareUri(context, bitmap, fileName)
}
