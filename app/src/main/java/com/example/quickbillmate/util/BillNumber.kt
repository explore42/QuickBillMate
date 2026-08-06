package com.example.quickbillmate.util

import kotlin.random.Random

object BillNumber {
    /** 单据编号 = 编号代码 + 日期(yyyyMMdd) + 流水号。 */
    fun build(code: String, date: String, serial: String): String =
        code.trim() + DateUtils.compact(date) + serial.trim()

    /** 随机 3 位数字，保留前导零。 */
    fun randomSerial(): String = Random.nextInt(1000).toString().padStart(3, '0')
}
