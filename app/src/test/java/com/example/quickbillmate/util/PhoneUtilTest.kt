package com.example.quickbillmate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneUtilTest {

    @Test
    fun `normalize strips separators and prefixes`() {
        assertEquals("13800000000", PhoneUtil.normalizePhone("138-0000-0000"))
        assertEquals("13800000000", PhoneUtil.normalizePhone("138 0000 0000"))
        assertEquals("13800000000", PhoneUtil.normalizePhone("+86 138-0000-0000"))
        assertEquals("13800000000", PhoneUtil.normalizePhone("0086 13800000000"))
        assertEquals("+442079460000", PhoneUtil.normalizePhone("0044 20 7946 0000"))
        assertEquals("+14155551234", PhoneUtil.normalizePhone("+1 (415) 555-1234"))
        assertEquals("", PhoneUtil.normalizePhone(""))
        assertEquals("", PhoneUtil.normalizePhone("---"))
    }

    @Test
    fun `validity boundaries`() {
        assertFalse(PhoneUtil.isValidPhone(""))
        assertFalse(PhoneUtil.isValidPhone("123456"))
        assertTrue(PhoneUtil.isValidPhone("1234567"))
        assertTrue(PhoneUtil.isValidPhone("13800000000"))
        assertTrue(PhoneUtil.isValidPhone("123456789012"))
        assertFalse(PhoneUtil.isValidPhone("1234567890123"))
        assertFalse(PhoneUtil.isValidPhone("+123456"))
        assertTrue(PhoneUtil.isValidPhone("+1234567"))
        assertTrue(PhoneUtil.isValidPhone("+123456789012345"))
        assertFalse(PhoneUtil.isValidPhone("+1234567890123456"))
    }

    @Test
    fun `display phones hidden first or all`() {
        assertEquals("13800000000", PhoneUtil.displayPhones("13800000000,13900000000", show = true, showMulti = false))
        assertEquals("13800000000,13900000000", PhoneUtil.displayPhones("13800000000,13900000000", show = true, showMulti = true))
        assertEquals("", PhoneUtil.displayPhones("", show = true, showMulti = false))
        // 不显示客户电话时无论内容与多电话开关，一律返回空
        assertEquals("", PhoneUtil.displayPhones("13800000000,13900000000", show = false, showMulti = false))
        assertEquals("", PhoneUtil.displayPhones("13800000000,13900000000", show = false, showMulti = true))
    }

    @Test
    fun `split phones normalizes`() {
        assertEquals(listOf("13800000000", "13900000000"), PhoneUtil.splitPhones("138-0000-0000, 13900000000"))
    }
}
