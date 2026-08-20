package com.example.quickbillmate.importexport

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** 设置页数据导入导出的四大分类。 */
enum class DataCategory(val type: String, val label: String) {
    BILLS("bills", "单据"),
    PRODUCTS("products", "商品"),
    CUSTOMERS("customers", "客户"),
    DEFAULTS("defaults", "默认信息"),
    ;

    companion object {
        fun fromType(type: String?): DataCategory? = entries.firstOrNull { it.type == type }

        /**
         * 从 JSON 文本识别分类：优先读取根对象 `type` 字段；旧文件（无 type）按数组名兜底。
         */
        fun detect(text: String): DataCategory? {
            val root = runCatching {
                Json.parseToJsonElement(text) as? JsonObject
            }.getOrNull() ?: return null
            fromType((root["type"] as? JsonPrimitive)?.contentOrNull)?.let { return it }
            return when {
                root["bills"] is JsonArray -> BILLS
                root["products"] is JsonArray -> PRODUCTS
                root["customers"] is JsonArray -> CUSTOMERS
                root["defaults"] is JsonObject -> DEFAULTS
                else -> null
            }
        }
    }
}
