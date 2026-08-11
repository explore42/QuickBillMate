package com.example.quickbillmate

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
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
import androidx.test.espresso.Espresso
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

    private fun waitHomeReady() {
        waitFor {
            composeRule.onAllNodesWithContentDescription("新建单据").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun createBillWithCustomer(name: String) {
        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("客户名称").performClick()
        composeRule.onNodeWithText("客户名称").performTextInput(name)
        composeRule.onNodeWithText("保存").performClick()
        waitHomeReady()
        waitFor { composeRule.onAllNodes(hasText(name, substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun homeShowsTitleNewButtonAndTabs() {
        composeRule.onNodeWithText("快贝智单").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("新建单据").assertIsDisplayed()
        composeRule.onNodeWithText("单据").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("商品").fetchSemanticsNodes().isNotEmpty()
        composeRule.onAllNodesWithContentDescription("客户").fetchSemanticsNodes().isNotEmpty()
        composeRule.onAllNodesWithContentDescription("设置").fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun newBillOpensEditorAndShowsPreview() {
        val name = "预览客户${System.currentTimeMillis() % 100000}"
        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }

        // 填写客户名称后，实时预览渲染
        composeRule.onNodeWithText("客户名称").performClick()
        composeRule.onNodeWithText("客户名称").performTextInput(name)
        waitFor { composeRule.onAllNodesWithTag("editor_preview").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("editor_preview").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun addProductFromLibraryAddsRowToBill() {
        val productName = "测试腻子${System.currentTimeMillis() % 100000}"

        // 商品页：新增商品
        composeRule.onNodeWithContentDescription("商品").performClick()
        waitFor { composeRule.onAllNodesWithContentDescription("新增商品").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithContentDescription("新增商品").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithTag("product_name").performClick()
        composeRule.onNodeWithTag("product_name").performTextInput(productName)
        composeRule.onNodeWithTag("product_price").performClick()
        composeRule.onNodeWithTag("product_price").performTextInput("35")
        composeRule.onNodeWithText("保存").performClick()
        waitFor { composeRule.onAllNodesWithText(productName).fetchSemanticsNodes().isNotEmpty() }

        // 单据页 → 新建 → 从商品库添加
        composeRule.onNodeWithContentDescription("单据").performClick()
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
        waitFor { composeRule.onAllNodesWithText("选择图片样式").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("选择图片样式").performClick()
        waitFor { composeRule.onAllNodesWithText("商务蓝").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("商务蓝").performClick()
        // 选中后预设列表收起
        waitFor { composeRule.onAllNodesWithText("商务蓝").fetchSemanticsNodes().isEmpty() }
        // 返回键关闭设置弹窗
        Espresso.pressBack()
        waitFor { composeRule.onAllNodesWithText("选择图片样式").fetchSemanticsNodes().isEmpty() }
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

        // 回到单据页，且该草稿已删除
        waitHomeReady()
        waitFor { composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun deleteSelectedBillRemovesIt() {
        val name = "待删客户${System.currentTimeMillis() % 100000}"
        createBillWithCustomer(name)

        // 长按进入多选
        composeRule.onAllNodes(hasText(name, substring = true))[0].performTouchInput { longClick() }
        waitFor { composeRule.onAllNodesWithText("已选中 1 项").fetchSemanticsNodes().isNotEmpty() }

        // 底部操作栏点删除 → 确认
        composeRule.onNodeWithText("删除").performClick()
        waitFor { composeRule.onAllNodesWithText("删除单据").fetchSemanticsNodes().isNotEmpty() }
        waitFor { composeRule.onAllNodesWithText("删除").fetchSemanticsNodes().size == 2 }
        composeRule.onAllNodesWithText("删除")[1].performClick()

        // 单据消失，回到普通单据页
        waitFor { composeRule.onAllNodes(hasText(name, substring = true)).fetchSemanticsNodes().isEmpty() }
        waitHomeReady()
    }

    @Test
    fun homeSearchFiltersBills() {
        val name = "搜索客户${System.currentTimeMillis() % 100000}"
        createBillWithCustomer(name)

        // 点击标题栏搜索图标展开
        composeRule.onNodeWithContentDescription("搜索").performClick()
        waitFor {
            composeRule.onAllNodesWithText("搜索商品 / 客户 / 时间").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasSetTextAction()).performTextInput(name)
        waitFor { composeRule.onAllNodes(hasText(name, substring = true)).fetchSemanticsNodes().isNotEmpty() }

        // 其他单据被过滤掉，只剩匹配的那条
        waitFor {
            composeRule.onAllNodes(hasText("示例客户", substring = true)).fetchSemanticsNodes().isEmpty()
        }

        // 叉号清除后输入无结果关键字，显示空态文案
        composeRule.onNodeWithContentDescription("清除搜索").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("绝对不存在的关键字xyz")
        waitFor { composeRule.onAllNodesWithText("没有找到匹配的单据").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun backInSelectionExitsSelectionInsteadOfFinishing() {
        val name = "返回客户${System.currentTimeMillis() % 100000}"
        createBillWithCustomer(name)

        composeRule.onAllNodes(hasText(name, substring = true))[0].performTouchInput { longClick() }
        waitFor { composeRule.onAllNodesWithText("已选中 1 项").fetchSemanticsNodes().isNotEmpty() }

        Espresso.pressBack()

        // 退出多选而不是退出应用
        waitFor { composeRule.onAllNodesWithText("已选中").fetchSemanticsNodes().isEmpty() }
        composeRule.onNodeWithText("快贝智单").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("新建单据").assertIsDisplayed()
    }

    @Test
    fun billViewShowsDetailsAndEditNavigates() {
        val name = "详情客户${System.currentTimeMillis() % 100000}"
        // 新建一张单据并填写客户名称
        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("客户名称").performClick()
        composeRule.onNodeWithText("客户名称").performTextInput(name)
        waitFor { composeRule.onAllNodesWithTag("editor_preview").fetchSemanticsNodes().isNotEmpty() }
        // 保存后自动返回单据列表
        composeRule.onNodeWithText("保存").performClick()
        waitHomeReady()

        // 点击第一条单据（最新）→ 查看页
        waitFor { composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodesWithText(name)[0].performClick()
        waitFor { composeRule.onAllNodesWithTag("view_preview").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("单据详情").assertIsDisplayed()

        // 修改 → 编辑页
        composeRule.onNodeWithText("修改").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun settingsShowsRowsAndAboutLink() {
        composeRule.onNodeWithContentDescription("设置").performClick()
        waitFor { composeRule.onAllNodesWithText("深色模式").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("深色模式").assertIsDisplayed()
        composeRule.onNodeWithText("默认图片样式").assertIsDisplayed()
        composeRule.onNodeWithText("默认公司信息").assertIsDisplayed()

        composeRule.onNodeWithText("关于").performClick()
        waitFor { composeRule.onAllNodesWithText("开源地址：").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("https://github.com/explore42/QuickBillMate").assertIsDisplayed()
    }

    @Test
    fun productClickShowsDetailThenEdit() {
        val name = "商品详情测试${System.currentTimeMillis() % 100000}"
        composeRule.onNodeWithContentDescription("商品").performClick()
        waitFor { composeRule.onAllNodesWithContentDescription("新增商品").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithContentDescription("新增商品").performClick()
        waitFor { composeRule.onAllNodesWithText("新增商品").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("product_name").performClick()
        composeRule.onNodeWithTag("product_name").performTextInput(name)
        composeRule.onNodeWithTag("product_price").performClick()
        composeRule.onNodeWithTag("product_price").performTextInput("35")
        composeRule.onNodeWithText("保存").performClick()
        waitFor { composeRule.onAllNodes(hasText(name, substring = true)).fetchSemanticsNodes().isNotEmpty() }

        // 点击行先打开“商品详情”，点“修改”才进入编辑
        composeRule.onAllNodes(hasText(name, substring = true))[0].performClick()
        waitFor { composeRule.onAllNodesWithText("商品详情").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("修改").performClick()
        waitFor { composeRule.onAllNodesWithText("编辑商品").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun customerClickShowsDetailWithCallAndEdit() {
        val name = "客户详情测试${System.currentTimeMillis() % 100000}"
        composeRule.onNodeWithContentDescription("客户").performClick()
        waitFor { composeRule.onAllNodesWithContentDescription("新增客户").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithContentDescription("新增客户").performClick()
        waitFor { composeRule.onAllNodesWithText("新增客户").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("姓名*").performClick()
        composeRule.onNodeWithText("姓名*").performTextInput(name)
        composeRule.onAllNodesWithTag("phone_input")[0].performClick()
        composeRule.onAllNodesWithTag("phone_input")[0].performTextInput("13800001111")
        composeRule.onNodeWithText("保存").performClick()
        waitFor { composeRule.onAllNodes(hasText(name, substring = true)).fetchSemanticsNodes().isNotEmpty() }

        // 点击行先打开“客户详情”，含“呼叫”与“修改”
        composeRule.onAllNodes(hasText(name, substring = true))[0].performClick()
        waitFor { composeRule.onAllNodesWithText("客户详情").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("呼叫").assertIsDisplayed()
        composeRule.onNodeWithText("修改").performClick()
        waitFor { composeRule.onAllNodesWithText("编辑客户").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun selectAllTogglesToDeselectAll() {
        val first = "全选切换甲${System.currentTimeMillis() % 100000}"
        val second = "全选切换乙${System.currentTimeMillis() % 100000}"
        createBillWithCustomer(first)
        createBillWithCustomer(second)

        // 只选第一条时，顶栏为“全选”（未全选）
        composeRule.onAllNodes(hasText(first, substring = true))[0].performTouchInput { longClick() }
        waitFor { composeRule.onAllNodesWithText("已选中 1 项").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("select_all_toggle").assertTextEquals("全选")

        // 全选 → 变为“取消全选”
        composeRule.onNodeWithTag("select_all_toggle").performClick()
        waitFor { composeRule.onAllNodesWithText("取消全选").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("select_all_toggle").assertTextEquals("取消全选")

        // 取消全选 → 清空但留在多选模式，按钮恢复“全选”
        composeRule.onNodeWithTag("select_all_toggle").performClick()
        waitFor { composeRule.onAllNodesWithText("已选中 0 项").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("select_all_toggle").assertTextEquals("全选")

        Espresso.pressBack()
        waitFor { composeRule.onAllNodesWithText("已选中").fetchSemanticsNodes().isEmpty() }
        composeRule.onNodeWithText("快贝智单").assertIsDisplayed()
    }

    @Test
    fun groupSelectAllMergesWithExistingSelection() {
        val first = "合并测试甲${System.currentTimeMillis() % 100000}"
        val second = "合并测试乙${System.currentTimeMillis() % 100000}"
        createBillWithCustomer(first)

        // 新建一张收藏单据
        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("收藏").performClick()
        composeRule.onNodeWithText("客户名称").performClick()
        composeRule.onNodeWithText("客户名称").performTextInput(second)
        composeRule.onNodeWithText("保存").performClick()
        waitHomeReady()
        waitFor { composeRule.onAllNodes(hasText(second, substring = true)).fetchSemanticsNodes().isNotEmpty() }

        // 长按月份组里的单据，再点收藏组“全选”→ 与已有选中合并
        composeRule.onAllNodes(hasText(first, substring = true))[0].performTouchInput { longClick() }
        waitFor { composeRule.onAllNodesWithText("已选中 1 项").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodesWithTag("select_group")[0].performClick()
        waitFor { composeRule.onAllNodesWithText("已选中 2 项").fetchSemanticsNodes().isNotEmpty() }
    }
}
