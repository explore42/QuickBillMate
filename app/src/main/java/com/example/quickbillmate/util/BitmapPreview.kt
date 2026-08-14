package com.example.quickbillmate.util

import android.graphics.Bitmap

/** 显示用预览位图的最大宽度（px）：控制常驻内存，导出时另行渲染全分辨率位图。 */
const val MAX_PREVIEW_WIDTH = 1200

/** 等比缩放到不超过 [maxWidth] 宽；需要缩放时回收原图并返回新图。 */
internal fun Bitmap.shrinkForPreview(maxWidth: Int = MAX_PREVIEW_WIDTH): Bitmap {
    if (width <= maxWidth) return this
    val scale = maxWidth.toFloat() / width
    val scaled = Bitmap.createScaledBitmap(
        this,
        maxWidth,
        (height * scale).toInt().coerceAtLeast(1),
        true,
    )
    if (scaled !== this) recycle()
    return scaled
}
