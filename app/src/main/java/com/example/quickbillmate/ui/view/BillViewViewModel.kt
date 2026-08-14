package com.example.quickbillmate.ui.view

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.render.InvoiceRenderBus
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.util.shrinkForPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ViewOutcome(
    val saved: Boolean,
    val fileName: String,
    val shareUri: Uri?,
    val message: String,
)

data class BillViewState(
    val loaded: Boolean = false,
    val bill: Bill? = null,
    val items: List<BillItem> = emptyList(),
    val presets: List<StylePreset> = emptyList(),
    val preview: Bitmap? = null,
    val exporting: Boolean = false,
    val exportOutcome: ViewOutcome? = null,
    val shareOutcome: ViewOutcome? = null,
)

class BillViewViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {

    var state by mutableStateOf(BillViewState())
        private set

    fun load(billId: Long) {
        viewModelScope.launch {
            val bill = repo.getBill(billId) ?: return@launch
            val items = repo.getItems(billId)
            val presets = repo.getPresets()
            state = BillViewState(
                loaded = true,
                bill = bill,
                items = items,
                presets = presets,
            )
            val qrBitmap = withContext(Dispatchers.Default) { repo.loadQrBitmap() }
            val id = InvoiceRenderBus.enqueue(
                repo.buildRenderInvoice(bill, items, qrBitmap),
                StylePresets.resolve(bill.presetKey, presets),
            )
            val preview = InvoiceRenderBus.await(id)
            if (preview != null) {
                state = state.copy(preview = preview.shrinkForPreview())
            }
        }
    }

    /** 导出/分享用：按当前单据重新渲染全分辨率位图，不常驻内存。 */
    private suspend fun renderFullRes(): Bitmap? {
        val bill = state.bill ?: return null
        val items = state.items
        val presets = state.presets
        val qrBitmap = withContext(Dispatchers.Default) { repo.loadQrBitmap() }
        val id = InvoiceRenderBus.enqueue(
            repo.buildRenderInvoice(bill, items, qrBitmap),
            StylePresets.resolve(bill.presetKey, presets),
        )
        return InvoiceRenderBus.await(id)
    }

    fun exportToGallery() {
        val bill = state.bill ?: return
        if (state.exporting) return
        viewModelScope.launch {
            state = state.copy(exporting = true)
            val fullRes = renderFullRes()
            val ok = fullRes != null && withContext(Dispatchers.IO) {
                repo.exportBitmapToGallery(app, bill, fullRes)
            }
            val shareUri = fullRes?.let { repo.shareBill(app, bill, it) }
            state = state.copy(
                exporting = false,
                exportOutcome = ViewOutcome(
                    saved = ok,
                    fileName = repo.billFileName(bill),
                    shareUri = shareUri,
                    message = when {
                        fullRes == null -> "渲染失败，请重试"
                        ok -> "已保存到相册"
                        else -> "保存失败，请检查存储空间或权限"
                    },
                ),
            )
        }
    }

    fun shareNow() {
        val bill = state.bill ?: return
        if (state.exporting) return
        viewModelScope.launch {
            state = state.copy(exporting = true)
            val fullRes = renderFullRes()
            if (fullRes == null) {
                state = state.copy(
                    exporting = false,
                    exportOutcome = ViewOutcome(
                        saved = false,
                        fileName = repo.billFileName(bill),
                        shareUri = null,
                        message = "渲染失败，请重试",
                    ),
                )
                return@launch
            }
            val uri = withContext(Dispatchers.IO) {
                repo.shareBill(app, bill, fullRes)
            }
            state = state.copy(
                exporting = false,
                shareOutcome = ViewOutcome(
                    saved = true,
                    fileName = repo.billFileName(bill),
                    shareUri = uri,
                    message = "",
                ),
            )
        }
    }

    fun consumeExportOutcome() {
        state = state.copy(exportOutcome = null)
    }

    fun consumeShareOutcome() {
        state = state.copy(shareOutcome = null)
    }
}
