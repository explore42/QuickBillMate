package com.example.quickbillmate

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerFeaturesUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitFor(timeoutMs: Long = 10000, condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = timeoutMs, condition = condition)
    }

    @Test
    fun customerFavoriteMovesToTop() {
        val nameA = "客户甲${System.currentTimeMillis() % 100000}"
        val nameB = "客户乙${System.currentTimeMillis() % 100000}"

        composeRule.onNodeWithContentDescription("客户").performClick()
        waitFor { composeRule.onAllNodesWithContentDescription("新增客户").fetchSemanticsNodes().isNotEmpty() }
        addCustomer(nameA)
        addCustomer(nameB)

        // 点击行先打开“客户详情”，点“修改”进入编辑，勾选“收藏”并保存
        waitFor { composeRule.onAllNodes(hasText(nameA, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodes(hasText(nameA, substring = true))[0].performClick()
        waitFor { composeRule.onAllNodesWithText("客户详情").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("修改").performClick()
        waitFor { composeRule.onAllNodesWithText("编辑客户").fetchSemanticsNodes().isNotEmpty() }
        waitFor { composeRule.onAllNodesWithText("收藏").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("收藏").performClick()
        composeRule.onNodeWithText("保存").performClick()

        // 收藏后，收藏客户应排在其他客户前面
        waitFor {
            val nodesA = composeRule.onAllNodes(hasText(nameA, substring = true)).fetchSemanticsNodes()
            val nodesB = composeRule.onAllNodes(hasText(nameB, substring = true)).fetchSemanticsNodes()
            nodesA.isNotEmpty() && nodesB.isNotEmpty() && nodesA[0].boundsInRoot.top < nodesB[0].boundsInRoot.top
        }
    }

    @Test
    fun editorCustomerDropdownShowsAndSelectsLibraryCustomer() {
        val name = "客户丙${System.currentTimeMillis() % 100000}"

        composeRule.onNodeWithContentDescription("客户").performClick()
        waitFor { composeRule.onAllNodesWithContentDescription("新增客户").fetchSemanticsNodes().isNotEmpty() }
        addCustomer(name)

        // 单据页 → 新建 → 打开客户名称下拉
        composeRule.onNodeWithContentDescription("单据").performClick()
        composeRule.onNodeWithTag("home_new_bill").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }
        waitFor { composeRule.onAllNodesWithText("客户名称").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("客户名称").performClick()

        waitFor { composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(name).performClick()

        // 选中后回填到输入框
        waitFor { composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(name).assertExists()
    }

    private fun addCustomer(name: String) {
        composeRule.onNodeWithContentDescription("新增客户").performClick()
        waitFor { composeRule.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("姓名*").performClick()
        composeRule.onNodeWithText("姓名*").performTextInput(name)
        composeRule.onNodeWithText("保存").performClick()
        waitFor { composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
    }


}
