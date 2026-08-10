package com.example.quickbillmate.util

/** 各文本输入字段的最大长度（只约束录入，不截断渲染；超限内容仍完整换行显示）。 */
object InputLimits {
    const val NAME = 20
    const val SPEC = 30
    const val PACK = 30
    const val COMPANY = 30
    const val REMARK = 60
    const val AD = 60
    const val UNIT = 6
    const val CUSTOMER_TYPE = 10
    const val MANAGER = 10
    const val CODE = 10
}
