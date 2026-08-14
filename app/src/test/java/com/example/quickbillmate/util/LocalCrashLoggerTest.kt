package com.example.quickbillmate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalCrashLoggerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun formatCrashContainsAllSections() {
        val text = LocalCrashLogger.formatCrash(
            threadName = "main",
            throwable = IllegalStateException("boom"),
            timestamp = "2026-08-14 12:00:00",
            version = "1.0.0 (1)",
            device = "TestMaker TestModel / Android 15 (API 35)",
        )
        assertTrue(text.contains("时间：2026-08-14 12:00:00"))
        assertTrue(text.contains("应用版本：1.0.0 (1)"))
        assertTrue(text.contains("设备：TestMaker TestModel / Android 15 (API 35)"))
        assertTrue(text.contains("线程：main"))
        assertTrue(text.contains("异常：java.lang.IllegalStateException: boom"))
        assertTrue(text.contains("堆栈："))
        assertTrue(text.contains("LocalCrashLoggerTest"))
    }

    @Test
    fun pruneKeepsNewestFilesAndBoundsTotalSize() {
        val dir = tmp.newFolder("crash")
        repeat(12) { i ->
            val file = File(dir, "crash_${i}.txt")
            file.writeText("x".repeat(100 * 1024))
            file.setLastModified(1000L + i)
        }

        LocalCrashLogger.pruneLogs(dir, maxFiles = 10, maxBytes = 1024L * 1024L)

        val remaining = dir.listFiles()!!.toList()
        assertEquals(10, remaining.size)
        assertFalse(File(dir, "crash_0.txt").exists())
        assertFalse(File(dir, "crash_1.txt").exists())
        assertTrue(File(dir, "crash_11.txt").exists())
    }

    @Test
    fun listLogsParsesSummaryAndOrdersNewestFirst() {
        val dir = tmp.newFolder("crash2")
        val older = File(dir, "crash_a.txt")
        older.writeText(
            LocalCrashLogger.formatCrash(
                threadName = "main",
                throwable = RuntimeException("aaa"),
                timestamp = "2026-01-01 00:00:00",
                version = "1.0.0 (1)",
                device = "d",
            )
        )
        val newer = File(dir, "crash_b.txt")
        newer.writeText(
            LocalCrashLogger.formatCrash(
                threadName = "main",
                throwable = IllegalStateException("bbb"),
                timestamp = "2026-01-02 00:00:00",
                version = "1.0.0 (1)",
                device = "d",
            )
        )
        older.setLastModified(1000L)
        newer.setLastModified(2000L)

        val records = LocalCrashLogger.listLogs(dir)

        assertEquals(2, records.size)
        assertEquals("crash_b.txt", records[0].fileName)
        assertEquals("java.lang.IllegalStateException: bbb", records[0].summary)
        assertEquals(2000L, records[0].timeMillis)
    }
}
