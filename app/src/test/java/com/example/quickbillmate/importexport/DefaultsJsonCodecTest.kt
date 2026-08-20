package com.example.quickbillmate.importexport

import com.example.quickbillmate.data.repository.DefaultInfoValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultsJsonCodecTest {

    @Test
    fun `导出与解析往返一致且不含二维码`() {
        val values = DefaultInfoValues(
            titleSuffix = "送货单",
            docCode = "PH",
            companyName = "示例公司",
            manager = "王经理",
            contactPhone = "13800000000",
            remark = "备注",
            adText = "广告",
            watermarkText = "水印",
            showWatermark = true,
        )
        val text = DefaultsJsonCodec.export(values, listOf("桶", "袋"))
        assertTrue(text.contains("\"type\":\"defaults\""))
        assertFalse(text.contains("qr"))

        val parsed = DefaultsJsonCodec.parse(text)
        assertEquals("送货单", parsed.values.titleSuffix)
        assertEquals("示例公司", parsed.values.companyName)
        assertEquals(listOf("桶", "袋"), parsed.customUnits)
        assertEquals(DataCategory.DEFAULTS, DataCategory.detect(text))
    }

    @Test
    fun `按分组导出部分字段`() {
        val values = DefaultInfoValues(titleSuffix = "单据", companyName = "公司A")
        val text = DefaultsJsonCodec.export(values, emptyList(), setOf(DefaultsGroup.TITLE))
        assertTrue(text.contains("\"titleSuffix\""))
        assertFalse(text.contains("companyName"))
        assertFalse(text.contains("customUnits"))
    }
}
