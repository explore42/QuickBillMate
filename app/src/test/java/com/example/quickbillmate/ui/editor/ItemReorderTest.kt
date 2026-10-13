package com.example.quickbillmate.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemReorderTest {

    /** 3 行等高卡片：每行 100px，间距 0。 */
    private val slots = List(3) { ItemSlot(top = it * 100f, height = 100f) }

    @Test
    fun `原地不动时目标仍在原槽位`() {
        assertEquals(0, ItemReorder.targetIndex(50f, slots))
        assertEquals(1, ItemReorder.targetIndex(150f, slots))
        assertEquals(2, ItemReorder.targetIndex(250f, slots))
    }

    @Test
    fun `相邻交换`() {
        // 第 0 行中心下移到第 1 行范围内
        assertEquals(1, ItemReorder.targetIndex(120f, slots))
        // 第 2 行中心上移到第 1 行范围内
        assertEquals(1, ItemReorder.targetIndex(180f, slots))
    }

    @Test
    fun `跨多行`() {
        assertEquals(2, ItemReorder.targetIndex(340f, slots))
    }

    @Test
    fun `越过首尾边界收敛到首末行`() {
        assertEquals(0, ItemReorder.targetIndex(-500f, slots))
        assertEquals(2, ItemReorder.targetIndex(9999f, slots))
    }

    @Test
    fun `空槽位返回首行`() {
        assertEquals(0, ItemReorder.targetIndex(120f, emptyList()))
    }
}
