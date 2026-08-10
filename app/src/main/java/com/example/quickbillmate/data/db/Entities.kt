package com.example.quickbillmate.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String = "",
    val customerPhone: String = "",
    val companyName: String = "",
    val contactPhone: String = "",
    val salesManager: String = "",
    val docCode: String = "PH",
    val docSerial: String = "000",
    val docDate: String = "",
    val discount: Double = 0.0,
    val remark: String = "",
    val titleSuffix: String = "单据",
    val disclaimer: String = "收到货物当日点清，如有问题请在2日内联系：",
    val showManager: Boolean = true,
    val showRemark: Boolean = true,
    val showWatermark: Boolean = false,
    val showMultiPhones: Boolean = false,
    val favorite: Boolean = false,
    val presetKey: String = "classic",
    val status: String = "草稿",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "bill_items",
    foreignKeys = [
        ForeignKey(
            entity = Bill::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("billId")],
)
data class BillItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long = 0,
    val sortOrder: Int = 0,
    val name: String = "",
    val spec: String = "",
    val unit: String = "桶",
    val qty: Double = 1.0,
    val price: Double = 0.0,
    val pack: String = "",
    val note: String = "",
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val spec: String = "",
    val unit: String = "桶",
    val price: Double = 0.0,
    val pack: String = "",
    val note: String = "",
    val favorite: Boolean = false,
    val pinyinInitial: String = "#",
    val pinyin: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val type: String = "",
    val remark: String = "",
    val favorite: Boolean = false,
    val pinyinInitial: String = "#",
    val pinyin: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "style_presets")
data class StylePreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val paramsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
)
