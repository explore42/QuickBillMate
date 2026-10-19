package com.example.quickbillmate

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.quickbillmate.data.db.AppDatabase
import com.example.quickbillmate.util.AppVersion
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 预置单位：内置「袋」替代旧「代」，并与存量自定义单位合并去重。
 */
@RunWith(AndroidJUnit4::class)
class UnitPresetsUiTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs =
        context.getSharedPreferences("quickbillmate_settings", Context.MODE_PRIVATE)

    @Before
    fun prepareSettings() {
        runBlocking { AppDatabase.get(context).clearAllTables() }
        prefs.edit().clear().commit()
        // 直接进入主界面；并模拟旧版本留下的自定义「袋」（写入应与内置合并，不再重复）
        prefs.edit()
            .putBoolean("onboarding_completed", true)
            .putInt("last_seen_version_code", AppVersion.code(context))
            .putString("custom_units", "袋,箱,袋")
            .commit()
    }

    @After
    fun restoreSafeState() {
        prefs.edit().clear().commit()
        prefs.edit()
            .putBoolean("onboarding_completed", true)
            .putInt("last_seen_version_code", AppVersion.code(context))
            .commit()
        runBlocking { AppDatabase.get(context).clearAllTables() }
    }

    private fun waitFor(timeoutMs: Long = 10000, condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = timeoutMs, condition = condition)
    }

    @Test
    fun presetDialogShowsBagInBuiltinWithoutDuplicateCustomEntry() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithContentDescription("设置").performClick()
            waitFor { composeRule.onAllNodesWithText("预置单位").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithText("预置单位").performScrollTo().performClick()
            waitFor { composeRule.onAllNodesWithText("内置单位").fetchSemanticsNodes().isNotEmpty() }

            composeRule.onNodeWithText("桶　筒　袋　张　卷　支　把").assertIsDisplayed()
            // 自定义单位保留「箱」，但「袋」已并入内置，不再单列；旧「代」彻底消失
            composeRule.onNodeWithText("箱").assertIsDisplayed()
            assertEquals(0, composeRule.onAllNodesWithText("袋").fetchSemanticsNodes().size)
            assertEquals(0, composeRule.onAllNodesWithText("代").fetchSemanticsNodes().size)
        }
    }

    @Test
    fun productUnitDropdownListsBagExactlyOnce() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithContentDescription("商品").performClick()
            waitFor { composeRule.onAllNodesWithText("还没有商品，点击右下角新增").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithContentDescription("新增商品").performClick()
            waitFor { composeRule.onAllNodesWithText("新增商品").fetchSemanticsNodes().isNotEmpty() }

            composeRule.onNodeWithTag("product_unit").performScrollTo().performClick()
            waitFor { composeRule.onAllNodesWithText("袋").fetchSemanticsNodes().isNotEmpty() }

            assertEquals(1, composeRule.onAllNodesWithText("袋").fetchSemanticsNodes().size)
            assertEquals(0, composeRule.onAllNodesWithText("代").fetchSemanticsNodes().size)
        }
    }
}
