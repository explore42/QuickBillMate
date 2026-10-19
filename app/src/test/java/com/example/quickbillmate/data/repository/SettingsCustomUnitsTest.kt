package com.example.quickbillmate.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 预置单位：内置「袋」替代旧「代」，自定义单位与内置合并时去重。
 */
class SettingsCustomUnitsTest {

    @Test
    fun builtinUnitsUseBagInsteadOfDai() {
        assertEquals(listOf("桶", "筒", "袋", "张", "卷", "支", "把"), SettingsStore.BUILTIN_UNITS)
        assertFalse(SettingsStore.BUILTIN_UNITS.contains("代"))
    }

    @Test
    fun customBagMergesIntoBuiltin() {
        // 存量设置/导入文件里的自定义「袋」不再单列，按原顺序保留其它自定义项
        assertEquals(
            listOf("箱", "包"),
            SettingsStore.normalizeCustomUnits(listOf("箱", "袋", "筒", "包")),
        )
    }

    @Test
    fun trimsWhitespaceAndDropsBlankEntries() {
        assertEquals(
            listOf("箱", "包"),
            SettingsStore.normalizeCustomUnits(listOf("  箱 ", "", "   ", "包", "桶")),
        )
    }

    @Test
    fun deduplicatesKeepingFirstOccurrence() {
        assertEquals(
            listOf("箱", "包"),
            SettingsStore.normalizeCustomUnits(listOf("箱", "包", "箱", "包")),
        )
    }

    @Test
    fun emptyInputGivesEmptyList() {
        assertTrue(SettingsStore.normalizeCustomUnits(emptyList()).isEmpty())
        assertTrue(SettingsStore.normalizeCustomUnits(listOf("", "  ", "袋")).isEmpty())
    }
}
