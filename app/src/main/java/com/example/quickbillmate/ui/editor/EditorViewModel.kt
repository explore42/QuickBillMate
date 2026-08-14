package com.example.quickbillmate.ui.editor

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.importexport.ContactsImporter
import com.example.quickbillmate.render.RenderInvoice
import com.example.quickbillmate.render.RenderItem
import com.example.quickbillmate.render.InvoiceRenderBus
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.ui.common.DefaultInfoValues
import com.example.quickbillmate.util.DateUtils
import com.example.quickbillmate.util.Money
import com.example.quickbillmate.util.PhoneUtil
import com.example.quickbillmate.util.Pinyin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ItemRow(
    val name: String = "",
    val spec: String = "",
    val unit: String = "桶",
    val qtyText: String = "1",
    val priceText: String = "0",
    val pack: String = "",
    val note: String = "",
) {
    fun qty(): Double = qtyText.toDoubleOrNull() ?: 0.0

    fun price(): Double = priceText.toDoubleOrNull() ?: 0.0

    fun amount(): Double = if (qty() <= 0) 0.0 else Money.round2(qty() * price())
}

data class CustomerSuggestion(
    val name: String,
    val phone: String,
    val type: String = "",
    val fromDb: Boolean,
)

data class EditorUiState(
    val loaded: Boolean = false,
    val billId: Long = 0L,
    val createdAt: Long = 0L,
    val customerName: String = "",
    val customerPhone: String = "",
    val companyName: String = "",
    val contactPhone: String = "",
    val salesManager: String = "",
    val docCode: String = "PH",
    val docSerial: String = "",
    val serialError: String? = null,
    val docDate: String = DateUtils.today(),
    val discountText: String = "0.00",
    val remark: String = "",
    val titleSuffix: String = "单据",
    val adText: String = "",
    val showManager: Boolean = true,
    val showRemark: Boolean = true,
    val showAd: Boolean = false,
    val showWatermark: Boolean = false,
    val watermarkText: String = "",
    val showContactPhone: Boolean = true,
    val showMultiPhones: Boolean = false,
    val favorite: Boolean = false,
    val presetKey: String = "classic_plain",
    val items: List<ItemRow> = listOf(ItemRow()),
    val preview: Bitmap? = null,
    val customers: List<Customer> = emptyList(),
    val suggestions: List<CustomerSuggestion> = emptyList(),
    val products: List<Product> = emptyList(),
    val presets: List<StylePreset> = emptyList(),
    val contactsGranted: Boolean = false,
    val savedTick: Int = 0,
)

class EditorViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {

    var state by mutableStateOf(EditorUiState())
        private set

    private var saveJob: Job? = null
    private var previewJob: Job? = null
    private var loaded = false
    private var cachedContacts: List<ContactsImporter.Candidate>? = null

    /** 客户排序缓存：收藏优先 + 拼音首字母，在后台线程计算，避免上千客户导致进入编辑页卡顿。 */
    private data class SortedCustomer(val customer: Customer, val letter: String)
    private var sortedCustomers: List<SortedCustomer> = emptyList()

    /** 进入编辑页时的原始快照，用于“不保存”时恢复；新建单据时为 null。 */
    private var originalBill: Bill? = null
    private var originalItems: List<BillItem> = emptyList()
    private var isNewDraft = false

    init {
        viewModelScope.launch {
            repo.observeCustomers("").collect { customers ->
                state = state.copy(customers = customers)
                rebuildSortedCustomers(customers)
            }
        }
        viewModelScope.launch {
            repo.observeProducts("").collect { state = state.copy(products = it) }
        }
        viewModelScope.launch {
            repo.observePresets().collect { state = state.copy(presets = it) }
        }
    }

