package com.example.quickbillmate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinyinSortTest {

    @Test
    fun `hash sorts after all letters`() {
        assertTrue(Pinyin.letterSortKey("#") > Pinyin.letterSortKey("Z"))
        assertEquals("A", Pinyin.letterSortKey("A"))
    }

    @Test
    fun `sorts favorites first then by pinyin initial`() {
        data class Item(val name: String, val fav: Boolean)
        val items = listOf(
            Item("张伟", false), // Z
            Item("李娜", true), // L (favorite)
            Item("陈静", false), // C
            Item("马超", false), // M
            Item("123", false), // #
        )
        val letters = mapOf(
            "张伟" to "Z",
            "李娜" to "L",
            "陈静" to "C",
            "马超" to "M",
            "123" to "#",
        )

        val sorted = Pinyin.sortByPinyinLetter(items, { it.fav }, { letters.getValue(it.name) })

        assertEquals(listOf("李娜", "陈静", "马超", "张伟", "123"), sorted.map { it.name })
    }

    @Test
    fun `stable within same letter`() {
        data class Item(val name: String)
        val items = listOf(Item("刘一"), Item("刘二"), Item("刘三"))

        val sorted = Pinyin.sortByPinyinLetter(items, { false }, { "L" })

        assertEquals(listOf("刘一", "刘二", "刘三"), sorted.map { it.name })
    }
}
