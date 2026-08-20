package com.example.quickbillmate.importexport

import com.example.quickbillmate.data.db.Customer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerJsonCodecTest {

    @Test
    fun `export and parse round trip`() {
        val customers = listOf(
            Customer(name = "张三", phone = "13800000000", type = "装修队", remark = "备注", favorite = true),
            Customer(name = "李四", phone = "13900000000"),
        )

        val text = CustomerJsonCodec.export(customers)
        assertTrue(text.contains("\"type\":\"customers\""))
        val parsed = CustomerJsonCodec.parse(text)

        assertEquals(2, parsed.size)
        assertEquals("张三", parsed[0].name)
        assertEquals("13800000000", parsed[0].phone)
        assertEquals("装修队", parsed[0].type)
        assertEquals("备注", parsed[0].remark)
        assertEquals(true, parsed[0].favorite)
        assertEquals("李四", parsed[1].name)
    }

    @Test
    fun `旧格式无type字段仍可解析`() {
        val text = """{"version":1,"customers":[{"name":"王五","phone":"13700000000"}]}"""
        val parsed = CustomerJsonCodec.parse(text)
        assertEquals(1, parsed.size)
        assertEquals("王五", parsed[0].name)
    }

    @Test
    fun `合并电话规范化去重追加`() {
        assertEquals("13800000000,13711112222", CustomerImportMerger.mergePhones("13800000000", listOf("137 1111 2222")))
        assertEquals("13800000000", CustomerImportMerger.mergePhones("13800000000", listOf("13800000000")))
        assertEquals("", CustomerImportMerger.mergePhones("", listOf("", "abc")))
    }
}
