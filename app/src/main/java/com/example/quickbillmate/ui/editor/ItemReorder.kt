package com.example.quickbillmate.ui.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue

/** 商品行槽位：纵向 top / height，坐标空间为商品列表容器。 */
data class ItemSlot(val top: Float, val height: Float) {
    val center: Float get() = top + height / 2f
}

/**
 * 拖拽排序的纯计算部分（无 Compose 依赖，便于单元测试）：
 * 依据被拖卡片当前中心的纵坐标与全部槽位，求出它应该落在哪个槽位。
 */
object ItemReorder {
    /**
     * @param centerY 被拖卡片当前中心的纵坐标（与 [slots] 同一坐标空间）
     * @param slots 各槽位，下标即行下标
     * @return 目标行下标（越界自动收敛到首/末行）
     */
    fun targetIndex(centerY: Float, slots: List<ItemSlot>): Int {
        if (slots.isEmpty()) return 0
        var target = 0
        for (index in slots.indices) {
            if (centerY >= slots[index].top) target = index
        }
        return target.coerceIn(slots.indices)
    }
}

/**
 * 编辑页商品行拖拽状态：卡片上报槽位、把手驱动拖拽，排序结果回调给 ViewModel。
 *
 * 位移自校正：被拖卡片的目标中心 = 起始中心 + 累计位移（手指位移 + 自动滚动位移），
 * translationY = 目标中心 − 当前槽位中心，因此重排后卡片仍严格跟手。
 */
@Stable
class ItemReorderState(
    private val onMove: (from: Int, to: Int) -> Unit,
) {
    private val slots = mutableStateMapOf<Int, ItemSlot>()

    var draggingIndex by mutableIntStateOf(-1)
        private set

    var translation by mutableFloatStateOf(0f)
        private set

    /** 商品列表容器在窗口中的纵坐标（随外层滚动变化）。 */
    var containerTop by mutableFloatStateOf(0f)

    /** 可视区上下边界（窗口中坐标），用于边缘自动滚动。 */
    var viewportTop by mutableFloatStateOf(0f)
    var viewportBottom by mutableFloatStateOf(0f)

    private var startCenter = 0f
    private var accumulated = 0f

    fun onSlot(index: Int, top: Float, height: Float) {
        slots[index] = ItemSlot(top, height)
    }

    /** 行数变化后槽位失效，重新上报前先清空（拖拽中不支持增删行）。 */
    fun clearSlots() {
        slots.clear()
    }

    fun isDragging(index: Int): Boolean = draggingIndex == index

    fun start(index: Int) {
        val slot = slots[index] ?: return
        draggingIndex = index
        startCenter = slot.center
        accumulated = 0f
        translation = 0f
    }

    fun drag(deltaY: Float) {
        if (draggingIndex < 0) return
        accumulated += deltaY
        sync()
    }

    /** 外层自动滚动后同步位移：滚动量与手指位移对“跟手”等效。 */
    fun onViewportScroll(delta: Float) {
        if (draggingIndex < 0) return
        accumulated += delta
        sync()
    }

    fun end() {
        draggingIndex = -1
        translation = 0f
    }

    /**
     * 每帧自动滚动位移（px）：仅当被拖卡片贴近可视区上下边缘时返回非 0，
     * 越靠边越快，最多 [maxPerFrame]（正数向下滚、负数向上滚）。
     */
    fun autoScrollDelta(edge: Float, maxPerFrame: Float): Float {
        val index = draggingIndex
        if (index < 0 || edge <= 0f) return 0f
        val slot = slots[index] ?: return 0f
        val centerWindow = containerTop + slot.center + translation
        val upBound = viewportTop + edge
        val downBound = viewportBottom - edge
        return when {
            centerWindow < upBound -> -maxPerFrame * ratio(upBound - centerWindow, edge)
            centerWindow > downBound -> maxPerFrame * ratio(centerWindow - downBound, edge)
            else -> 0f
        }
    }

    private fun ratio(distance: Float, edge: Float): Float =
        (distance / edge).coerceIn(0f, 1f)

    private fun sync() {
        val index = draggingIndex
        if (index < 0) return
        val slot = slots[index] ?: return
        val center = startCenter + accumulated
        translation = center - slot.center
        val target = ItemReorder.targetIndex(center, slotList())
        if (target == index || slots[target] == null) return
        onMove(index, target)
        draggingIndex = target
        translation = center - (slots[target]?.center ?: slot.center)
    }

    private fun slotList(): List<ItemSlot> {
        val count = slots.size
        if (count == 0) return emptyList()
        return (0 until count).map { slots[it] ?: ItemSlot(0f, 0f) }
    }
}
