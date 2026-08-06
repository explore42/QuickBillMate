package com.example.quickbillmate.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY updatedAt DESC LIMIT 20")
    fun observeRecent(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :id")
    fun observeBill(id: Long): Flow<Bill?>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBill(id: Long): Bill?

    @Query(
        "SELECT COUNT(*) FROM bills WHERE docCode = :code AND docDate = :date " +
            "AND docSerial = :serial AND id != :excludeId"
    )
    suspend fun countSerialConflict(code: String, date: String, serial: String, excludeId: Long): Int

    @Insert
    suspend fun insert(bill: Bill): Long

    @Update
    suspend fun update(bill: Bill)

    @Delete
    suspend fun delete(bill: Bill)
}

@Dao
interface BillItemDao {
    @Query("SELECT * FROM bill_items WHERE billId = :billId ORDER BY sortOrder ASC")
    fun observeItems(billId: Long): Flow<List<BillItem>>

    @Query("SELECT * FROM bill_items WHERE billId = :billId ORDER BY sortOrder ASC")
    suspend fun getItems(billId: Long): List<BillItem>

    @Insert
    suspend fun insertAll(items: List<BillItem>)

    @Update
    suspend fun updateAll(items: List<BillItem>)

    @Query("DELETE FROM bill_items WHERE billId = :billId")
    suspend fun deleteByBill(billId: Long)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY createdAt DESC, id DESC")
    suspend fun getAll(): List<Product>

    @Query(
        "SELECT * FROM products WHERE name LIKE '%' || :q || '%' OR spec LIKE '%' || :q || '%' " +
            "ORDER BY createdAt DESC, id DESC"
    )
    fun observeSearch(q: String): Flow<List<Product>>

    @Insert
    suspend fun insert(product: Product): Long

    @Insert
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY fromContacts DESC, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY createdAt DESC, id DESC")
    suspend fun getAll(): List<Customer>

    @Query(
        "SELECT * FROM customers WHERE name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%' " +
            "OR type LIKE '%' || :q || '%' ORDER BY fromContacts DESC, createdAt DESC, id DESC"
    )
    fun observeSearch(q: String): Flow<List<Customer>>

    @Query("SELECT COUNT(*) FROM customers WHERE name = :name AND phone = :phone")
    suspend fun countDuplicate(name: String, phone: String): Int

    @Insert
    suspend fun insert(customer: Customer): Long

    @Insert
    suspend fun insertAll(customers: List<Customer>)

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)
}

@Dao
interface StylePresetDao {
    @Query("SELECT * FROM style_presets ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<StylePreset>>

    @Query("SELECT * FROM style_presets ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(): List<StylePreset>

    @Query("SELECT * FROM style_presets WHERE id = :id")
    suspend fun getById(id: Long): StylePreset?

    @Insert
    suspend fun insert(preset: StylePreset): Long

    @Update
    suspend fun update(preset: StylePreset)

    @Delete
    suspend fun delete(preset: StylePreset)
}