    /** 新建：立即创建草稿并载入。 */
    fun createNew() {
        viewModelScope.launch {
            val settings = repo.settings
            val draft = repo.createDraft(
                docCode = settings.defaultDocCode,
                docDate = DateUtils.today(),
                companyName = settings.defaultCompany,
                contactPhone = settings.defaultPhone,
                salesManager = settings.defaultManager,
                titleSuffix = settings.defaultTitleSuffix,
                remark = settings.defaultRemark,
                adText = settings.defaultAdText,
                showManager = settings.defaultShowManager,
                showRemark = settings.defaultShowRemark,
                showAd = settings.defaultShowAd,
                showWatermark = settings.defaultShowWatermark,
                watermarkText = settings.defaultWatermarkText,
                showContactPhone = settings.defaultShowContactPhone,
                showMultiPhones = settings.defaultShowMultiPhones,
            )
            isNewDraft = true
            originalBill = draft
            originalItems = emptyList()
            applyBill(draft, emptyList())
            refreshSuggestions(state.customerName)
            scheduleSave()
            schedulePreview()
        }
    }

    /** 编辑已有单据。 */
    fun load(billId: Long) {
        viewModelScope.launch {
            val bill = repo.getBill(billId)
            if (bill == null) {
                createNew()
            } else {
                val items = repo.getItems(billId)
                isNewDraft = false
                originalBill = bill
                originalItems = items
                applyBill(bill, items)
                refreshSuggestions(state.customerName)
                schedulePreview()
            }
        }
    }

    /**
     * “不保存”：新建单据时删除草稿；编辑已有单据时恢复进入页面时的原始内容。
     * 完成后回调（通常返回上一页）。
     */
    fun discardChanges(onDone: () -> Unit) {
        viewModelScope.launch {
            if (isNewDraft) {
                repo.getBill(state.billId)?.let { repo.deleteBill(it) }
            } else {
                val original = originalBill
                if (original != null) {
                    repo.saveBill(original, originalItems)
                }
            }
            onDone()
        }
    }

    private fun applyBill(bill: Bill, items: List<BillItem>) {
        loaded = true
        state = state.copy(
            loaded = true,
            billId = bill.id,
            createdAt = bill.createdAt,
            customerName = bill.customerName,
            customerPhone = bill.customerPhone,
            companyName = bill.companyName,
            contactPhone = bill.contactPhone,
            salesManager = bill.salesManager,
            docCode = bill.docCode,
            docSerial = bill.docSerial,
            docDate = bill.docDate,
            discountText = Money.format(bill.discount),
            remark = bill.remark,
            titleSuffix = bill.titleSuffix,
            adText = bill.adText,
            showManager = bill.showManager,
            showRemark = bill.showRemark,
            showAd = bill.showAd,
            showWatermark = bill.showWatermark,
            watermarkText = bill.watermarkText,
            showContactPhone = bill.showContactPhone,
            showMultiPhones = bill.showMultiPhones,
            favorite = bill.favorite,
            presetKey = bill.presetKey,
            items = items.map { it.toRow() }.ifEmpty { listOf(ItemRow()) },
            contactsGranted = currentContactsPermission(),
        )
        if (state.contactsGranted) loadContacts()
    }

    /** 每次进入编辑页时按系统真实授权状态刷新，避免已授权仍显示授权按钮。 */
    private fun currentContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /** 在后台线程完成拼音首字母提取与排序，结果缓存后用于联想。 */
    private fun rebuildSortedCustomers(customers: List<Customer>) {
        viewModelScope.launch(Dispatchers.Default) {
            val sorted = customers.map { SortedCustomer(it, it.pinyinInitial) }
                .sortedWith(
                    compareByDescending<SortedCustomer> { it.customer.favorite }
                        .thenBy { Pinyin.letterSortKey(it.letter) }
                )
            withContext(Dispatchers.Main) {
                sortedCustomers = sorted
                refreshSuggestions(state.customerName)
            }
        }
    }

    /** 通讯录查询放后台线程，避免进入编辑页时阻塞主线程。 */
    private fun loadContacts() {
        if (cachedContacts != null) return
        viewModelScope.launch(Dispatchers.Default) {
            val contacts = ContactsImporter.query(app)
            withContext(Dispatchers.Main) {
                cachedContacts = contacts
                refreshSuggestions(state.customerName)
            }
        }
    }

