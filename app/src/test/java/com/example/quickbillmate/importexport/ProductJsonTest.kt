package com.example.quickbillmate.importexport

import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductJsonTest {

    private val validJson = """
        {
          "version": 1,
          "products": [
            { "name": "腻子粉", "spec": "YGP800 20kg", "unit": "袋", "price": 35, "pack": "20袋/托", "note": "" },
            { "name": "墙衬", "spec": "YGP400 20kg", "price": 28.00 },
            { "name": "蓝和纸墙面保护膜", "spec": "3m*18m", "unit": "卷", "price": 350.00 }
          ]
        }
    """.trimIndent()

    @Test
    fun `合法文件解析成功并规范化价格`() {
        val result = ProductJsonCodec.parse(validJson, emptyList())
        assertEquals(3, result.success)
        assertEquals(0, result.skipped)
        assertEquals(0, result.failures.size)
        assertEquals("35.00", Money.format(result.imported[0].price))
        assertEquals("桶", result.imported[1].unit) // 缺省单位默认“桶”
    }

    @Test
    fun `非法JSON整文件失败`() {
        val ex = assertThrows(ProductJsonException::class.java) {
            ProductJsonCodec.parse("{bad json", emptyList())
        }
        assertEquals("JSON 格式错误", ex.message)
    }

    @Test
    fun `缺少products数组整文件失败`() {
        assertThrows(ProductJsonException::class.java) {
            ProductJsonCodec.parse("""{"version":1}""", emptyList())
        }
    }

    @Test
    fun `负数价格该行失败`() {
        val json = """{"products":[{"name":"甲","price":-5}]}"""
        val result = ProductJsonCodec.parse(json, emptyList())
        assertEquals(0, result.success)
        assertEquals(1, result.failures.size)
        assertEquals(1, result.failures[0].first)
    }

    @Test
    fun `价格缺失该行失败`() {
        val json = """{"products":[{"name":"甲"}]}"""
        val result = ProductJsonCodec.parse(json, emptyList())
        assertEquals(0, result.success)
        assertEquals(1, result.failures.size)
    }

    @Test
    fun `与库内商品按名称加规格去重`() {
        val existing = listOf(Product(name = "腻子粉", spec = "YGP800 20kg", price = 35.0))
        val result = ProductJsonCodec.parse(validJson, existing)
        assertEquals(2, result.success)
        assertEquals(1, result.skipped)
    }

    @Test
    fun `空名称行跳过不计入结果`() {
        val json = """{"products":[{"name":"  ","price":35},{"name":"甲","price":10}]}"""
        val result = ProductJsonCodec.parse(json, emptyList())
        assertEquals(1, result.success)
        assertEquals(0, result.failures.size)
    }

    @Test
    fun `价格字符串或布尔值视为非法`() {
        val json = """{"products":[{"name":"甲","price":"35"},{"name":"乙","price":true}]}"""
        val result = ProductJsonCodec.parse(json, emptyList())
        assertEquals(0, result.success)
        assertEquals(2, result.failures.size)
    }

    @Test
    fun `超过2MB文件拒绝导入`() {
        val big = "{\"products\":[" + "x".repeat(ProductJsonCodec.MAX_SIZE_BYTES + 100) + "]}"
        val ex = assertThrows(ProductJsonException::class.java) {
            ProductJsonCodec.parse(big, emptyList())
        }
        assertTrue(ex.message!!.contains("2MB"))
    }

    @Test
    fun `导出价格保留两位小数且格式可再导入`() {
        val products = listOf(
            Product(name = "腻子粉", spec = "YGP800 20kg", unit = "袋", price = 35.0, pack = "20袋/托", note = ""),
        )
        val text = ProductJsonCodec.export(products)
        assertTrue(text.contains("\"price\": 35.00"))
        val result = ProductJsonCodec.parse(text, emptyList())
        assertEquals(1, result.success)
    }

    @Test
    fun `导出包含type字段且旧格式可解析`() {
        val text = ProductJsonCodec.export(listOf(Product(name = "甲", price = 1.0)))
        assertTrue(text.contains("\"type\": \"products\""))
        val legacy = """{"version":1,"products":[{"name":"乙","price":2}]}"""
        val result = ProductJsonCodec.parse(legacy, emptyList())
        assertEquals(1, result.success)
        assertEquals(DataCategory.detect(legacy), DataCategory.PRODUCTS)
        assertEquals(DataCategory.detect(text), DataCategory.PRODUCTS)
    }
}
