package com.example.quickbillmate.navigation

object Routes {
    const val TABS = "tabs"
    const val ONBOARDING = "onboarding"
    const val DATA_MANAGER = "data_manager"
    const val REPORT = "report"
    const val HOME = "home"
    const val PRODUCTS = "products"
    const val CUSTOMERS = "customers"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{billId}"
    const val VIEW = "bill_view/{billId}"
    const val PRESETS = "presets"
    const val PRESET_EDITOR = "preset_editor?presetId={presetId}&base={base}"
    const val CONTACTS_IMPORT = "contacts_import"

    const val EDITOR_ARG_BILL_ID = "billId"
    const val VIEW_ARG_BILL_ID = "billId"
    const val PRESET_EDITOR_ARG_ID = "presetId"
    const val PRESET_EDITOR_ARG_BASE = "base"

    fun editor(billId: Long) = "editor/$billId"

    fun view(billId: Long) = "bill_view/$billId"

    fun presetEditor(presetId: Long = 0, base: String = "classic") =
        "preset_editor?presetId=$presetId&base=$base"

    val tabRoutes = listOf(HOME, PRODUCTS, CUSTOMERS, SETTINGS)
}
