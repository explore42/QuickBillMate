package com.example.quickbillmate

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.quickbillmate.data.db.AppDatabase
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.BillItem
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportUiTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearState() {
        val db = AppDatabase.get(context)
        runBlocking {
            db.clearAllTables()
        }
        context.getSharedPreferences("quickbillmate_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    private fun waitFor(timeoutMs: Long = 10000, condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = timeoutMs, condition = condition)
    }

    @Test
    fun reportAggregatesBills() {
        val db = AppDatabase.get(context)
        runBlocking {
            val billId = db.billDao().insert(
                Bill(customerName = "张老板", docDate = "2026-08-01", discount = 10.0),
            )
            db.billItemDao().insertAll(
                listOf(
                    BillItem(billId = billId, name = "腻子粉", qty = 2.0, price = 35.0, sortOrder = 0),
                    BillItem(billId = billId, name = "墙锢", qty = 1.0, price = 65.0, sortOrder = 1),
                ),
            )
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor { composeRule.onAllNodesWithContentDescription("报表").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithContentDescription("报表").performClick()
            waitFor { composeRule.onAllNodesWithText("数据报表").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithText("数据报表").assertIsDisplayed()
            // 汇总：总金额 = 70 + 65 - 10 = 125；单据数 1；客单价 125
            waitFor { composeRule.onAllNodesWithText("¥125.00").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onAllNodesWithText("¥125.00").fetchSemanticsNodes().isNotEmpty()
            composeRule.onAllNodesWithText("1 单").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
