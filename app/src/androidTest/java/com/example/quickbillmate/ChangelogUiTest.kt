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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChangelogUiTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs =
        context.getSharedPreferences("quickbillmate_settings", Context.MODE_PRIVATE)

    @Before
    fun clearState() {
        runBlocking { AppDatabase.get(context).clearAllTables() }
        prefs.edit().clear().commit()
    }

    @After
    fun restoreSafeState() {
        prefs.edit()
            .putBoolean("onboarding_completed", true)
            .putInt("last_seen_version_code", com.example.quickbillmate.util.AppVersion.code(context))
            .commit()
        runBlocking { AppDatabase.get(context).clearAllTables() }
    }

    private fun waitFor(timeoutMs: Long = 10000, condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = timeoutMs, condition = condition)
    }

    private fun seedExistingUser(lastSeen: Int) {
        runBlocking {
            AppDatabase.get(context).productDao().insert(
                Product(name = "旧数据", price = 1.0),
            )
        }
        prefs.edit().putInt("last_seen_version_code", lastSeen).commit()
    }

    @Test
    fun upgradeShowsChangelogOnceThenMain() {
        seedExistingUser(lastSeen = 4)

        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor { composeRule.onAllNodesWithText("v1.2.0 主要更新").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithText("开始使用").performClick()
            waitFor { composeRule.onAllNodesWithText("还没有单据，点击右下角新建").fetchSemanticsNodes().isNotEmpty() }
        }

        // 重启后不再显示更新说明
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor { composeRule.onAllNodesWithText("还没有单据，点击右下角新建").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onAllNodesWithText("v1.2.0 主要更新").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun oldUserFirstRunShowsChangelog() {
        seedExistingUser(lastSeen = 0)

        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor { composeRule.onAllNodesWithText("v1.2.0 主要更新").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithText("开始使用").performClick()
            waitFor { composeRule.onAllNodesWithText("还没有单据，点击右下角新建").fetchSemanticsNodes().isNotEmpty() }
        }
    }
}
