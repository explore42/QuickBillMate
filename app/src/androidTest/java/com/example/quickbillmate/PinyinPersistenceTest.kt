package com.example.quickbillmate

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.quickbillmate.data.db.AppDatabase
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.data.repository.QrImageStore
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.importexport.ContactsImporter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PinyinPersistenceTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: AppRepository
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = AppRepository(db, SettingsStore(context), QrImageStore(context))
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun customerPinyinComputedOnInsertAndKeptOnPhoneChange() = runBlocking {
        repo.saveCustomer(Customer(name = "李娜", phone = "13800000000"))
        var customer = repo.getCustomers().single()
        assertEquals("L", customer.pinyinInitial)
        assertTrue(customer.pinyin.isNotBlank())

        // 只改电话：拼音保持不变
        repo.saveCustomer(customer.copy(phone = "13800000001"))
        customer = repo.getCustomers().single()
        assertEquals("L", customer.pinyinInitial)
        assertEquals("13800000001", customer.phone)
    }

    @Test
    fun customerPinyinRecomputedOnRename() = runBlocking {
        repo.saveCustomer(Customer(name = "李娜"))
        repo.saveCustomer(repo.getCustomers().single().copy(name = "陈静"))
        val customer = repo.getCustomers().single()
        assertEquals("C", customer.pinyinInitial)
    }

    @Test
    fun productPinyinComputedOnInsertAndKeptOnPriceChange() = runBlocking {
        repo.saveProduct(Product(name = "腻子粉", price = 35.0))
        var product = repo.getProducts().single()
        assertEquals("N", product.pinyinInitial)
        assertTrue(product.pinyin.isNotBlank())

        repo.saveProduct(product.copy(price = 40.0))
        product = repo.getProducts().single()
        assertEquals("N", product.pinyinInitial)
    }

    @Test
    fun contactsImportComputesPinyin() = runBlocking {
        repo.importContactCandidates(listOf(ContactsImporter.Candidate("王强", "13800000001")))
        val customer = repo.getCustomers().single()
        assertEquals("W", customer.pinyinInitial)
    }

    @Test
    fun productJsonImportComputesPinyin() {
        runBlocking {
            val file = File(context.cacheDir, "products_test.json")
            file.writeText(
                """{"version":1,"products":[{"name":"腻子粉","spec":"20kg","unit":"袋","price":35.0}]}"""
            )
            val result = repo.importProductsFromUri(context, Uri.fromFile(file))
            assertEquals(1, result.success)
            val product = repo.getProducts().single()
            assertEquals("N", product.pinyinInitial)
            assertTrue(product.pinyin.isNotBlank())
            file.delete()
        }
    }
}
