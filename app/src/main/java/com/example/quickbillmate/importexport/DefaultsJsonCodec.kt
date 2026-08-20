package com.example.quickbillmate.importexport

import com.example.quickbillmate.data.repository.DefaultInfoValues
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/** “默认信息”导出结果：全部默认字段 + 预置单位（不含微信二维码）。 */
data class DefaultsExport(
    val values: DefaultInfoValues,
    val customUnits: List<String>?,
    /** defaults 对象中实际出现的字段名（部分导出时用于按字段覆盖）。 */
    val presentFields: Set<String>,
)

/** “默认信息”可导出的分组（“选择导出”粒度）。 */
enum class DefaultsGroup(val label: String) {
    TITLE("标题栏"),
    COMPANY("公司信息"),
    OTHER("其他信息"),
    UNITS("预置单位"),
}

/**
 * “默认信息” JSON 导出 / 解析。
 * 导出支持按 [DefaultsGroup] 选择部分字段；导入只覆盖文件中出现的字段。
 */
object DefaultsJsonCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun export(
        values: DefaultInfoValues,
        customUnits: List<String>,
        groups: Set<DefaultsGroup> = DefaultsGroup.entries.toSet(),
    ): String {
        val root = buildJsonObject {
            put("version", 1)
            put("type", "defaults")
            put(
                "defaults",
                buildJsonObject {
                    if (DefaultsGroup.TITLE in groups) {
                        put("titleSuffix", values.titleSuffix)
                        put("docCode", values.docCode)
                    }
                    if (DefaultsGroup.COMPANY in groups) {
                        put("companyName", values.companyName)
                        put("manager", values.manager)
                        put("showManager", values.showManager)
                        put("contactPhone", values.contactPhone)
                        put("showContactPhone", values.showContactPhone)
                    }
                    if (DefaultsGroup.OTHER in groups) {
                        put("showCustomerPhone", values.showCustomerPhone)
                        put("showMultiPhones", values.showMultiPhones)
                        put("showRemark", values.showRemark)
                        put("showAd", values.showAd)
                        put("remark", values.remark)
                        put("adText", values.adText)
                        put("watermarkText", values.watermarkText)
                        put("showWatermark", values.showWatermark)
                    }
                }
            )
            if (DefaultsGroup.UNITS in groups) {
                put(
                    "customUnits",
                    buildJsonArray { customUnits.forEach { add(it) } }
                )
            }
        }
        return json.encodeToString(JsonObject.serializer(), root)
    }

    fun parse(text: String): DefaultsExport {
        val root = try {
            json.parseToJsonElement(text)
        } catch (_: Exception) {
            throw DataImportException("JSON 格式错误")
        } as? JsonObject ?: throw DataImportException("JSON 格式错误")
        val d = root["defaults"] as? JsonObject ?: throw DataImportException("缺少 defaults 对象")
        return DefaultsExport(
            values = DefaultInfoValues(
                titleSuffix = d.string("titleSuffix") ?: "单据",
                docCode = d.string("docCode") ?: "PH",
                showCustomerPhone = d.bool("showCustomerPhone") ?: false,
                showMultiPhones = d.bool("showMultiPhones") ?: false,
                companyName = d.string("companyName").orEmpty(),
                manager = d.string("manager").orEmpty(),
                showManager = d.bool("showManager") ?: true,
                contactPhone = d.string("contactPhone").orEmpty(),
                showContactPhone = d.bool("showContactPhone") ?: true,
                remark = d.string("remark").orEmpty(),
                showRemark = d.bool("showRemark") ?: true,
                showAd = d.bool("showAd") ?: false,
                adText = d.string("adText").orEmpty(),
                watermarkText = d.string("watermarkText").orEmpty(),
                showWatermark = d.bool("showWatermark") ?: false,
            ),
            customUnits = (root["customUnits"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
            presentFields = d.keys.toSet(),
        )
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()

    private fun JsonObject.bool(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.booleanOrNull
}
