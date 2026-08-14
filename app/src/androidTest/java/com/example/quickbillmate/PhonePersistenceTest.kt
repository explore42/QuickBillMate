package com.example.quickbillmate

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.quickbillmate.data.db.AppDatabase
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.data.repository.QrImageStore
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.importexport.ContactsImporter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhonePersistenceTest {

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
    fun importContactCandidatesStoresNormalizedPhone() = runBlocking {
        repo.importContactCandidates(listOf(ContactsImporter.Candidate("张三", "+86 138-0000-0000")))
        val customer = repo.getCustomers().single()
        assertEquals("13800000000", customer.phone)
    }

    @Test
    fun buildRenderInvoiceUsesFirstPhoneWhenMultiHidden() {
        val bill = Bill(
            id = 1,
            customerName = "张三",
            customerPhone = "13800000000,13900000000",
            showMultiPhones = false,
        )
        val invoice = repo.buildRenderInvoice(bill, emptyList())
        assertEquals("13800000000", invoice.customerPhone)
    }

    @Test
    fun buildRenderInvoiceShowsAllPhonesWhenMultiEnabled() {
        val bill = Bill(
            id = 1,
            customerName = "张三",
            customerPhone = "13800000000,13900000000",
            showMultiPhones = true,
        )
        val invoice = repo.buildRenderInvoice(bill, emptyList())
        assertEquals("13800000000,13900000000", invoice.customerPhone)
    }
}
