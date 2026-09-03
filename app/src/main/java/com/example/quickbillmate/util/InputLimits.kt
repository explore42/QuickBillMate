package com.example.quickbillmate.util

/**
 * 各文本输入字段的最大长度（只约束录入，不截断渲染；超限内容仍完整换行显示）。
 * 录入侧一律用 [take] 截断到上限：粘贴超长内容时保留前缀而不是整段丢弃。
 */
object InputLimits {
    const val NAME = 20
    const val SPEC = 30
    const val PACK = 30
    const val COMPANY = 30
    const val REMARK = 60

    /** 广告文案：支持多行，上限 300 字符（页脚自动换行增高，极限长度下排版良好）。 */
    const val AD = 300

    /** 水印文案：单行短文案，独立于广告上限。 */
    const val WATERMARK = 60
    const val UNIT = 6
    const val CUSTOMER_TYPE = 10
    const val MANAGER = 10
    const val CODE = 10
}
