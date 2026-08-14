package com.example.quickbillmate.importexport

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object GalleryWriter {
    const val RELATIVE_DIR = "Pictures/QuickBillMate"
    const val AUTHORITY = "com.example.quickbillmate.fileprovider"

    /** 保存 PNG 到系统相册 Pictures/QuickBillMate。 */
    fun save(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_DIR)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        return try {
            val ok = context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: false
            if (ok) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                true
            } else {
                // 压缩失败（返回 false 不抛异常）：删除残留的空文件
                context.contentResolver.delete(uri, null, null)
                false
            }
        } catch (_: Exception) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {
            }
            false
        }
    }

    /** 把位图写入应用缓存并返回可分享的 FileProvider URI。 */
    fun shareUri(context: Context, bitmap: Bitmap, fileName: String): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, AUTHORITY, file)
    }
}
