package com.example.quickbillmate.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DateUtils {
    /** 今天的日期，格式 yyyy-MM-dd。 */
    fun today(): String = LocalDate.now().toString()

    /** 去掉横线的紧凑日期，如 2025-11-21 -> 20251121。 */
    fun compact(date: String): String = date.replace("-", "")

    /** 毫秒时间戳转 yyyy-MM-dd。 */
    fun fromMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    fun isValid(date: String): Boolean =
        try {
            LocalDate.parse(date)
            true
        } catch (_: Exception) {
            false
        }
}
