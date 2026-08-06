package com.example.quickbillmate.ui.customers

import com.example.quickbillmate.data.db.Customer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerGroupTest {

    @Test
    fun `favorite customer appears only in favorite section`() {
        val fav = Customer(id = 1, name = "李娜", favorite = true)
        val normal = Customer(id = 2, name = "陈静", favorite = false)

        val grouped = groupCustomers(listOf(fav, normal), listOf("L", "C"))

        assertEquals(listOf("收藏", "C"), grouped.map { it.title })
        assertEquals(listOf(1L), grouped[0].customers.map { it.id })
        assertEquals(listOf(2L), grouped[1].customers.map { it.id })
    }

    @Test
    fun `letter groups keep order and hash goes last`() {
        val a = Customer(id = 1, name = "高翔")
        val b = Customer(id = 2, name = "郭涛")
        val h = Customer(id = 3, name = "123")

        val grouped = groupCustomers(listOf(h, a, b), listOf("#", "G", "G"))

        assertEquals(listOf("G", "#"), grouped.map { it.title })
        assertEquals(listOf(1L, 2L), grouped[0].customers.map { it.id })
        assertEquals(listOf(3L), grouped[1].customers.map { it.id })
    }

    @Test
    fun `no favorites means no favorite section`() {
        val grouped = groupCustomers(listOf(Customer(id = 1, name = "陈静")), listOf("C"))
        assertTrue(grouped.none { it.title == "收藏" })
        assertEquals(listOf("C"), grouped.map { it.title })
    }
}
