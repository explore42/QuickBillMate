package com.example.quickbillmate.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * 全局微信二维码图片存储：内部存储单文件 `qr_code.png`。
 * 上传时先在裁剪页截取方形二维码区域，再经 [save] 写入；
 * 单据渲染（预览 / 详情 / 导出共用）时经 [load] 读取。
 */
class QrImageStore(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)

    fun exists(): Boolean = file.exists()

    /** 读取二维码位图；解码时按需降采样，控制渲染内存。 */
    fun load(): Bitmap? {
        if (!file.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()
    }

    fun save(bitmap: Bitmap) {
        file.parentFile?.mkdirs()
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    fun clear() {
        if (file.exists()) file.delete()
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 1
        val maxSide = maxOf(width, height)
        return when {
            maxSide > 2048 -> 4
            maxSide > 1024 -> 2
            else -> 1
        }
    }

    companion object {
        private const val FILE_NAME = "qr_code.png"

        /** 从图片选择器 URI 采样解码，限制最大边长以控制内存；失败返回 null。 */
        fun decodeSampled(context: Context, uri: Uri): Bitmap? = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val sample = when {
                maxOf(bounds.outWidth, bounds.outHeight) > 4096 -> 4
                maxOf(bounds.outWidth, bounds.outHeight) > 2048 -> 2
                else -> 1
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        }.getOrNull()
    }
}
