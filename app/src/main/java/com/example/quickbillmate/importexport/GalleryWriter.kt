package com.example.quickbillmate.importexport

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object GalleryWriter {
    const val RELATIVE_DIR = "Pictures/QuickBillMate"
    const val AUTHORITY = "com.example.quickbillmate.fileprovider"

    /** 保存 PNG 到系统相册 Pictures/QuickBillMate。 */
    fun save(context: Context, bitmap: Bitmap, fileName: String): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveModern(context, bitmap, fileName)
        } else {
            saveLegacy(context, bitmap, fileName)
        }

    private fun saveModern(context: Context, bitmap: Bitmap, fileName: String): Boolean {
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
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            ok
        } catch (_: Exception) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {
            }
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "QuickBillMate",
        )
        if (!dir.exists() && !dir.mkdirs()) return false
        val file = File(dir, fileName)
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null,
            )
            true
        } catch (_: Exception) {
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