    private fun update(block: EditorUiState.() -> EditorUiState) {
        state = state.block()
        scheduleSave()
        schedulePreview()
    }

    // ---------- 字段编辑 ----------

    fun onCustomerNameChange(value: String) {
        update { copy(customerName = value) }
        refreshSuggestions(value)
    }

    fun onCustomerPhoneChange(value: String) = update { copy(customerPhone = value) }
    fun onCompanyNameChange(value: String) = update { copy(companyName = value) }
    fun onContactPhoneChange(value: String) = update { copy(contactPhone = value) }
    fun onManagerChange(value: String) = update { copy(salesManager = value) }
    fun onDocCodeChange(value: String) = update { copy(docCode = value) }
    /** 流水号：只接受 0-9 且最多三位，多余输入直接忽略。 */
    fun onSerialChange(value: String) {
        if (value.length <= 3 && value.all { it.isDigit() }) {
            update { copy(docSerial = value, serialError = null) }
        }
    }

    /** 流水号校验：仅在输入框失去焦点时触发；不合法或重复时不自动修改，标红提示。 */
    fun validateSerial() {
        viewModelScope.launch {
            val s = state
            val serial = s.docSerial.trim()
            val error = when {
                !serial.matches(Regex("\\d{3}")) -> "需要三位数字"
                repo.serialConflict(s.docCode.ifBlank { "PH" }, s.docDate, serial, s.billId) ->
                    "流水号已存在，请更换"
                else -> null
            }
            if (state.serialError != error) {
                state = state.copy(serialError = error)
            }
        }
    }
    fun onDateChange(value: String) = update { copy(docDate = value) }
    fun onDiscountChange(value: String) = update { copy(discountText = value) }
    fun onRemarkChange(value: String) = update { copy(remark = value) }
    fun onTitleSuffixChange(value: String) = update { copy(titleSuffix = value) }
    fun onAdTextChange(value: String) = update { copy(adText = value) }
    fun onShowManagerChange(value: Boolean) = update { copy(showManager = value) }
    fun onShowRemarkChange(value: Boolean) = update { copy(showRemark = value) }
    fun onShowAdChange(value: Boolean) = update { copy(showAd = value) }
    fun onShowWatermarkChange(value: Boolean) = update { copy(showWatermark = value) }
    fun onShowContactPhoneChange(value: Boolean) = update { copy(showContactPhone = value) }

    fun onShowMultiPhonesChange(value: Boolean) = update { copy(showMultiPhones = value) }

    /** 用“默认信息”表单值更新当前单据（局部修改，不影响全局）。 */
    fun applyDefaultInfoValues(values: DefaultInfoValues) {
        update {
            copy(
                titleSuffix = values.titleSuffix,
                docCode = values.docCode,
                showMultiPhones = values.showMultiPhones,
                companyName = values.companyName,
                salesManager = values.manager,
                showManager = values.showManager,
                contactPhone = values.contactPhone,
                showContactPhone = values.showContactPhone,
                showRemark = values.showRemark,
                remark = values.remark,
                showAd = values.showAd,
                adText = values.adText,
                watermarkText = values.watermarkText,
                showWatermark = values.showWatermark,
            )
        }
    }

    fun onFavoriteChange(value: Boolean) = update { copy(favorite = value) }

    fun regenerateSerial() {
        viewModelScope.launch {
            val serial = repo.generateUniqueSerial(state.docCode.ifBlank { "PH" }, state.docDate)
            update { copy(docSerial = serial, serialError = null) }
        }
    }

    fun selectPreset(key: String) = update { copy(presetKey = key) }

    fun onContactsPermission(granted: Boolean) {
        state = state.copy(contactsGranted = granted)
        refreshSuggestions(state.customerName)
    }

