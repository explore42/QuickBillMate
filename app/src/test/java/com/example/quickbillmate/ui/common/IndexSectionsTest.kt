package com.example.quickbillmate.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexSectionsTest {

    @Test
    fun `month key keeps year-month and falls back for blank`() {
        assertEquals("2025-11", monthKey("2025-11-21"))
        assertEquals("其他", monthKey(""))
    }

    @Test
    fun `month bubble formats year and month`() {
        assertEquals("2025年11月", monthBubble("2025-11"))
        assertEquals("2025年1月", monthBubble("2025-01"))
        assertEquals("其他", monthBubble("其他"))
    }

    @Test
    fun `month sections preserve list order and dedupe`() {
        val result = monthSections(listOf("2025-11", "2025-10", "2025-11", "2025-12"))
        assertEquals(
            listOf("2025-11", "2025-10", "2025-12"),
            result.map { it.label },
        )
        assertEquals("2025年11月", result.first().bubble)
    }

    @Test
    fun `letter sections sort A to Z and put hash last`() {
        val result = letterSections(listOf("B", "#", "A", "C", "B"))
        assertEquals(listOf("A", "B", "C", "#"), result.map { it.label })
    }

    @Test
    fun `star section only appears when favorites exist`() {
        assertEquals(listOf("A"), letterSections(listOf("A")).map { it.label })
        assertEquals(listOf("A"), letterSections(listOf("A"), hasFavorites = false).map { it.label })
        val withStar = letterSections(listOf("A", "B", "#"), hasFavorites = true)
        assertEquals(listOf("♥", "A", "B", "#"), withStar.map { it.label })
        assertEquals("收藏", withStar.first().bubble)
    }

    @Test
    fun `empty letters produce empty sections`() {
        assertTrue(letterSections(emptyList()).isEmpty())
        assertTrue(monthSections(emptyList()).isEmpty())
    }
}
