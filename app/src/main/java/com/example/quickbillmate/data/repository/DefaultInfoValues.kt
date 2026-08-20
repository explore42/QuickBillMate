package com.example.quickbillmate.data.repository

/** “默认信息”表单的数据集合：设置页（全局默认）、单据编辑页（仅当前单据）与导入导出共用。 */
data class DefaultInfoValues(
    val titleSuffix: String = "单据",
    val docCode: String = "PH",
    val showCustomerPhone: Boolean = false,
    val showMultiPhones: Boolean = false,
    val companyName: String = "",
    val manager: String = "",
    val showManager: Boolean = true,
    val contactPhone: String = "",
    val showContactPhone: Boolean = true,
    val remark: String = "",
    val showRemark: Boolean = true,
    val showAd: Boolean = false,
    val adText: String = "",
    val watermarkText: String = "",
    val showWatermark: Boolean = false,
)
