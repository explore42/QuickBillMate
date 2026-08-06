package com.example.quickbillmate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillNumberTest {

    @Test
    fun `单据编号拼接`() {
        assertEquals("XS20251121482", BillNumber.build("XS", "2025-11-21", "482"))
    }

    @Test
    fun `随机流水号保留前导零且为三位数字`() {
        repeat(500) {
            val serial = BillNumber.randomSerial()
            assertEquals(3, serial.length)
            assertTrue(serial.all { it.isDigit() })
            assertTrue(serial.toInt() in 0..999)
        }
    }

    @Test
    fun `随机流水号可生成前导零`() {
        // 000-099 出现的概率约为 10%，500 次抽样几乎必然至少命中一次
        val hasLeadingZero = (1..500).any { BillNumber.randomSerial().startsWith("0") }
        assertTrue(hasLeadingZero)
    }
}
