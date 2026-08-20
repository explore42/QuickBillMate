package com.example.quickbillmate.data.repository

import android.content.Context
import androidx.core.content.edit

/** 应用偏好：主题、默认公司信息、默认预设。 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("quickbillmate_settings", Context.MODE_PRIVATE)

    var themeMode: String
        get() = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit { putString(KEY_THEME, value) }

    /** Monet 动态取色开关，默认开启。 */
    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) = prefs.edit { putBoolean(KEY_DYNAMIC_COLOR, value) }

    /** 主题种子色（ARGB Long），0L 表示跟随壁纸；默认品牌紫 #9C11E1。 */
    var themeKeyColor: Long
        get() = prefs.getLong(KEY_THEME_KEY_COLOR, 0xFF9C11E1L)
        set(value) = prefs.edit { putLong(KEY_THEME_KEY_COLOR, value) }

    /** Monet 配色风格（ThemePaletteStyle 名称），默认 TonalSpot。 */
    var themePaletteStyle: String
        get() = prefs.getString(KEY_THEME_PALETTE_STYLE, "TonalSpot") ?: "TonalSpot"
        set(value) = prefs.edit { putString(KEY_THEME_PALETTE_STYLE, value) }

    /** 触觉反馈开关，默认开启。 */
    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit { putBoolean(KEY_HAPTICS, value) }

    var defaultCompany: String
        get() = prefs.getString(KEY_COMPANY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_COMPANY, value) }

    var defaultPhone: String
        get() = prefs.getString(KEY_PHONE, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PHONE, value) }

    var defaultManager: String
        get() = prefs.getString(KEY_MANAGER, "") ?: ""
        set(value) = prefs.edit { putString(KEY_MANAGER, value) }

    var defaultPresetKey: String
        get() = prefs.getString(KEY_PRESET, "classic_plain") ?: "classic_plain"
        set(value) = prefs.edit { putString(KEY_PRESET, value) }

    var defaultShowManager: Boolean
        get() = prefs.getBoolean(KEY_SHOW_MANAGER, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_MANAGER, value) }

    var defaultShowRemark: Boolean
        get() = prefs.getBoolean(KEY_SHOW_REMARK, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_REMARK, value) }

    var defaultShowWatermark: Boolean
        get() = prefs.getBoolean(KEY_SHOW_WATERMARK, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_WATERMARK, value) }

    var defaultShowMultiPhones: Boolean
        get() = prefs.getBoolean(KEY_SHOW_MULTI_PHONES, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_MULTI_PHONES, value) }

    /** 是否在单据上显示客户电话，默认不显示；开启后数量由“多个电话”开关控制。 */
    var defaultShowCustomerPhone: Boolean
        get() = prefs.getBoolean(KEY_SHOW_CUSTOMER_PHONE, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_CUSTOMER_PHONE, value) }

    var defaultDocCode: String
        get() = prefs.getString(KEY_DOC_CODE, "PH") ?: "PH"
        set(value) = prefs.edit { putString(KEY_DOC_CODE, value) }

    var defaultTitleSuffix: String
        get() = prefs.getString(KEY_TITLE_SUFFIX, "单据") ?: "单据"
        set(value) = prefs.edit { putString(KEY_TITLE_SUFFIX, value) }

    var defaultAdText: String
        get() = prefs.getString(KEY_AD_TEXT, "") ?: ""
        set(value) = prefs.edit { putString(KEY_AD_TEXT, value) }

    var defaultShowAd: Boolean
        get() = prefs.getBoolean(KEY_SHOW_AD, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_AD, value) }

    var defaultRemark: String
        get() = prefs.getString(KEY_REMARK, "") ?: ""
        set(value) = prefs.edit { putString(KEY_REMARK, value) }

    var defaultWatermarkText: String
        get() = prefs.getString(KEY_WATERMARK_TEXT, "") ?: ""
        set(value) = prefs.edit { putString(KEY_WATERMARK_TEXT, value) }

    var defaultShowContactPhone: Boolean
        get() = prefs.getBoolean(KEY_SHOW_CONTACT_PHONE, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_CONTACT_PHONE, value) }

    /** 商品单位预设的自定义部分（逗号分隔存储；内置预设见 [BUILTIN_UNITS]）。 */
    var customUnits: List<String>
        get() = prefs.getString(KEY_CUSTOM_UNITS, "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        set(value) = prefs.edit { putString(KEY_CUSTOM_UNITS, value.joinToString(",")) }

    /** 首次安装引导页是否已完成（老用户升级不显示引导）。 */
    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, value) }

    /** 批量写入“默认信息”：引导页与设置页共用入口。 */
    fun applyDefaults(values: DefaultInfoValues) {
        defaultTitleSuffix = values.titleSuffix
        defaultDocCode = values.docCode
        defaultShowMultiPhones = values.showMultiPhones
        defaultShowCustomerPhone = values.showCustomerPhone
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
    }

    /** 当前“默认信息”快照（引导页预填与导出共用）。 */
    fun defaultsSnapshot(): DefaultInfoValues = DefaultInfoValues(
        titleSuffix = defaultTitleSuffix,
        docCode = defaultDocCode,
        showCustomerPhone = defaultShowCustomerPhone,
        showMultiPhones = defaultShowMultiPhones,
        companyName = defaultCompany,
        manager = defaultManager,
        showManager = defaultShowManager,
        contactPhone = defaultPhone,
        showContactPhone = defaultShowContactPhone,
        showRemark = defaultShowRemark,
        showAd = defaultShowAd,
        remark = defaultRemark,
        adText = defaultAdText,
        watermarkText = defaultWatermarkText,
        showWatermark = defaultShowWatermark,
    )

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        /** 商品单位内置预设，与自定义预设合并后供新增商品下拉选择。 */
        val BUILTIN_UNITS = listOf("桶", "筒", "代", "张", "卷", "支", "把")

        private const val KEY_THEME = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "theme_dynamic_color"
        private const val KEY_THEME_KEY_COLOR = "theme_key_color"
        private const val KEY_THEME_PALETTE_STYLE = "theme_palette_style"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_COMPANY = "default_company"
        private const val KEY_PHONE = "default_phone"
        private const val KEY_MANAGER = "default_manager"
        private const val KEY_PRESET = "default_preset_key"
        private const val KEY_SHOW_MANAGER = "default_show_manager"
        private const val KEY_SHOW_REMARK = "default_show_remark"
        private const val KEY_SHOW_WATERMARK = "default_show_watermark"
        private const val KEY_SHOW_MULTI_PHONES = "default_show_multi_phones"
        private const val KEY_SHOW_CUSTOMER_PHONE = "default_show_customer_phone"
        private const val KEY_DOC_CODE = "default_doc_code"
        private const val KEY_TITLE_SUFFIX = "default_title_suffix"
        private const val KEY_AD_TEXT = "default_ad_text"
        private const val KEY_SHOW_AD = "default_show_ad"
        private const val KEY_REMARK = "default_remark"
        private const val KEY_WATERMARK_TEXT = "default_watermark_text"
        private const val KEY_SHOW_CONTACT_PHONE = "default_show_contact_phone"
        private const val KEY_CUSTOM_UNITS = "custom_units"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
