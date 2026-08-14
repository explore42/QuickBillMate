package com.example.quickbillmate

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.quickbillmate.data.db.AppDatabase
import com.example.quickbillmate.data.db.Migrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room 迁移测试骨架：按导出的 v1 schema 建库并写入数据，
 * 迁移到最新版本后校验 schema 与数据完整。
 * 未来新增 MIGRATION_1_2 等时，把 `runMigrationsAndValidate` 的目标版本
 * 改为最新版本并补充对应迁移数据断言。
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun openV1SchemaAndKeepData() {
        // 按导出的 v1 schema 建库并插入一条商品数据
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO products " +
                    "(name, spec, unit, price, pack, note, favorite, pinyinInitial, pinyin, createdAt) " +
                    "VALUES ('迁移测试商品', 'spec', '桶', 35.0, '', '', 0, '#', '', 1)"
            )
            close()
        }

        // 迁移到最新版本（当前为 1，无迁移）并校验 schema 与数据
        val db = helper.runMigrationsAndValidate(testDb, 1, true, *Migrations.ALL)
        val cursor = db.query("SELECT name, price FROM products WHERE name = ?", arrayOf("迁移测试商品"))
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("迁移测试商品", it.getString(0))
            assertEquals(35.0, it.getDouble(1), 0.001)
        }
        db.close()
    }
}