    /**
     * 客户联想（下拉）：客户库优先（收藏 → 时间），通讯录兜底，手动输入始终可用。
     * 输入为空时也返回默认候选（收藏客户优先），便于直接下拉选择。
     */
    private fun refreshSuggestions(query: String) {
        val q = query.trim()
        // 使用后台预排序的缓存（收藏优先 → 拼音 A-Z → #），主线程只做轻量过滤
        val pool = sortedCustomers
        val fromDb = (if (q.isBlank()) pool else pool.filter {
            it.customer.name.contains(q) || it.customer.phone.contains(q)
        })
            .map {
                CustomerSuggestion(
                    name = it.customer.name,
                    phone = it.customer.phone,
                    type = it.customer.type,
                    fromDb = true,
                )
            }

        val dbKeys = fromDb.map { it.name to it.phone }.toSet()
        val fromContacts = if (state.contactsGranted) {
            cachedContacts
                ?.filter { q.isBlank() || it.name.contains(q) || it.phone.contains(q) }
                ?.filterNot { (it.name to it.phone) in dbKeys }
                ?.take(3)
                ?.map {
                    CustomerSuggestion(
                        name = it.name,
                        phone = it.phone,
                        fromDb = false,
                    )
                }
                .orEmpty()
        } else {
            emptyList()
        }

        state = state.copy(suggestions = fromDb + fromContacts)
    }

    fun selectSuggestion(suggestion: CustomerSuggestion) {
        update {
            copy(
                customerName = suggestion.name,
                customerPhone = suggestion.phone,
                suggestions = emptyList(),
            )
        }
        // 通讯录候选选中后自动按合并语义导入客户库
        if (!suggestion.fromDb) {
            viewModelScope.launch {
                repo.importContactCandidates(
                    listOf(ContactsImporter.Candidate(suggestion.name, suggestion.phone))
                )
            }
        }
    }

    // ---------- 商品行 ----------

    fun updateItem(index: Int, transform: (ItemRow) -> ItemRow) {
        if (index !in state.items.indices) return
        update {
            copy(items = items.mapIndexed { i, row -> if (i == index) transform(row) else row })
        }
    }

    fun addItem() = update { copy(items = items + ItemRow()) }

    fun removeItem(index: Int) {
        update {
            if (items.size <= 1) {
                copy(items = listOf(ItemRow()))
            } else {
                copy(items = items.filterIndexed { i, _ -> i != index })
            }
        }
    }

    fun moveItem(index: Int, delta: Int) {
        val target = index + delta
        if (target !in state.items.indices) return
        val list = state.items.toMutableList()
        val tmp = list[index]
        list[index] = list[target]
        list[target] = tmp
        update { copy(items = list) }
    }

    fun addProductToItems(product: Product) {
        update {
            copy(
                items = items + ItemRow(
                    name = product.name,
                    spec = product.spec,
                    unit = product.unit,
                    qtyText = "1",
                    priceText = Money.format(product.price),
                    pack = product.pack,
                    note = product.note,
                )
            )
        }
    }

    // ---------- 自动保存 / 手动保存 / 预览 ----------

    private fun scheduleSave() {
        if (!loaded) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            autosave()
        }
    }

    /**
     * 独立“保存”按钮：校验流水号后保存，成功后回调（返回上一页）。
     * 校验不通过时只标红提示，不自动修改、不保存。
     */
    fun saveNow(onSaved: () -> Unit) {
        if (!loaded) return
        saveJob?.cancel()
        viewModelScope.launch {
            val s = state
            val serial = s.docSerial.trim()
            val error = when {
                !serial.matches(Regex("\\d{3}")) -> "需要三位数字"
                repo.serialConflict(s.docCode.ifBlank { "PH" }, s.docDate, serial, s.billId) ->
                    "流水号已存在，请更换"
                else -> null
            }
            state = state.copy(serialError = error)
            if (error == null && autosave()) {
                state = state.copy(savedTick = state.savedTick + 1)
                onSaved()
            }
        }
    }

    /** 自动保存草稿：流水号不合法或重复时跳过，不做任何自动修改。 */
    private suspend fun autosave(): Boolean {
        val s = state
        if (!s.loaded || s.billId == 0L) return false
        val serial = s.docSerial.trim()
        if (!serial.matches(Regex("\\d{3}"))) return false
        if (repo.serialConflict(s.docCode.ifBlank { "PH" }, s.docDate, serial, s.billId)) return false
        val bill = s.toBill(serial)
        val items = s.items.mapIndexed { index, row -> row.toEntity(s.billId, index) }
        repo.saveBill(bill, items)
        return true
    }

    private fun schedulePreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(300)
            val s = state
            if (!s.loaded) return@launch
            val qrBitmap = withContext(Dispatchers.Default) { repo.loadQrBitmap() }
            val id = InvoiceRenderBus.enqueue(
                s.toRenderInvoice().copy(qrBitmap = qrBitmap),
                StylePresets.resolve(s.presetKey, s.presets),
            )
            val bitmap = InvoiceRenderBus.await(id)
            if (bitmap != null) {
                state = state.copy(preview = bitmap)
            }
        }
    }
}

