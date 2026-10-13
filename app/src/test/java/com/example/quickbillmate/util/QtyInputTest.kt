package com.example.quickbillmate.util

import org.junit.Assert.assertEquals
import org.junit.Test

class QtyInputTest {

    @Test
    fun `负数只允许前导负号`() {
        assertEquals("-2", QtyInput.sanitize("-2"))
        assertEquals("-12", QtyInput.sanitize("-12"))
        assertEquals("12", QtyInput.sanitize("12-"))
        assertEquals("-1", QtyInput.sanitize("--1"))
        assertEquals("-12", QtyInput.sanitize("-1-2"))
    }

    @Test
    fun `单独负号与空串作为中间态保留`() {
        assertEquals("-", QtyInput.sanitize("-"))
        assertEquals("", QtyInput.sanitize(""))
        assertEquals("", QtyInput.sanitize("abc"))
        assertEquals("-", QtyInput.sanitize("-abc"))
    }

    @Test
    fun `数字位数上限四位`() {
        assertEquals("9999", QtyInput.sanitize("99999"))
        assertEquals("1234", QtyInput.sanitize("12345"))
    }

    @Test
    fun `超出上下限时收敛到边界`() {
        assertEquals("-999", QtyInput.sanitize("-9999"))
        assertEquals("9999", QtyInput.sanitize("99999"))
        assertEquals("-999", QtyInput.sanitize("-12345"))
    }

    @Test
    fun `按上下限收敛并归一化负零`() {
        assertEquals(-999.0, QtyInput.clamp(-9999.0), 1e-9)
        assertEquals(9999.0, QtyInput.clamp(99999.0), 1e-9)
        assertEquals(0.0, QtyInput.clamp(-0.0), 1e-9)
        assertEquals(-2.0, QtyInput.clamp(-2.0), 1e-9)
    }
}
