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
 * Room 迁移测试：按导出的 v1 schema 建库并写入数据，
 * 迁移到最新版本后校验 schema 与数据完整。
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

        // 迁移到最新版本并校验 schema 与数据
        val db = helper.runMigrationsAndValidate(testDb, 2, true, *Migrations.ALL)
        val cursor = db.query("SELECT name, price FROM products WHERE name = ?", arrayOf("迁移测试商品"))
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("迁移测试商品", it.getString(0))
            assertEquals(35.0, it.getDouble(1), 0.001)
        }
        db.close()
    }

    @Test
    fun migrateV1ToV2BackfillsShowCustomerPhoneHidden() {
        // v1 单据没有 showCustomerPhone 列；迁移后应统一回填为 0（不显示），其余数据保持完整
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO bills " +
                    "(customerName, customerPhone, companyName, contactPhone, salesManager, docCode, docSerial, " +
                    "docDate, discount, remark, titleSuffix, adText, showManager, showRemark, showAd, " +
                    "showWatermark, watermarkText, showMultiPhones, showContactPhone, favorite, presetKey, " +
                    "createdAt, updatedAt) " +
                    "VALUES ('张三', '13800000000,13900000000', '', '', '', 'PH', '001', '2026-01-01', 0.0, '', " +
                    "'单据', '', 1, 1, 0, 0, '', 1, 1, 0, 'classic', 1, 1)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 2, true, *Migrations.ALL)
        val cursor = db.query(
            "SELECT customerPhone, showMultiPhones, showCustomerPhone FROM bills WHERE customerName = ?",
            arrayOf("张三"),
        )
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("13800000000,13900000000", it.getString(0))
            assertEquals(1, it.getInt(1))
            assertEquals(0, it.getInt(2))
        }
        db.close()
    }
}
