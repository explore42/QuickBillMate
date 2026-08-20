package com.example.quickbillmate.importexport

import com.example.quickbillmate.data.db.Customer
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
import com.example.quickbillmate.util.PhoneUtil

/** 客户 JSON 导入结果：新增 / 合并（同名） / 失败。 */
data class CustomerImportResult(
    val success: Int,
    val skipped: Int,
    val failures: List<Pair<Int, String>>,
)

/** 客户 JSON 导出 / 解析（选中客户导出用）。 */
object CustomerJsonCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun export(customers: List<Customer>): String {
        val root = buildJsonObject {
            put("version", 1)
            put("type", "customers")
            put(
                "customers",
                buildJsonArray {
                    customers.forEach { c ->
                        add(
                            buildJsonObject {
                                put("name", c.name)
                                put("phone", c.phone)
                                put("type", c.type)
                                put("remark", c.remark)
                                put("favorite", c.favorite)
                            }
                        )
                    }
                }
            )
        }
        return json.encodeToString(JsonObject.serializer(), root)
    }

    fun parse(text: String): List<Customer> {
        val root = json.parseToJsonElement(text) as? JsonObject ?: return emptyList()
        val array = root["customers"] as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            val name = (o["name"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            Customer(
                name = name,
                phone = (o["phone"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                type = (o["type"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                remark = (o["remark"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                favorite = (o["favorite"] as? JsonPrimitive)?.booleanOrNull ?: false,
            )
        }
    }
}

/** 客户导入合并规则：按姓名合并，电话规范化后去重追加（纯函数，便于单元测试）。 */
object CustomerImportMerger {
    fun mergePhones(existing: String, incoming: List<String>): String {
        val all = PhoneUtil.splitPhones(existing).toMutableList()
        incoming.forEach { phone ->
            val normalized = PhoneUtil.normalizePhone(phone)
            if (normalized.isNotEmpty() && normalized !in all) all.add(normalized)
        }
        return all.joinToString(",")
    }
}
