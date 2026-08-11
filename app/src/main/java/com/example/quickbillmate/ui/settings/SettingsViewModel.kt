package com.example.quickbillmate.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.ui.common.DefaultInfoValues
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
    var defaultShowManager by mutableStateOf(repo.settings.defaultShowManager)
        private set
    var defaultShowRemark by mutableStateOf(repo.settings.defaultShowRemark)
        private set
    var defaultShowWatermark by mutableStateOf(repo.settings.defaultShowWatermark)
        private set
    var defaultShowMultiPhones by mutableStateOf(repo.settings.defaultShowMultiPhones)
        private set
    var defaultDocCode by mutableStateOf(repo.settings.defaultDocCode)
        private set
    var defaultTitleSuffix by mutableStateOf(repo.settings.defaultTitleSuffix)
        private set
    var defaultAdText by mutableStateOf(repo.settings.defaultAdText)
        private set
    var defaultShowAd by mutableStateOf(repo.settings.defaultShowAd)
        private set
    var defaultRemark by mutableStateOf(repo.settings.defaultRemark)
        private set
    var defaultWatermarkText by mutableStateOf(repo.settings.defaultWatermarkText)
        private set
    var defaultShowContactPhone by mutableStateOf(repo.settings.defaultShowContactPhone)
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

    fun updateDefaultPreset(key: String) {
        defaultPresetKey = key
        repo.settings.defaultPresetKey = key
    }

    /** 保存“默认信息”：全部写入全局默认值。 */
    fun updateDefaults(values: DefaultInfoValues) {
        defaultTitleSuffix = values.titleSuffix
        defaultDocCode = values.docCode
        defaultShowMultiPhones = values.showMultiPhones
        defaultCompany = values.companyName
        defaultManager = values.manager
        defaultShowManager = values.showManager
        defaultPhone = values.contactPhone
        defaultShowContactPhone = values.showContactPhone
        defaultShowRemark = values.showRemark
        defaultShowAd = values.showAd
        defaultRemark = values.remark
        defaultAdText = values.adText
        defaultWatermarkText = values.watermarkText
        defaultShowWatermark = values.showWatermark
        val s = repo.settings
        s.defaultTitleSuffix = values.titleSuffix
        s.defaultDocCode = values.docCode
        s.defaultShowMultiPhones = values.showMultiPhones
        s.defaultCompany = values.companyName
        s.defaultManager = values.manager
        s.defaultShowManager = values.showManager
        s.defaultPhone = values.contactPhone
        s.defaultShowContactPhone = values.showContactPhone
        s.defaultShowRemark = values.showRemark
        s.defaultShowAd = values.showAd
        s.defaultRemark = values.remark
        s.defaultAdText = values.adText
        s.defaultWatermarkText = values.watermarkText
        s.defaultShowWatermark = values.showWatermark
    }
}
