package com.example.quickbillmate.ui.presets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.render.StyleParams
import com.example.quickbillmate.render.StylePresets
import kotlinx.coroutines.launch

class PresetEditorViewModel(private val repo: AppRepository) : ViewModel() {

    var loaded by mutableStateOf(false)
        private set
    var name by mutableStateOf("")
        private set
    var params by mutableStateOf(StyleParams())
        private set

    private var presetId = 0L

    fun load(presetId: Long, base: String) {
        viewModelScope.launch {
            val presets = repo.getPresets()
            val existing = presets.firstOrNull { it.id == presetId }
            if (existing != null) {
                this@PresetEditorViewModel.presetId = existing.id
                name = existing.name
                params = StyleParams.fromJson(existing.paramsJson)
            } else {
                this@PresetEditorViewModel.presetId = 0
                params = StylePresets.resolve(base, presets)
                name = if (base == "classic") "我的预设" else StylePresets.builtInName(base) + " 副本"
            }
            loaded = true
        }
    }

    fun updateName(value: String) {
        name = value
    }

    fun updateParams(transform: (StyleParams) -> StyleParams) {
        params = transform(params)
    }

    fun restoreDefaults() {
        params = StyleParams()
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.savePreset(
                StylePreset(
                    id = presetId,
                    name = name.ifBlank { "未命名预设" },
                    paramsJson = params.toJson(),
                )
            )
            onDone()
        }
    }
}
