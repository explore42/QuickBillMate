package com.example.quickbillmate.util

/** 电话工具：规范化、校验、拆分与展示。 */
object PhoneUtil {

    /**
     * 规范化电话：去空格/连字符/括号/点；去 +86、0086 国内前缀；
     * 00… 国际前缀转 +…；其余保留数字与 +。
     */
    fun normalizePhone(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return ""
        s = s.filter { it.isDigit() || it == '+' }
        if (s.isEmpty()) return ""
        if (s.startsWith("+86")) return s.drop(3)
        if (s.startsWith("0086")) return s.drop(4)
        if (s.startsWith("00")) return "+" + s.drop(2)
        return s
    }

    /** 宽松校验：空串非法；+ 开头时其后 7–15 位数字合法；否则 7–12 位数字合法。 */
    fun isValidPhone(normalized: String): Boolean {
        if (normalized.isEmpty()) return false
        return if (normalized.startsWith("+")) {
            normalized.length in 8..16 && normalized.drop(1).all { it.isDigit() }
        } else {
            normalized.length in 7..12 && normalized.all { it.isDigit() }
        }
    }

    /** 按逗号拆分并规范化，丢弃空项。 */
    fun splitPhones(raw: String): List<String> =
        raw.split(",").map { normalizePhone(it) }.filter { it.isNotEmpty() }

    /** 展示电话：关闭多电话时只取第一个；开启时返回原串（去掉末尾多余逗号）。 */
    fun displayPhones(raw: String, showMulti: Boolean): String =
        if (showMulti) raw.trim().trimEnd(',') else raw.split(",").firstOrNull()?.trim().orEmpty()
}
