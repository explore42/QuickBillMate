package com.example.quickbillmate.ui.presets

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.render.InvoiceRenderBus
import com.example.quickbillmate.render.RenderInvoice
import com.example.quickbillmate.render.RenderItem
import com.example.quickbillmate.render.StylePresets
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PresetsViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {

    val presets: StateFlow<List<StylePreset>> = repo.observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var defaultKey by mutableStateOf(repo.settings.defaultPresetKey)
        private set

    /** key -> 缩略预览位图 */
    var previews by mutableStateOf<Map<String, Bitmap>>(emptyMap())
        private set

    private val sampleInvoice = RenderInvoice(
        customerName = "示例客户",
        customerPhone = "13800000000",
        companyName = "示例建材有限公司",
        contactPhone = "13800138000",
        salesManager = "李经理",
        docCode = "XS",
        docSerial = "482",
        docDate = "2025-11-21",
        discount = 12.0,
        items = listOf(
            RenderItem("腻子粉", "YGP800 20kg", "袋", 10.0, 35.0, "20袋/托", ""),
            RenderItem("墙衬", "YGP400 20kg", "袋", 5.0, 28.0, "20袋/托", ""),
            RenderItem("乳胶漆", "净味 18L", "桶", 3.0, 420.0, "", ""),
        ),
    )

    init {
        viewModelScope.launch {
            val customs = repo.getPresets()
            val keys = StylePresets.builtIns.map { it.key } + customs.map { "custom:${it.id}" }
            val paramsByKey = keys.associateWith { key -> StylePresets.resolve(key, customs) }
            val map = keys.mapNotNull { key ->
                val id = InvoiceRenderBus.enqueue(sampleInvoice, paramsByKey.getValue(key))
                val bitmap = InvoiceRenderBus.await(id) ?: return@mapNotNull null
                key to bitmap
            }
                .toMap()
            previews = map
        }
    }

    fun setDefault(key: String) {
        defaultKey = key
        repo.settings.defaultPresetKey = key
    }

    fun duplicatePreset(key: String, customs: List<StylePreset>) {
        viewModelScope.launch {
            val params = StylePresets.resolve(key, customs)
            val baseName = if (key.startsWith("custom:")) {
                val id = key.removePrefix("custom:").toLongOrNull()
                customs.firstOrNull { it.id == id }?.name ?: "我的预设"
            } else {
                StylePresets.builtInName(key)
            }
            repo.savePreset(StylePreset(name = "$baseName 副本", paramsJson = params.toJson()))
        }
    }

    fun deletePreset(preset: StylePreset) {
        viewModelScope.launch {
            repo.deletePreset(preset)
            if (defaultKey == "custom:${preset.id}") {
                setDefault("classic")
            }
        }
    }
}
