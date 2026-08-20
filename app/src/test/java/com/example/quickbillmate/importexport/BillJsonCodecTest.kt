package com.example.quickbillmate.importexport

import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillJsonCodecTest {

    @Test
    fun `导出与解析往返一致`() {
        val bill = Bill(
            customerName = "张老板",
            customerPhone = "13800000000",
            companyName = "示例公司",
            docCode = "PH",
            docSerial = "007",
            docDate = "2026-08-20",
            discount = 10.0,
            remark = "含税",
            titleSuffix = "单据",
        )
        val items = listOf(
            BillItem(name = "腻子粉", spec = "20kg", unit = "袋", qty = 2.0, price = 35.0, pack = "20袋/托", note = "", sortOrder = 0),
            BillItem(name = "墙锢", spec = "18kg", unit = "桶", qty = 1.0, price = 65.0, sortOrder = 1),
        )

        val text = BillJsonCodec.export(listOf(BillWithItems(bill, items)))
        assertTrue(text.contains("\"type\":\"bills\""))
        assertTrue(text.contains("\"docSerial\":\"007\""))

        val result = BillJsonCodec.parse(text)
        assertEquals(1, result.success)
        assertEquals(0, result.failures.size)
        val parsed = result.imported[0]
        assertEquals("张老板", parsed.bill.customerName)
        assertEquals("007", parsed.bill.docSerial)
        assertEquals(2, parsed.items.size)
        assertEquals("腻子粉", parsed.items[0].name)
        assertEquals(65.0, parsed.items[1].price, 0.0001)
        // 不含 id/时间戳，导入时新 id 由库生成
        assertEquals(0L, parsed.bill.id)
    }

    @Test
    fun `非法商品行导致该单据失败`() {
        val text = """
            {"version":1,"type":"bills","bills":[
              {"docCode":"PH","items":[{"name":"甲","qty":-1,"price":10}]}
            ]}
        """.trimIndent()
        val result = BillJsonCodec.parse(text)
        assertEquals(0, result.success)
        assertEquals(1, result.failures.size)
    }

    @Test
    fun `缺少bills数组整文件失败`() {
        val ex = org.junit.Assert.assertThrows(DataImportException::class.java) {
            BillJsonCodec.parse("""{"version":1,"type":"bills"}""")
        }
        assertEquals("缺少 bills 数组", ex.message)
    }

    @Test
    fun `DataCategory可识别单据文件`() {
        val text = BillJsonCodec.export(emptyList())
        assertEquals(DataCategory.BILLS, DataCategory.detect(text))
    }
}
