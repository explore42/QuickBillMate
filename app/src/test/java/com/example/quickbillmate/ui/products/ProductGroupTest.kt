package com.example.quickbillmate.ui.products

import com.example.quickbillmate.data.db.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductGroupTest {

    @Test
    fun `favorite product appears only in favorite section`() {
        val fav = Product(id = 1, name = "腻子粉", favorite = true)
        val normal = Product(id = 2, name = "防水涂料", favorite = false)

        val grouped = groupProducts(listOf(fav, normal), listOf("N", "F"))

        assertEquals(listOf("收藏", "F"), grouped.map { it.title })
        assertEquals(listOf(1L), grouped[0].products.map { it.id })
        assertEquals(listOf(2L), grouped[1].products.map { it.id })
    }

    @Test
    fun `letter groups keep order and hash goes last`() {
        val a = Product(id = 1, name = "瓷砖胶")
        val b = Product(id = 2, name = "美缝剂")
        val h = Product(id = 3, name = "123")

        val grouped = groupProducts(listOf(h, a, b), listOf("#", "C", "M"))

        assertEquals(listOf("C", "M", "#"), grouped.map { it.title })
        assertEquals(listOf(1L), grouped[0].products.map { it.id })
        assertEquals(listOf(2L), grouped[1].products.map { it.id })
        assertEquals(listOf(3L), grouped[2].products.map { it.id })
    }

    @Test
    fun `no favorites means no favorite section`() {
        val grouped = groupProducts(listOf(Product(id = 1, name = "石膏线")), listOf("S"))
        assertTrue(grouped.none { it.title == "收藏" })
        assertEquals(listOf("S"), grouped.map { it.title })
    }
}
