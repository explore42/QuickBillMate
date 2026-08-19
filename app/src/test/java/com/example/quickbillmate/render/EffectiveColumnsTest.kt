package com.example.quickbillmate.render

import org.junit.Assert.assertEquals
import org.junit.Test

class EffectiveColumnsTest {

    @Test
    fun `默认列为八列且备注在最后`() {
        assertEquals(
            listOf("序号", "名称", "规格", "单位", "数量", "单价", "金额", "备注"),
            DEFAULT_COLUMNS.map { it.label },
        )
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 8, 7), DEFAULT_ORDER)
    }

    @Test
    fun `空配置使用默认列`() {
        assertEquals(DEFAULT_COLUMNS, effectiveColumns(StyleParams()))
    }

    @Test
    fun `自定义列序与权重生效`() {
        val params = StyleParams(
            columnOrder = listOf(0, 1, 3, 2, 4, 5, 8, 7),
            columnWeights = listOf(1.0f, 3.0f, 1.0f, 1.5f, 1.3f, 1.6f, 2.2f, 2.4f),
        )
        val columns = effectiveColumns(params)
        assertEquals(listOf(0, 1, 3, 2, 4, 5, 8, 7), columns.map { it.id })
        assertEquals(3.0f, columns[1].defaultWeight)
        assertEquals(2.4f, columns.last().defaultWeight)
    }

    @Test
    fun `旧九列预设自动剔除包装列后仍生效`() {
        // v1 预设：默认列序 + 自定义权重（含已下架的包装列 id=6 权重 9.9f）
        val legacy = StyleParams(
            columnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8),
            columnWeights = listOf(1.0f, 2.7f, 1.8f, 1.0f, 1.3f, 1.6f, 9.9f, 2.1f, 2.5f),
        )
        val columns = effectiveColumns(legacy)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 7, 8), columns.map { it.id })
        // 包装列的 9.9f 被丢弃，备注/金额保留各自权重
        assertEquals(2.1f, columns.first { it.id == 7 }.defaultWeight)
        assertEquals(2.5f, columns.first { it.id == 8 }.defaultWeight)
    }

    @Test
    fun `仅列序无权重的旧配置按默认权重生效`() {
        val legacy = StyleParams(columnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8))
        val columns = effectiveColumns(legacy)
        // 用户显式保存过的列序在剔除包装列后原样保留（v1 默认顺序为备注在金额前）
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 7, 8), columns.map { it.id })
        assertEquals(DEFAULT_WEIGHTS, columns.map { it.defaultWeight })
    }

    @Test
    fun `非法配置回退默认列`() {
        // 列数不足 / 含未知 id / 权重非正
        assertEquals(
            DEFAULT_COLUMNS,
            effectiveColumns(StyleParams(columnOrder = listOf(0, 1, 2), columnWeights = listOf(1f, 1f, 1f))),
        )
        assertEquals(
            DEFAULT_COLUMNS,
            effectiveColumns(StyleParams(columnOrder = listOf(0, 1, 2, 3, 4, 5, 7, 99))),
        )
        assertEquals(
            DEFAULT_ORDER,
            effectiveColumns(
                StyleParams(
                    columnOrder = listOf(0, 1, 2, 3, 4, 5, 8, 7),
                    columnWeights = listOf(1f, 1f, 1f, 1f, 1f, 1f, 0f, 1f),
                ),
            ).map { it.id },
        )
    }

    @Test
    fun `归一化剔除旧预设的下架列并重置孤儿权重`() {
        val legacy = StyleParams(
            columnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8),
            columnWeights = listOf(1.0f, 2.7f, 1.8f, 1.0f, 1.3f, 1.6f, 9.9f, 2.1f, 2.5f),
        )
        val normalized = legacy.dropRetiredColumns()
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 7, 8), normalized.columnOrder)
        assertEquals(listOf(1.0f, 2.7f, 1.8f, 1.0f, 1.3f, 1.6f, 2.1f, 2.5f), normalized.columnWeights)

        // v1 中仅权重无列序的配置从未参与渲染，归一化时直接重置
        val weightsOnly = StyleParams(columnWeights = List(9) { 1.5f })
        assertEquals(emptyList<Int>(), weightsOnly.dropRetiredColumns().columnOrder)
        assertEquals(emptyList<Float>(), weightsOnly.dropRetiredColumns().columnWeights)

        // 新配置（不含下架列）原样返回
        val fresh = StyleParams(columnOrder = listOf(0, 1, 2, 3, 4, 5, 8, 7))
        assertEquals(fresh, fresh.dropRetiredColumns())
    }
}
