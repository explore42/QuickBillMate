package com.example.quickbillmate

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.quickbillmate.data.db.AppDatabase
import com.example.quickbillmate.util.AppVersion
import com.example.quickbillmate.util.LocalCrashLogger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrashLogUiTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun seedCrashLog() {
        runBlocking { AppDatabase.get(context).clearAllTables() }
        // 保持“已引导、已看过当前版本说明”状态，避免引导页/更新说明拦截主界面
        context.getSharedPreferences("quickbillmate_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", true)
            .putInt("last_seen_version_code", AppVersion.code(context))
            .commit()
        LocalCrashLogger.clearLogs(context)
        LocalCrashLogger.writeLogForTest(IllegalStateException("测试崩溃"), context)
    }

    @After
    fun cleanup() {
        LocalCrashLogger.clearLogs(context)
    }

    private fun waitFor(timeoutMs: Long = 10000, condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = timeoutMs, condition = condition)
    }

    @Test
    fun crashLogVisibleAndClearable() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor { composeRule.onAllNodesWithContentDescription("设置").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithContentDescription("设置").performClick()
            waitFor { composeRule.onAllNodesWithText("崩溃日志").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithText("崩溃日志").performScrollTo()
            composeRule.onNodeWithText("最近 1 条").assertIsDisplayed()

            composeRule.onNodeWithText("崩溃日志").performSemanticsAction(SemanticsActions.OnClick)
            waitFor {
                composeRule.onAllNodesWithText("java.lang.IllegalStateException: 测试崩溃")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("java.lang.IllegalStateException: 测试崩溃").assertIsDisplayed()

            // 清除 → 二次确认 → 弹窗显示空态
            composeRule.onNodeWithText("清除").performClick()
            waitFor { composeRule.onAllNodesWithText("清除崩溃日志").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithText("确认清除").performClick()
            waitFor { composeRule.onAllNodesWithText("暂无崩溃记录").fetchSemanticsNodes().isNotEmpty() }
        }
    }
}
