package com.example.quickbillmate.ui.editor

import android.app.Application
import android.graphics.Bitmap
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
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.util.DateUtils
import com.example.quickbillmate.util.Money
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
    val status: String = "草稿",
    val customerName: String = "",
    val customerPhone: String = "",
    val companyName: String = "",
    val contactPhone: String = "",
    val salesManager: String = "",
    val docCode: String = "XS",
    val docSerial: String = "",
    val docDate: String = DateUtils.today(),
    val discountText: String = "0.00",
    val remark: String = "",
    val titleSuffix: String = "销售清单",
    val disclaimer: String = "收到货物当日点清，如有问题请在2日内联系：",
    val showManager: Boolean = true,
    val showRemark: Boolean = true,
    val showWatermark: Boolean = false,
    val favorite: Boolean = false,
    val presetKey: String = "classic",
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

    /** 进入编辑页时的原始快照，用于“不保存”时恢复；新建单据时为 null。 */
    private var originalBill: Bill? = null
    private var originalItems: List<BillItem> = emptyList()
    private var isNewDraft = false

    init {
        viewModelScope.launch {
            repo.observeCustomers("").collect { customers ->
                state = state.copy(customers = customers)
                refreshSuggestions(state.customerName)
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
                docCode = "XS",
                docDate = DateUtils.today(),
                companyName = settings.defaultCompany,
                contactPhone = settings.defaultPhone,
                salesManager = settings.defaultManager,
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
            status = bill.status,
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
            disclaimer = bill.disclaimer,
            showManager = bill.showManager,
            showRemark = bill.showRemark,
            showWatermark = bill.showWatermark,
            favorite = bill.favorite,
            presetKey = bill.presetKey,
            items = items.map { it.toRow() }.ifEmpty { listOf(ItemRow()) },
        )
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
    fun onSerialChange(value: String) = update { copy(docSerial = value) }
    fun onDateChange(value: String) = update { copy(docDate = value) }
    fun onDiscountChange(value: String) = update { copy(discountText = value) }
    fun onRemarkChange(value: String) = update { copy(remark = value) }
    fun onTitleSuffixChange(value: String) = update { copy(titleSuffix = value) }
    fun onDisclaimerChange(value: String) = update { copy(disclaimer = value) }
    fun onShowManagerChange(value: Boolean) = update { copy(showManager = value) }
    fun onShowRemarkChange(value: Boolean) = update { copy(showRemark = value) }
    fun onShowWatermarkChange(value: Boolean) = update { copy(showWatermark = value) }

    fun onFavoriteChange(value: Boolean) = update { copy(favorite = value) }

    fun regenerateSerial() {
        viewModelScope.launch {
            val serial = repo.generateUniqueSerial(state.docCode.ifBlank { "XS" }, state.docDate)
            update { copy(docSerial = serial) }
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
        val libraryPool = state.customers.sortedWith(
            compareByDescending<Customer> { it.favorite }
                .thenByDescending { it.createdAt }
        )

        val fromDb = (if (q.isBlank()) libraryPool else libraryPool.filter {
            it.name.contains(q) || it.phone.contains(q)
        })
            .take(6)
            .map {
                CustomerSuggestion(
                    name = it.name,
                    phone = it.phone,
                    type = it.type,
                    fromDb = true,
                )
            }

        val dbKeys = fromDb.map { it.name to it.phone }.toSet()
        val fromContacts = if (state.contactsGranted) {
            val contacts = cachedContacts ?: ContactsImporter.query(app).also { cachedContacts = it }
            contacts
                .filter { q.isBlank() || it.name.contains(q) || it.phone.contains(q) }
                .filterNot { (it.name to it.phone) in dbKeys }
                .take(6 - fromDb.size)
                .map {
                    CustomerSuggestion(
                        name = it.name,
                        phone = it.phone,
                        fromDb = false,
                    )
                }
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

    // ---------- 示例 / 自动保存 / 手动保存 / 预览 ----------

    fun loadSample() {
        update {
            copy(
                customerName = "示例客户",
                customerPhone = "13800000000",
                companyName = "示例建材有限公司",
                contactPhone = "13800138000",
                salesManager = "李经理",
                docCode = "XS",
                docDate = DateUtils.today(),
                discountText = "0.00",
                remark = "客户自提",
                titleSuffix = "销售清单",
                disclaimer = "收到货物当日点清，如有问题请在2日内联系：",
                items = sampleItems(),
            )
        }
    }

    private fun sampleItems(): List<ItemRow> = listOf(
        ItemRow("腻子粉", "YGP800 20kg", "袋", "10", "35.00", "20袋/托", ""),
        ItemRow("墙衬", "YGP400 20kg", "袋", "5", "28.00", "20袋/托", ""),
        ItemRow("蓝和纸墙面保护膜", "3m*18m", "卷", "2", "350.00", "50卷/件", "现货"),
        ItemRow("乳胶漆", "净味五合一 18L", "桶", "3", "420.00", "4桶/件", ""),
        ItemRow("腻子粉", "YGP800 20kg", "袋", "4", "35.00", "20袋/托", "补单"),
        ItemRow("防水涂料", "JS-II 20kg", "桶", "2", "260.00", "4桶/件", ""),
        ItemRow("瓷砖胶", "C2 20kg", "袋", "8", "45.00", "20袋/托", ""),
        ItemRow("美缝剂", "瓷白色 400ml", "支", "12", "38.00", "50支/箱", ""),
        ItemRow("玻璃胶", "透明 300ml", "支", "6", "15.00", "50支/箱", ""),
    )

    private fun scheduleSave() {
        if (!loaded) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            autosave()
        }
    }

    /** 独立“保存”按钮：立即保存并提示。 */
    fun saveNow() {
        if (!loaded) return
        saveJob?.cancel()
        viewModelScope.launch {
            autosave()
            state = state.copy(savedTick = state.savedTick + 1)
        }
    }

    private suspend fun autosave() {
        val s = state
        if (!s.loaded || s.billId == 0L) return
        var serial = s.docSerial.trim()
        if (serial.isBlank() ||
            repo.serialConflict(s.docCode.ifBlank { "XS" }, s.docDate, serial, s.billId)
        ) {
            serial = repo.generateUniqueSerial(s.docCode.ifBlank { "XS" }, s.docDate)
            state = state.copy(docSerial = serial)
        }
        val bill = s.toBill(serial)
        val items = s.items.mapIndexed { index, row -> row.toEntity(s.billId, index) }
        repo.saveBill(bill, items)
    }

    private fun schedulePreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(300)
            val s = state
            if (!s.loaded) return@launch
            val bitmap = withContext(Dispatchers.Default) {
                repo.renderInvoice(s.toRenderInvoice(), s.presetKey, 1600)
            }
            state = state.copy(preview = bitmap)
        }
    }
}

private fun BillItem.toRow(): ItemRow = ItemRow(
    name = name,
    spec = spec,
    unit = unit,
    qtyText = Money.format(qty),
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
    customerPhone = customerPhone.trim(),
    companyName = companyName.trim(),
    contactPhone = contactPhone.trim(),
    salesManager = salesManager.trim(),
    docCode = docCode.trim().ifBlank { "XS" },
    docSerial = serial,
    docDate = docDate,
    discount = discountText.toDoubleOrNull()?.let { Money.round2(it) } ?: 0.0,
    remark = remark,
    titleSuffix = titleSuffix.trim().ifBlank { "销售清单" },
    disclaimer = disclaimer,
    showManager = showManager,
    showRemark = showRemark,
    showWatermark = showWatermark,
    favorite = favorite,
    presetKey = presetKey,
    status = status,
    createdAt = createdAt,
)

fun EditorUiState.toRenderInvoice(): RenderInvoice = RenderInvoice(
    customerName = customerName,
    customerPhone = customerPhone,
    companyName = companyName,
    contactPhone = contactPhone,
    salesManager = salesManager,
    docCode = docCode,
    docSerial = docSerial,
    docDate = docDate,
    discount = discountText.toDoubleOrNull()?.let { Money.round2(it) } ?: 0.0,
    remark = remark,
    titleSuffix = titleSuffix,
    disclaimer = disclaimer,
    showManager = showManager,
    showRemark = showRemark,
    showWatermark = showWatermark,
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
