package com.example.quickbillmate.util

/**
 * 单据商品行数量输入规则：整数，可为负数（退货场景用负数量表达）。
 * - 只保留数字，最多一个前导负号，数字位数不超过 [MAX_DIGITS]；
 * - 单独一个 "-" 属于可用的中间态（继续输入即成为负数）；
 * - 入库与步进统一走 [clamp]，避免出现 -0 这类负零显示。
 */
object QtyInput {
    /** 数量上限（含）。 */
    const val MAX = 9999

    /** 数量下限（含）。 */
    const val MIN = -999

    /** 数字位数上限：上限 9999 为 4 位。 */
    const val MAX_DIGITS = 4

    /**
     * 输入过滤：负数只允许前导负号，数字截断到 [MAX_DIGITS] 位，并把数值收敛到 [MIN]/[MAX]，
     * 保证界面显示的数值与实际生效数值始终一致。
     */
    fun sanitize(raw: String): String {
        val negative = raw.startsWith("-")
        val digits = raw.filter(Char::isDigit).take(MAX_DIGITS)
        if (digits.isEmpty()) return if (negative) "-" else ""
        val value = digits.toLongOrNull() ?: return if (negative) "-" else ""
        return if (negative) "-${minOf(value, -MIN.toLong())}" else minOf(value, MAX.toLong()).toString()
    }

    /** 按上下限收敛并归一化负零（-0 → 0）。 */
    fun clamp(value: Double): Double {
        val bounded = value.coerceIn(MIN.toDouble(), MAX.toDouble())
        return if (bounded == 0.0) 0.0 else bounded
    }
}
