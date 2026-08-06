package com.example.quickbillmate.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {

    var themeMode by mutableStateOf(repo.settings.themeMode)
        private set
    var defaultCompany by mutableStateOf(repo.settings.defaultCompany)
        private set
    var defaultPhone by mutableStateOf(repo.settings.defaultPhone)
        private set
    var defaultManager by mutableStateOf(repo.settings.defaultManager)
        private set
    var defaultPresetKey by mutableStateOf(repo.settings.defaultPresetKey)
        private set
    var versionName by mutableStateOf("")

    val presets: StateFlow<List<StylePreset>> = repo.observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        versionName = runCatching {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: ""
        }.getOrElse { "" }
    }

    fun updateThemeMode(mode: String) {
        themeMode = mode
        repo.settings.themeMode = mode
    }

    fun updateCompany(value: String) {
        defaultCompany = value
        repo.settings.defaultCompany = value
    }

    fun updatePhone(value: String) {
        defaultPhone = value
        repo.settings.defaultPhone = value
    }

    fun updateManager(value: String) {
        defaultManager = value
        repo.settings.defaultManager = value
    }

    fun updateDefaultPreset(key: String) {
        defaultPresetKey = key
        repo.settings.defaultPresetKey = key
    }
}
