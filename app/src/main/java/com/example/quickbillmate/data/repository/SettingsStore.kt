package com.example.quickbillmate.data.repository

import android.content.Context

/** 应用偏好：主题、默认公司信息、默认预设。 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("quickbillmate_settings", Context.MODE_PRIVATE)

    var themeMode: String
        get() = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var defaultCompany: String
        get() = prefs.getString(KEY_COMPANY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_COMPANY, value).apply()

    var defaultPhone: String
        get() = prefs.getString(KEY_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHONE, value).apply()

    var defaultManager: String
        get() = prefs.getString(KEY_MANAGER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MANAGER, value).apply()

    var defaultPresetKey: String
        get() = prefs.getString(KEY_PRESET, "classic_plain") ?: "classic_plain"
        set(value) = prefs.edit().putString(KEY_PRESET, value).apply()

    var defaultShowManager: Boolean
        get() = prefs.getBoolean(KEY_SHOW_MANAGER, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_MANAGER, value).apply()

    var defaultShowRemark: Boolean
        get() = prefs.getBoolean(KEY_SHOW_REMARK, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_REMARK, value).apply()

    var defaultShowWatermark: Boolean
        get() = prefs.getBoolean(KEY_SHOW_WATERMARK, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_WATERMARK, value).apply()

    var defaultShowMultiPhones: Boolean
        get() = prefs.getBoolean(KEY_SHOW_MULTI_PHONES, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_MULTI_PHONES, value).apply()

    var defaultDocCode: String
        get() = prefs.getString(KEY_DOC_CODE, "PH") ?: "PH"
        set(value) = prefs.edit().putString(KEY_DOC_CODE, value).apply()

    var defaultTitleSuffix: String
        get() = prefs.getString(KEY_TITLE_SUFFIX, "单据") ?: "单据"
        set(value) = prefs.edit().putString(KEY_TITLE_SUFFIX, value).apply()

    var defaultAdText: String
        get() = prefs.getString(KEY_AD_TEXT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AD_TEXT, value).apply()

    var defaultShowAd: Boolean
        get() = prefs.getBoolean(KEY_SHOW_AD, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_AD, value).apply()

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        private const val KEY_THEME = "theme_mode"
        private const val KEY_COMPANY = "default_company"
        private const val KEY_PHONE = "default_phone"
        private const val KEY_MANAGER = "default_manager"
        private const val KEY_PRESET = "default_preset_key"
        private const val KEY_SHOW_MANAGER = "default_show_manager"
        private const val KEY_SHOW_REMARK = "default_show_remark"
        private const val KEY_SHOW_WATERMARK = "default_show_watermark"
        private const val KEY_SHOW_MULTI_PHONES = "default_show_multi_phones"
        private const val KEY_DOC_CODE = "default_doc_code"
        private const val KEY_TITLE_SUFFIX = "default_title_suffix"
        private const val KEY_AD_TEXT = "default_ad_text"
        private const val KEY_SHOW_AD = "default_show_ad"
    }
}
