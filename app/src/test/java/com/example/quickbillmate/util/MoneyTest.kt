package com.example.quickbillmate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun `中文大写金额_设计文档验收表`() {
        assertEquals("零元整", Money.toChineseAmount(0.0))
        assertEquals("伍角", Money.toChineseAmount(0.5))
        assertEquals("壹元整", Money.toChineseAmount(1.00))
        assertEquals("壹拾贰元叁角", Money.toChineseAmount(12.30))
        assertEquals("壹佰元零伍分", Money.toChineseAmount(100.05))
        assertEquals("壹拾万元整", Money.toChineseAmount(100000.00))
        assertEquals("壹仟贰佰叁拾肆元伍角陆分", Money.toChineseAmount(1234.56))
    }

    @Test
    fun `金额统一两位小数`() {
        assertEquals("35.00", Money.format(35.0))
        assertEquals("0.50", Money.format(0.5))
        assertEquals("100.05", Money.format(100.05))
        assertEquals("0.00", Money.format(0.0))
    }

    @Test
    fun `金额输入过滤_最多两位小数`() {
        assertEquals("35", Money.sanitizeAmountInput("35"))
        assertEquals("35.5", Money.sanitizeAmountInput("35.5"))
        assertEquals("35.56", Money.sanitizeAmountInput("35.56"))
        // 第三位小数被丢弃，其余内容不受影响
        assertEquals("35.56", Money.sanitizeAmountInput("35.567"))
        assertEquals("0.99", Money.sanitizeAmountInput("0.999"))
        // 仅保留第一个小数点，多余的小数点与非法字符被剔除
        assertEquals("1.23", Money.sanitizeAmountInput("1.2.3"))
        assertEquals("12.5", Money.sanitizeAmountInput("12元.5"))
        assertEquals("", Money.sanitizeAmountInput(" abc"))
        assertEquals(".", Money.sanitizeAmountInput(".."))
    }

    @Test
    fun `四舍五入到分`() {
        assertEquals(12.35, Money.round2(12.345), 1e-9)
        assertEquals(12.34, Money.round2(12.344), 1e-9)
        assertEquals(1.01, Money.round2(1.005), 1e-9)
    }

    @Test
    fun `含零数字的整数部分`() {
        assertEquals("壹仟零伍元整", Money.toChineseAmount(1005.0))
        assertEquals("壹佰万元整", Money.toChineseAmount(1000000.0))
        assertEquals("壹亿元整", Money.toChineseAmount(100000000.0))
    }

    @Test
    fun `大写字包含零角的情况`() {
        assertEquals("壹佰元零伍分", Money.toChineseAmount(100.05))
        assertEquals("伍分", Money.toChineseAmount(0.05))
        assertEquals("零元整", Money.toChineseAmount(0.0))
        assertTrue(Money.toChineseAmount(100.05).contains("零伍分"))
    }
}
