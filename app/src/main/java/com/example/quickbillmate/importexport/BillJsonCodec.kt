package com.example.quickbillmate.importexport

import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/** 单据及其商品行（导入导出用）。 */
data class BillWithItems(
    val bill: Bill,
    val items: List<BillItem>,
)

/** 单据 JSON 导入结果。 */
data class BillImportResult(
    val success: Int,
    val failures: List<Pair<Int, String>>,
    val imported: List<BillWithItems>,
)

/** 单据 JSON 导出 / 解析：单据与商品行嵌套在同一个对象中，不含 id/时间戳。 */
object BillJsonCodec {
    const val MAX_SIZE_BYTES = 2 * 1024 * 1024

    private val json = Json { ignoreUnknownKeys = true }

    fun export(bills: List<BillWithItems>): String {
        val root = buildJsonObject {
            put("version", 1)
            put("type", "bills")
            put(
                "bills",
                buildJsonArray {
                    bills.forEach { bw ->
                        val b = bw.bill
                        add(
                            buildJsonObject {
                                put("customerName", b.customerName)
                                put("customerPhone", b.customerPhone)
                                put("companyName", b.companyName)
                                put("contactPhone", b.contactPhone)
                                put("salesManager", b.salesManager)
                                put("docCode", b.docCode)
                                put("docSerial", b.docSerial)
                                put("docDate", b.docDate)
                                put("discount", b.discount)
                                put("remark", b.remark)
                                put("titleSuffix", b.titleSuffix)
                                put("adText", b.adText)
                                put("showManager", b.showManager)
                                put("showRemark", b.showRemark)
                                put("showAd", b.showAd)
                                put("showWatermark", b.showWatermark)
                                put("watermarkText", b.watermarkText)
                                put("showMultiPhones", b.showMultiPhones)
                                put("showCustomerPhone", b.showCustomerPhone)
                                put("showContactPhone", b.showContactPhone)
                                put("favorite", b.favorite)
                                put("presetKey", b.presetKey)
                                put(
                                    "items",
                                    buildJsonArray {
                                        bw.items.sortedBy { it.sortOrder }.forEach { i ->
                                            add(
                                                buildJsonObject {
                                                    put("name", i.name)
                                                    put("spec", i.spec)
                                                    put("unit", i.unit)
                                                    put("qty", i.qty)
                                                    put("price", i.price)
                                                    put("pack", i.pack)
                                                    put("note", i.note)
                                                    put("sortOrder", i.sortOrder)
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            )
        }
        return json.encodeToString(JsonObject.serializer(), root)
    }

    fun parse(text: String): BillImportResult {
        if (text.toByteArray(Charsets.UTF_8).size > MAX_SIZE_BYTES) {
            throw DataImportException("文件超过 2MB，已拒绝导入")
        }
        val root = try {
            json.parseToJsonElement(text)
        } catch (_: Exception) {
            throw DataImportException("JSON 格式错误")
        } as? JsonObject ?: throw DataImportException("JSON 格式错误")

        val array = root["bills"] as? JsonArray ?: throw DataImportException("缺少 bills 数组")
        var success = 0
        val failures = mutableListOf<Pair<Int, String>>()
        val imported = mutableListOf<BillWithItems>()

        array.forEachIndexed { index, element ->
            val row = index + 1
            val obj = element as? JsonObject
            if (obj == null) {
                failures.add(row to "不是对象")
                return@forEachIndexed
            }
            val items = buildList {
                val arr = obj["items"] as? JsonArray
                if (arr != null) {
                    var valid = true
                    arr.forEachIndexed { i, itemElement ->
                        val o = itemElement as? JsonObject
                        if (o == null) {
                            valid = false
                            return@forEachIndexed
                        }
                        val qty = o.double("qty") ?: 1.0
                        val price = o.double("price") ?: 0.0
                        if (qty < 0 || price < 0) {
                            valid = false
                            return@forEachIndexed
                        }
                        add(
                            BillItem(
                                name = o.string("name"),
                                spec = o.string("spec"),
                                unit = o.string("unit").ifBlank { "桶" },
                                qty = qty,
                                price = price,
                                pack = o.string("pack"),
                                note = o.string("note"),
                                sortOrder = i,
                            )
                        )
                    }
                    if (!valid) {
                        failures.add(row to "商品行数据非法")
                        return@forEachIndexed
                    }
                }
            }
            imported.add(
                BillWithItems(
                    bill = Bill(
                        customerName = obj.string("customerName"),
                        customerPhone = obj.string("customerPhone"),
                        companyName = obj.string("companyName"),
                        contactPhone = obj.string("contactPhone"),
                        salesManager = obj.string("salesManager"),
                        docCode = obj.string("docCode").ifBlank { "PH" },
                        docSerial = obj.string("docSerial").ifBlank { "000" },
                        docDate = obj.string("docDate"),
                        discount = obj.double("discount") ?: 0.0,
                        remark = obj.string("remark"),
                        titleSuffix = obj.string("titleSuffix").ifBlank { "单据" },
                        adText = obj.string("adText"),
                        showManager = obj.bool("showManager", true),
                        showRemark = obj.bool("showRemark", true),
                        showAd = obj.bool("showAd", false),
                        showWatermark = obj.bool("showWatermark", false),
                        watermarkText = obj.string("watermarkText"),
                        showMultiPhones = obj.bool("showMultiPhones", false),
                        showCustomerPhone = obj.bool("showCustomerPhone", false),
                        showContactPhone = obj.bool("showContactPhone", true),
                        favorite = obj.bool("favorite", false),
                        presetKey = obj.string("presetKey").ifBlank { "classic" },
                    ),
                    items = items,
                )
            )
            success++
        }
        return BillImportResult(success, failures, imported)
    }

    private fun JsonObject.string(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

    private fun JsonObject.double(key: String): Double? =
        (this[key] as? JsonPrimitive)?.doubleOrNull

    private fun JsonObject.bool(key: String, default: Boolean): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull ?: default

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.intOrNull
}
