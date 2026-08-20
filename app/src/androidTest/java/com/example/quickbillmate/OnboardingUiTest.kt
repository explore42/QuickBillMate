package com.example.quickbillmate

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.quickbillmate.data.db.AppDatabase
import com.example.quickbillmate.data.db.Product
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingUiTest {

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
    fun freshInstallShowsOnboardingAndCompletes() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor { composeRule.onAllNodesWithText("去填写默认信息").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithText("去填写默认信息").performClick()
            waitFor { composeRule.onAllNodesWithText("保存并开始使用").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithText("保存并开始使用").performClick()
            waitFor { composeRule.onAllNodesWithText("还没有单据，点击右下角新建").fetchSemanticsNodes().isNotEmpty() }
        }
    }

    @Test
    fun upgradeWithExistingDataSkipsOnboarding() {
        runBlocking {
            AppDatabase.get(context).productDao().insert(
                Product(name = "测试商品", price = 1.0),
            )
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor { composeRule.onAllNodesWithText("还没有单据，点击右下角新建").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onAllNodesWithText("去填写默认信息").fetchSemanticsNodes().isEmpty()
        }
    }
}
