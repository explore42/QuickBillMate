package com.example.quickbillmate.importexport

import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.util.Money
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/** 商品 JSON 导入结果。 */
data class ProductImportResult(
    val success: Int,
    val skipped: Int,
    val failures: List<Pair<Int, String>>,
    val imported: List<Product>,
)

class ProductJsonException(message: String) : Exception(message)

object ProductJsonCodec {
    const val MAX_SIZE_BYTES = 2 * 1024 * 1024

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 解析商品 JSON 文件文本。规则见设计文档第 7 节：
     * - products 缺失或非数组：整文件失败
     * - name 空：跳过不计
     * - price 必须为 JSON number：字符串/布尔/缺失/负数视为该行失败
     * - 按 name + spec 与库内商品去重
     */
    fun parse(text: String, existing: List<Product>): ProductImportResult {
        if (text.toByteArray(Charsets.UTF_8).size > MAX_SIZE_BYTES) {
            throw ProductJsonException("文件超过 2MB，已拒绝导入")
        }
        val root = try {
            json.parseToJsonElement(text)
        } catch (_: Exception) {
            throw ProductJsonException("JSON 格式错误")
        } as? JsonObject ?: throw ProductJsonException("JSON 格式错误")

        val array = root["products"] as? JsonArray
            ?: throw ProductJsonException("缺少 products 数组")

        var success = 0
        var skipped = 0
        val failures = mutableListOf<Pair<Int, String>>()
        val imported = mutableListOf<Product>()
        val seen = existing.map { it.name.trim() to it.spec.trim() }.toMutableSet()

        array.forEachIndexed { index, element ->
            val row = index + 1
            val obj = element as? JsonObject
            if (obj == null) {
                failures.add(row to "不是对象")
                return@forEachIndexed
            }
            val name = obj.stringField("name")
            if (name.isBlank()) return@forEachIndexed // 空行跳过

            val price = obj.priceNumber()
            if (price == null || price < 0) {
                failures.add(row to "价格缺失或非法")
                return@forEachIndexed
            }

            val spec = obj.stringField("spec")
            val key = name to spec
            if (!seen.add(key)) {
                skipped++
                return@forEachIndexed
            }

            val unit = obj.stringField("unit").ifBlank { "桶" }
            imported.add(
                Product(
                    name = name,
                    spec = spec,
                    unit = unit,
                    price = Money.round2(price),
                    pack = obj.stringField("pack"),
                    note = obj.stringField("note"),
                )
            )
            success++
        }
        return ProductImportResult(success, skipped, failures, imported)
    }

    /** 仅接受 JSON number；字符串（含数字字符串）与布尔值视为非法。 */
    private fun JsonObject.priceNumber(): Double? {
        val primitive = this["price"] as? JsonPrimitive ?: return null
        if (primitive.toString().startsWith("\"")) return null
        return primitive.doubleOrNull
    }

    /** 导出 JSON 文本，价格统一保留两位小数。 */
    fun export(products: List<Product>): String {
        val sb = StringBuilder()
        sb.append("{\n  \"version\": 1,\n  \"products\": [\n")
        products.forEachIndexed { index, p ->
            sb.append("    {")
            sb.append("\"name\": ").append(quote(p.name))
            sb.append(", \"spec\": ").append(quote(p.spec))
            sb.append(", \"unit\": ").append(quote(p.unit))
            sb.append(", \"price\": ").append(Money.format(p.price))
            sb.append(", \"pack\": ").append(quote(p.pack))
            sb.append(", \"note\": ").append(quote(p.note))
            sb.append("}")
            if (index != products.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n}\n")
        return sb.toString()
    }

    private fun JsonObject.stringField(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
        append('"')
    }

    /** 模板示例文本（与 docs/products_template.json 一致）。 */
    fun templateText(): String = """{
  "version": 1,
  "products": [
    { "name": "腻子粉", "spec": "YGP800 20kg", "unit": "袋", "price": 35.00, "pack": "20袋/托", "note": "" },
    { "name": "墙衬", "spec": "YGP400 20kg", "unit": "袋", "price": 28.00, "pack": "20袋/托", "note": "" },
    { "name": "底层腻子", "spec": "YN600 20kg", "unit": "袋", "price": 25.00, "pack": "20袋/托", "note": "" },
    { "name": "嵌缝石膏", "spec": "QN600 2.5kg", "unit": "袋", "price": 18.00, "pack": "24袋/箱", "note": "" },
    { "name": "墙锢", "spec": "QG500 18kg", "unit": "桶", "price": 65.00, "pack": "4桶/箱", "note": "" },
    { "name": "墙尼", "spec": "QN500 20kg", "unit": "袋", "price": 22.00, "pack": "20袋/托", "note": "" },
    { "name": "界面剂", "spec": "通用型 18kg", "unit": "桶", "price": 78.00, "pack": "4桶/箱", "note": "" },
    { "name": "网格布", "spec": "80g 1m×50m", "unit": "卷", "price": 42.00, "pack": "10卷/包", "note": "" },
    { "name": "阴阳角条", "spec": "PVC 2.5m", "unit": "根", "price": 1.50, "pack": "100根/包", "note": "" },
    { "name": "蓝和纸墙面保护膜", "spec": "3m*18m", "unit": "卷", "price": 350.00, "pack": "50卷/件", "note": "" }
  ]
}"""
}
