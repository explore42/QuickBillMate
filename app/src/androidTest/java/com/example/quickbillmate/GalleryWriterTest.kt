package com.example.quickbillmate

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.quickbillmate.importexport.GalleryWriter
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryWriterTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun saveWritesNonEmptyPngToGallery() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val fileName = "gallery_test_${System.currentTimeMillis()}.png"
        var uri: Uri? = null
        try {
            assertTrue(GalleryWriter.save(context, bitmap, fileName))

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.SIZE),
                "${MediaStore.Images.Media.DISPLAY_NAME}=?",
                arrayOf(fileName),
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val size = cursor.getLong(1)
                    assertTrue("导出文件不应为空", size > 0)
                    uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(0),
                    )
                }
            }
            assertNotNull("相册中应能找到导出的文件", uri)
        } finally {
            uri?.let { context.contentResolver.delete(it, null, null) }
            bitmap.recycle()
        }
    }
}