private fun BillItem.toRow(): ItemRow = ItemRow(
    name = name,
    spec = spec,
    unit = unit,
    qtyText = qty.toLong().toString(),
    priceText = Money.format(price),
    pack = pack,
    note = note,
)

private fun ItemRow.toEntity(billId: Long, order: Int): BillItem = BillItem(
    billId = billId,
    sortOrder = order,
    name = name,
    spec = spec,
    unit = unit,
    qty = qty(),
    price = price(),
    pack = pack,
    note = note,
)

private fun EditorUiState.toBill(serial: String): Bill = Bill(
    id = billId,
    customerName = customerName.trim(),
    customerPhone = customerPhone.trim().trimEnd(','),
    companyName = companyName.trim(),
    contactPhone = contactPhone.trim(),
    salesManager = salesManager.trim(),
    docCode = docCode.trim().ifBlank { "PH" },
    docSerial = serial,
    docDate = docDate,
    discount = discountText.toDoubleOrNull()?.let { Money.round2(it) } ?: 0.0,
    remark = remark,
    titleSuffix = titleSuffix.trim().ifBlank { "单据" },
    adText = adText,
    showManager = showManager,
    showRemark = showRemark,
    showAd = showAd,
    showWatermark = showWatermark,
    watermarkText = watermarkText,
    showContactPhone = showContactPhone,
    showMultiPhones = showMultiPhones,
    favorite = favorite,
    presetKey = presetKey,
    createdAt = createdAt,
)

fun EditorUiState.toRenderInvoice(): RenderInvoice = RenderInvoice(
    customerName = customerName,
    customerPhone = PhoneUtil.displayPhones(customerPhone, showMultiPhones),
    companyName = companyName,
    contactPhone = contactPhone,
    salesManager = salesManager,
    docCode = docCode,
    docSerial = docSerial,
    docDate = docDate,
    discount = discountText.toDoubleOrNull()?.let { Money.round2(it) } ?: 0.0,
    remark = remark,
    titleSuffix = titleSuffix,
    adText = adText,
    showManager = showManager,
    showRemark = showRemark,
    showAd = showAd,
    showWatermark = showWatermark,
    watermarkText = watermarkText,
    showContactPhone = showContactPhone,
    items = items.map {
        RenderItem(
            name = it.name,
            spec = it.spec,
            unit = it.unit,
            qty = it.qty(),
            price = it.price(),
            pack = it.pack,
            note = it.note,
        )
    },
)

fun presetDisplayName(presetKey: String, presets: List<StylePreset>): String {
    val customId = presetKey.removePrefix("custom:").toLongOrNull()
    if (customId != null) {
        presets.firstOrNull { it.id == customId }?.let { return it.name }
    }
    return StylePresets.builtInName(presetKey)
}

/** 当前单据字段 → 默认信息表单值。 */
fun EditorUiState.toDefaultInfoValues(): DefaultInfoValues = DefaultInfoValues(
    titleSuffix = titleSuffix,
    docCode = docCode,
    showMultiPhones = showMultiPhones,
    companyName = companyName,
    manager = salesManager,
    showManager = showManager,
    contactPhone = contactPhone,
    showContactPhone = showContactPhone,
    showRemark = showRemark,
    showAd = showAd,
    adText = adText,
    watermarkText = watermarkText,
    showWatermark = showWatermark,
)
