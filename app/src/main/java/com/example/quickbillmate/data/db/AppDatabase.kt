package com.example.quickbillmate.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Bill::class, BillItem::class, Product::class, Customer::class, StylePreset::class],
    version = 1,
    exportSchema = false,
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
                    // 尚未发布正式版：结构变更直接重建开发库，不使用迁移脚本
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
