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
    fun `display phones first or all`() {
        assertEquals("13800000000", PhoneUtil.displayPhones("13800000000,13900000000", showMulti = false))
        assertEquals("13800000000,13900000000", PhoneUtil.displayPhones("13800000000,13900000000", showMulti = true))
        assertEquals("", PhoneUtil.displayPhones("", showMulti = false))
    }

    @Test
    fun `split phones normalizes`() {
        assertEquals(listOf("13800000000", "13900000000"), PhoneUtil.splitPhones("138-0000-0000, 13900000000"))
    }
}
