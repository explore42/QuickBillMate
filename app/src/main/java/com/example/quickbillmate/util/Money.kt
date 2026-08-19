package com.example.quickbillmate.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object Money {
    private const val DIGITS = "零壹贰叁肆伍陆柒捌玖"
    private val SMALL_UNITS = arrayOf("", "拾", "佰", "仟")
    private val BIG_UNITS = arrayOf("", "万", "亿", "兆")

    /** 四舍五入到分。 */
    fun round2(value: Double): Double =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()

    /** 统一两位小数输出。 */
    fun format(value: Double): String = String.format(Locale.US, "%.2f", round2(value))

    /** 金额输入过滤：仅保留数字与一个小数点，小数位最多 maxDecimals 位（默认两位）。 */
    fun sanitizeAmountInput(raw: String, maxDecimals: Int = 2): String {
        val digits = raw.filter { it.isDigit() || it == '.' }
        val dot = digits.indexOf('.')
        if (dot < 0) return digits
        val intPart = digits.take(dot)
        val decimals = digits.substring(dot + 1).filter { it != '.' }.take(maxDecimals)
        return "$intPart.$decimals"
    }

    /** 中文大写金额，与 DEMO toChineseAmount() 逻辑一致。 */
    fun toChineseAmount(amount: Double): String {
        val rounded = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
        val intPart = rounded.toBigInteger().toString()
        val cents = rounded.movePointRight(2).remainder(BigDecimal(100)).abs().toInt()
        val jiao = cents / 10
        val fen = cents % 10
        val intZero = intPart == "0"

        val sb = StringBuilder()
        if (!intZero) {
            sb.append(intToChinese(intPart)).append("元")
        }
        if (jiao > 0) {
            sb.append(DIGITS[jiao]).append("角")
        }
        if (fen > 0) {
            if (!intZero && jiao == 0) sb.append("零")
            sb.append(DIGITS[fen]).append("分")
        }
        if (intZero && jiao == 0 && fen == 0) return "零元整"
        if (!intZero && jiao == 0 && fen == 0) sb.append("整")
        return sb.toString()
    }

    private fun intToChinese(num: String): String {
        val groups = ArrayList<String>()
        var s = num
        while (s.length > 4) {
            groups.add(0, s.takeLast(4))
            s = s.dropLast(4)
        }
        groups.add(0, s)

        val sb = StringBuilder()
        var zeroPending = false
        val bigCount = groups.size
        for ((gi, g) in groups.withIndex()) {
            val gv = g.toInt()
            if (gv == 0) {
                if (bigCount > 1) zeroPending = true
                continue
            }
            if (zeroPending) {
                sb.append("零")
                zeroPending = false
            }
            var innerZero = false
            for (i in g.indices) {
                val d = g[i] - '0'
                val pos = g.length - 1 - i
                if (d == 0) {
                    innerZero = true
                } else {
                    if (innerZero) {
                        sb.append("零")
                        innerZero = false
                    }
                    sb.append(DIGITS[d]).append(SMALL_UNITS[pos])
                }
            }
            sb.append(BIG_UNITS[bigCount - 1 - gi])
        }
        return sb.toString()
    }
}
