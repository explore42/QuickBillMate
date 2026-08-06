package com.example.quickbillmate

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
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
        composeRule.onNodeWithContentDescription("新建销售清单").assertIsDisplayed()
        composeRule.onNodeWithText("首页").assertIsDisplayed()
        composeRule.onNodeWithText("商品").assertIsDisplayed()
        composeRule.onNodeWithText("客户").assertIsDisplayed()
        composeRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun newBillOpensEditorAndSampleLoadsPreview() {
        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }

        // 右上角设置 → 应用示例
        composeRule.onNodeWithContentDescription("设置").performClick()
        waitFor { composeRule.onAllNodesWithText("应用示例").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("应用示例").performClick()
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
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithContentDescription("设置").performClick()
        waitFor { composeRule.onAllNodesWithText("选择样式预设").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("选择样式预设").performClick()
        waitFor { composeRule.onAllNodesWithText("商务蓝").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("商务蓝").performClick()
        // 选中后预设列表收起
        waitFor { composeRule.onAllNodesWithText("商务蓝").fetchSemanticsNodes().isEmpty() }
        composeRule.onNodeWithText("关闭").performClick()
        waitFor { composeRule.onAllNodesWithText("关闭").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun discardChangesRemovesNewDraft() {
        val name = "临时客户${System.currentTimeMillis() % 100000}"

        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("客户名称").performClick()
        composeRule.onNodeWithText("客户名称").performTextInput(name)

        composeRule.onNodeWithText("不保存").performClick()
        waitFor { composeRule.onAllNodesWithText("放弃").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("放弃").performClick()

        // 回到首页，且该草稿已删除
        waitFor { composeRule.onAllNodesWithText("最近单据").fetchSemanticsNodes().isNotEmpty() }
        waitFor { composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun deleteSelectedBillRemovesIt() {
        val name = "待删客户${System.currentTimeMillis() % 100000}"

        // 新建一张带唯一客户名的单据
        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("客户名称").performClick()
        composeRule.onNodeWithText("客户名称").performTextInput(name)
        composeRule.onNodeWithText("保存").performClick()
        composeRule.onNodeWithContentDescription("返回").performClick()
        waitFor { composeRule.onAllNodes(hasText(name, substring = true)).fetchSemanticsNodes().isNotEmpty() }

        // 长按进入多选
        composeRule.onAllNodes(hasText(name, substring = true))[0].performTouchInput { longClick() }
        waitFor { composeRule.onAllNodesWithText("已选中 1 项").fetchSemanticsNodes().isNotEmpty() }

        // 底部操作栏点删除 → 确认
        composeRule.onNodeWithText("删除").performClick()
        waitFor { composeRule.onAllNodesWithText("删除单据").fetchSemanticsNodes().isNotEmpty() }
        waitFor { composeRule.onAllNodesWithText("删除").fetchSemanticsNodes().size == 2 }
        composeRule.onAllNodesWithText("删除")[1].performClick()

        // 单据消失，回到普通首页
        waitFor { composeRule.onAllNodes(hasText(name, substring = true)).fetchSemanticsNodes().isEmpty() }
        waitFor { composeRule.onAllNodesWithText("最近单据").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun billViewShowsDetailsAndEditNavigates() {
        // 新建一张单据并载入示例
        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithContentDescription("设置").performClick()
        waitFor { composeRule.onAllNodesWithText("应用示例").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("应用示例").performClick()
        waitFor { composeRule.onAllNodesWithTag("editor_preview").fetchSemanticsNodes().isNotEmpty() }
        // 保存后返回
        composeRule.onNodeWithText("保存").performClick()
        composeRule.onNodeWithContentDescription("返回").performClick()
        waitFor { composeRule.onAllNodesWithText("最近单据").fetchSemanticsNodes().isNotEmpty() }

        // 点击第一条单据（最新）→ 查看页
        waitFor { composeRule.onAllNodesWithText("示例客户").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodesWithText("示例客户")[0].performClick()
        waitFor { composeRule.onAllNodesWithTag("view_preview").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("单据详情").assertIsDisplayed()

        // 编辑 → 编辑页
        composeRule.onNodeWithContentDescription("编辑").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun settingsShowsRowsAndAboutLink() {
        composeRule.onNodeWithText("设置").performClick()
        waitFor { composeRule.onAllNodesWithText("深色模式").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("深色模式").assertIsDisplayed()
        composeRule.onNodeWithText("默认样式预设").assertIsDisplayed()
        composeRule.onNodeWithText("默认公司信息").assertIsDisplayed()

        composeRule.onNodeWithText("关于").performClick()
        waitFor { composeRule.onAllNodesWithText("开源地址：").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("https://github.com/explore42/QuickBillMate").assertIsDisplayed()
    }
}
