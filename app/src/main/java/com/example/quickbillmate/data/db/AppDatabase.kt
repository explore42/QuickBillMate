package com.example.quickbillmate.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Bill::class, BillItem::class, Product::class, Customer::class, StylePreset::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun billItemDao(): BillItemDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun stylePresetDao(): StylePresetDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quickbillmate.db",
                )
                    // 数据库升级规范：
                    // - v1 为首个正式版基线（当前未发布），schema JSON 提交于 app/schemas/
                    // - 发布后任何结构变更必须新增 Migration(n, n+1) 并在 Migrations.ALL 注册，
                    //   同时递增 @Database version，禁止直接改实体不升版本
                    // - 不启用 fallbackToDestructiveMigration，避免发布后静默丢数据
                    .addMigrations(*Migrations.ALL)
                    .build()
                    .also { instance = it }
            }
    }
}
