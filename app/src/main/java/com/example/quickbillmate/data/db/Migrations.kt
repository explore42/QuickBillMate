package com.example.quickbillmate.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 数据库迁移注册表。
 *
 * 规范：发布后每次结构变更（新增表/列、修改约束等）必须：
 * 1. 在此新增 `Migration(旧版本, 新版本)`，`@Database version` 同步 +1；
 * 2. 在 [ALL] 中注册；
 * 3. 新增列须在迁移 SQL 中提供默认值；
 * 4. 在 androidTest 的 MigrationTest 中补充“旧版本数据 → 迁移 → 数据完整”用例。
 */
object Migrations {
    /** v2：单据客户电话改为三态展示（不显示/一个/多个），存量单据统一置为不显示。 */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE bills ADD COLUMN showCustomerPhone INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
    )
}
