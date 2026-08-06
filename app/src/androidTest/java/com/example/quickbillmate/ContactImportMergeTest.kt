package com.example.quickbillmate

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.quickbillmate.data.db.AppDatabase
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.importexport.ContactsImporter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactImportMergeTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: AppRepository
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = AppRepository(db, SettingsStore(context))
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun sameNameDiffPhoneMergesAndAppends() = runBlocking {
        repo.saveCustomer(Customer(name = "张三", phone = "13800000000"))

        val outcome = repo.importContactCandidates(
            listOf(ContactsImporter.Candidate("张三", "13900000000"))
        )

        assertEquals(0, outcome.inserted)
        assertEquals(1, outcome.merged)
        val customer = repo.getCustomers().single()
        assertEquals("13800000000,13900000000", customer.phone)
    }

    @Test
    fun sameNameSamePhoneCountsAsMergedWithoutChange() = runBlocking {
        repo.saveCustomer(Customer(name = "张三", phone = "13800000000"))

        val outcome = repo.importContactCandidates(
            listOf(ContactsImporter.Candidate("张三", "13800000000"))
        )

        assertEquals(0, outcome.inserted)
        assertEquals(1, outcome.merged)
        assertEquals("13800000000", repo.getCustomers().single().phone)
    }

    @Test
    fun newNameInsertsCustomer() = runBlocking {
        val outcome = repo.importContactCandidates(
            listOf(ContactsImporter.Candidate("李四", "13700000000"))
        )

        assertEquals(1, outcome.inserted)
        assertEquals(0, outcome.merged)
        val customer = repo.getCustomers().single()
        assertEquals("李四", customer.name)
        assertEquals("13700000000", customer.phone)
    }
}
