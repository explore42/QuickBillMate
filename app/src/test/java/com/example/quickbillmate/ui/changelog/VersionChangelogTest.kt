package com.example.quickbillmate.ui.changelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionChangelogTest {

    @Test
    fun `区间边界等于上次已读不显示且包含当前`() {
        val result = VersionChangelog.entriesFor(lastSeen = 4, current = 5)
        assertEquals(1, result.size)
        assertEquals(5, result[0].versionCode)
        assertTrue(result[0].changes.isNotEmpty())
    }

    @Test
    fun `上次已读等于当前版本时为空`() {
        assertTrue(VersionChangelog.entriesFor(lastSeen = 5, current = 5).isEmpty())
    }

    @Test
    fun `老用户首启展示全部条目`() {
        val result = VersionChangelog.entriesFor(lastSeen = 0, current = 5)
        assertEquals(1, result.size)
        assertEquals(5, result[0].versionCode)
    }

    @Test
    fun `跨多版本按最新在前排序`() {
        val fake = listOf(
            VersionChange(5, "1.2.0", "v5", listOf("a")),
            VersionChange(4, "1.1.2", "v4", listOf("b")),
            VersionChange(6, "1.2.1", "v6", listOf("c")),
        )
        val result = VersionChangelog.entriesFor(lastSeen = 4, current = 6, all = fake)
        assertEquals(listOf(6, 5), result.map { it.versionCode })
    }

    @Test
    fun `区间无条目时给出通用兜底`() {
        val result = VersionChangelog.sectionsFor(
            lastSeen = 6,
            current = 7,
            currentVersionName = "1.2.1",
            all = emptyList(),
        )
        assertEquals(1, result.size)
        assertEquals("已更新至 v1.2.1", result[0].title)
        assertTrue(result[0].changes.isEmpty())
    }

    @Test
    fun `未升级时无任何分段`() {
        assertTrue(
            VersionChangelog.sectionsFor(lastSeen = 5, current = 5, currentVersionName = "1.2.0").isEmpty()
        )
    }
}
