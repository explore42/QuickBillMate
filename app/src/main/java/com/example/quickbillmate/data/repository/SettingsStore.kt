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
        get() = prefs.getString(KEY_PRESET, "classic") ?: "classic"
        set(value) = prefs.edit().putString(KEY_PRESET, value).apply()

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        private const val KEY_THEME = "theme_mode"
        private const val KEY_COMPANY = "default_company"
        private const val KEY_PHONE = "default_phone"
        private const val KEY_MANAGER = "default_manager"
        private const val KEY_PRESET = "default_preset_key"
    }
}
