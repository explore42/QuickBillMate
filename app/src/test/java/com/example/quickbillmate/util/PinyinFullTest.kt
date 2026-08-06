package com.example.quickbillmate.util

import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.data.repository.withPinyin
import org.junit.Assert.assertEquals
import org.junit.Test

class PinyinFullTest {

    @Test
    fun `full pinyin lowercases ascii and keeps digits`() {
        assertEquals("apple", Pinyin.fullPinyin("Apple"))
        assertEquals("abc123", Pinyin.fullPinyin("ABC123"))
        assertEquals("", Pinyin.fullPinyin(""))
        assertEquals("", Pinyin.fullPinyin("  "))
    }

    @Test
    fun `withPinyin fills fields for ascii names`() {
        val customer = Customer(name = "Alice").withPinyin()
        assertEquals("A", customer.pinyinInitial)
        assertEquals("alice", customer.pinyin)

        val product = Product(name = "Water").withPinyin()
        assertEquals("W", product.pinyinInitial)
        assertEquals("water", product.pinyin)
    }
}
