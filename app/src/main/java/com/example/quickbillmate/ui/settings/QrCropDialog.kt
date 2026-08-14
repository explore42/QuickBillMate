package com.example.quickbillmate.ui.settings

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton

/** 裁剪输出的二维码边长（px）。 */
private const val OUTPUT_SIZE = 720

/**
 * 微信二维码裁剪对话框：图片在方形裁剪框内可拖动 / 双指缩放，
 * 初始定位到图片顶部（适配 1094×1625 收款码布局，二维码在图片上部）。
 * 保存时按裁剪框区域截取正方形位图。
 */
@Composable
fun QrCropDialog(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onSave: (Bitmap) -> Unit,
) {
    BackHandler(onBack = onCancel)
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            val density = LocalDensity.current
            // 方形裁剪窗口：约屏宽 72%，同时不超过可用高度 68%（预留标题与底部按钮）
            val cropSize = minOf(maxWidth * 0.72f, maxHeight * 0.68f)
            val cropPx = with(density) { cropSize.toPx() }
            val fitScale = min(cropPx / bitmap.width, cropPx / bitmap.height)
            val fitW = bitmap.width * fitScale
            val fitH = bitmap.height * fitScale
            // 覆盖基准缩放：图片铺满裁剪窗口后，额外缩放倍数为 1..4
            val coverZoom = max(cropPx / fitW, cropPx / fitH)

            var zoom by remember { mutableFloatStateOf(1f) }
            var offset by remember(bitmap, cropPx) {
                // 初始：图片顶部对齐裁剪窗口顶部、水平居中
                mutableStateOf(Offset(0f, (fitH * coverZoom - cropPx) / 2f))
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "调整二维码区域",
                    color = Color.White,
                    style = AppThemeTypography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(cropSize)
                            .border(2.dp, Color.White),
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "二维码裁剪预览",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = coverZoom * zoom
                                    scaleY = coverZoom * zoom
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoomChange, _ ->
                                        val newZoom = (zoom * zoomChange).coerceIn(1f, 4f)
                                        val w = fitW * coverZoom * newZoom
                                        val h = fitH * coverZoom * newZoom
                                        val maxX = (w - cropPx) / 2f
                                        val maxY = (h - cropPx) / 2f
                                        zoom = newZoom
                                        offset = Offset(
                                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                                            (offset.y + pan.y).coerceIn(-maxY, maxY),
                                        )
                                    }
                                },
                        )
                    }

                    // 裁剪框外遮罩 + 白色边框 + 三分网格
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cw = size.width
                        val ch = size.height
                        val left = (cw - cropPx) / 2f
                        val top = (ch - cropPx) / 2f
                        val right = left + cropPx
                        val bottom = top + cropPx
                        val scrim = Color.Black.copy(alpha = 0.55f)
                        drawRect(scrim, topLeft = Offset(0f, 0f), size = Size(cw, top))
                        drawRect(scrim, topLeft = Offset(0f, bottom), size = Size(cw, ch - bottom))
                        drawRect(scrim, topLeft = Offset(0f, top), size = Size(left, cropPx))
                        drawRect(scrim, topLeft = Offset(right, top), size = Size(cw - right, cropPx))
                        drawRect(
                            Color.White,
                            topLeft = Offset(left, top),
                            size = Size(cropPx, cropPx),
                            style = Stroke(width = 3f),
                        )
                        for (i in 1..2) {
                            val x = left + cropPx * i / 3f
                            drawLine(Color.White.copy(alpha = 0.7f), Offset(x, top), Offset(x, bottom), strokeWidth = 1f)
                            val y = top + cropPx * i / 3f
                            drawLine(Color.White.copy(alpha = 0.7f), Offset(left, y), Offset(right, y), strokeWidth = 1f)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    TextButton(
                        text = "取消",
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(textColor = Color.White),
                    )
                    Button(onClick = {
                        onSave(
                            cropSquare(
                                bitmap = bitmap,
                                fitScale = fitScale,
                                fitW = fitW,
                                fitH = fitH,
                                coverZoom = coverZoom,
                                cropPx = cropPx,
                                zoom = zoom,
                                offset = offset,
                            )
                        )
                    }) { Text("保存裁剪") }
                }
            }
        }
    }
}

/** 按当前裁剪窗口几何截取源图正方形区域，并缩放为 [OUTPUT_SIZE] 输出。 */
private fun cropSquare(
    bitmap: Bitmap,
    fitScale: Float,
    fitW: Float,
    fitH: Float,
    coverZoom: Float,
    cropPx: Float,
    zoom: Float,
    offset: Offset,
): Bitmap {
    val z = coverZoom * zoom
    val pxPerSrc = fitScale * z
    // 渲染后图片左上角在裁剪窗口坐标系中的位置
    val imgLeft = cropPx / 2f + offset.x - fitW * z / 2f
    val imgTop = cropPx / 2f + offset.y - fitH * z / 2f
    val srcLeft = -imgLeft / pxPerSrc
    val srcTop = -imgTop / pxPerSrc
    val srcSide = cropPx / pxPerSrc
    val l = srcLeft.coerceIn(0f, (bitmap.width - 1).toFloat())
    val t = srcTop.coerceIn(0f, (bitmap.height - 1).toFloat())
    val side = minOf(srcSide, bitmap.width - l, bitmap.height - t).coerceAtLeast(1f)
    val cropped = Bitmap.createBitmap(
        bitmap,
        l.roundToInt(),
        t.roundToInt(),
        side.roundToInt(),
        side.roundToInt(),
    )
    return Bitmap.createScaledBitmap(cropped, OUTPUT_SIZE, OUTPUT_SIZE, true)
}
