package com.example.quickbillmate

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.quickbillmate.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseConfigTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun databaseUsesTruncateJournalMode() {
        val db = AppDatabase.get(context)
        db.openHelper.writableDatabase.query("PRAGMA journal_mode").use { cursor ->
            assertTrue(cursor.moveToFirst())
            // TRUNCATE 模式保证主库文件始终包含最新数据，备份/恢复一致
            assertEquals("truncate", cursor.getString(0))
        }
    }
}
