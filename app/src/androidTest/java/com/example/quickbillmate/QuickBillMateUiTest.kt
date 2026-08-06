package com.example.quickbillmate

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickBillMateUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitFor(timeoutMs: Long = 10000, condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = timeoutMs, condition = condition)
    }

    @Test
    fun homeShowsTitleNewButtonAndTabs() {
        composeRule.onNodeWithText("快贝智单").assertIsDisplayed()
        composeRule.onNodeWithText("新建销售清单").assertIsDisplayed()
        composeRule.onNodeWithText("首页").assertIsDisplayed()
        composeRule.onNodeWithText("商品").assertIsDisplayed()
        composeRule.onNodeWithText("客户").assertIsDisplayed()
        composeRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun newBillOpensEditorAndSampleLoadsPreview() {
        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithTag("editor_sample").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithTag("editor_sample").performClick()
        waitFor { composeRule.onAllNodesWithTag("editor_preview").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("editor_preview").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun addProductFromLibraryAddsRowToBill() {
        val productName = "测试腻子${System.currentTimeMillis() % 100000}"

        // 商品页：新增商品
        composeRule.onNodeWithText("商品").performClick()
        waitFor { composeRule.onAllNodesWithContentDescription("新增商品").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithContentDescription("新增商品").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithTag("product_name").performClick()
        composeRule.onNodeWithTag("product_name").performTextInput(productName)
        composeRule.onNodeWithTag("product_price").performClick()
        composeRule.onNodeWithTag("product_price").performTextInput("35")
        composeRule.onNodeWithText("保存").performClick()
        waitFor { composeRule.onAllNodesWithText(productName).fetchSemanticsNodes().isNotEmpty() }

        // 首页 → 新建 → 从商品库添加
        composeRule.onNodeWithText("首页").performClick()
        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithTag("editor_add_from_library").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("editor_add_from_library").performScrollTo().performClick()
        waitFor { composeRule.onAllNodesWithText(productName).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(productName).performClick()

        waitFor { composeRule.onAllNodesWithText("第 2 行").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("第 2 行").assertExists()
    }

    @Test
    fun presetPickerOpensAndAppliesPreset() {
        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithTag("editor_sample").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithContentDescription("样式预设").performClick()
        waitFor { composeRule.onAllNodesWithText("选择样式预设").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("商务蓝").performClick()
        waitFor { composeRule.onAllNodesWithText("选择样式预设").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun darkModeToggleSwitchesImmediately() {
        val current = if (composeRule.onAllNodesWithText("☾ 深色").fetchSemanticsNodes().isNotEmpty()) {
            "☾ 深色"
        } else {
            "☀ 浅色"
        }
        val target = if (current == "☾ 深色") "☀ 浅色" else "☾ 深色"
        composeRule.onNodeWithText(current).performClick()
        waitFor { composeRule.onAllNodesWithText(target).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(target).assertIsDisplayed()
    }
}
