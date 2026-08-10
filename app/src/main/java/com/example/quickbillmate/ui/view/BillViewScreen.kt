package com.example.quickbillmate.ui.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.SectionCard
import com.example.quickbillmate.ui.editor.presetDisplayName
import com.example.quickbillmate.util.BillNumber
import com.example.quickbillmate.util.Money
import com.example.quickbillmate.util.PhoneUtil

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillViewScreen(
    billId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: BillViewViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(Unit) {
        viewModel.load(billId)
    }

    val s = viewModel.state
    val context = LocalContext.current
    var storageDenied by remember { mutableStateOf(false) }
    var previewFull by remember { mutableStateOf(false) }
    var showPreviewMenu by remember { mutableStateOf(false) }

    fun dial(phone: String) {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.exportToGallery() else storageDenied = true
    }

    fun doExport() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.exportToGallery() else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            viewModel.exportToGallery()
        }
    }

    val shareOutcome = s.shareOutcome
    LaunchedEffect(shareOutcome) {
        shareOutcome?.let { outcome ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, outcome.shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享单据"))
            viewModel.consumeShareOutcome()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "单据详情",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },

            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onEdit(billId) },
                    modifier = Modifier.weight(0.8f),
                    enabled = !s.exporting,
                ) {
                    Text("修改")
                }
                OutlinedButton(
                    onClick = { doExport() },
                    modifier = Modifier.weight(1f),
                    enabled = !s.exporting,
                ) {
                    Text(if (s.exporting) "导出中…" else "导出图片")
                }
                Button(
                    onClick = { viewModel.shareNow() },
                    modifier = Modifier.weight(1.3f),
                    enabled = !s.exporting,
                ) {
                    Text("分享图片")
                }
            }
        },
    ) { padding ->
        if (!s.loaded || s.bill == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val bill = s.bill
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                s.preview?.let { bitmap ->
                    Box {
                        // 图片自带轻微卡片观感（圆角 + 阴影），仅显示层，不影响导出位图
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "单据预览",
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = { previewFull = true },
                                    onLongClick = { showPreviewMenu = true },
                                )
                                .testTag("view_preview"),
                        )
                        DropdownMenu(
                            expanded = showPreviewMenu,
                            onDismissRequest = { showPreviewMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("导出图片") },
                                onClick = {
                                    showPreviewMenu = false
                                    viewModel.exportToGallery()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("分享图片") },
                                onClick = {
                                    showPreviewMenu = false
                                    viewModel.shareNow()
                                },
                            )
                        }
                    }
                }

                SectionCard("客户信息") {
                    InfoLine("客户名称", bill.customerName.ifBlank { "—" })
                    val phones = PhoneUtil.splitPhones(bill.customerPhone)
                    val dialPhones = if (bill.showMultiPhones) phones else phones.take(1)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(
                            "客户电话",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(110.dp),
                        )
                        if (dialPhones.isEmpty()) {
                            Text("—", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                dialPhones.forEach { phone ->
                                    PhoneTag(phone = phone, onClick = { dial(phone) })
                                }
                            }
                        }
                    }
                }

                SectionCard("客单信息") {
                    InfoLine(
                        "单据编号",
                        BillNumber.build(bill.docCode, bill.docDate, bill.docSerial).ifBlank { "—" },
                    )
                    InfoLine("单据日期", bill.docDate.ifBlank { "—" })
                    InfoLine("优惠金额", Money.format(bill.discount))
                    if (bill.remark.isNotBlank()) InfoLine("备注", bill.remark)
                    InfoLine("标题后缀", bill.titleSuffix)
                    if (bill.adText.isNotBlank()) InfoLine("广告文案", bill.adText)
                    InfoLine("图片样式", presetDisplayName(bill.presetKey, s.presets))
                }

                SectionCard("商品信息") {
                    if (s.items.isEmpty()) {
                        Text("无商品行", color = MaterialTheme.colorScheme.outline)
                    } else {
                        s.items.forEachIndexed { index, item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${index + 1}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.width(32.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name.ifBlank { "（未填写名称）" }, style = MaterialTheme.typography.bodyMedium)
                                    val detail = listOf(item.spec, item.unit, item.pack)
                                        .filter { it.isNotBlank() }
                                        .joinToString("  ")
                                    if (detail.isNotBlank()) {
                                        Text(
                                            detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (item.note.isNotBlank()) {
                                        Text(
                                            "备注：${item.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${Money.format(item.qty)} × ${Money.format(item.price)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        Money.format(if (item.qty <= 0) 0.0 else Money.round2(item.qty * item.price)),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            if (index != s.items.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        val total = s.items.sumOf {
                            if (it.qty <= 0) 0.0 else Money.round2(it.qty * it.price)
                        }
                        val receivable = Math.max(0.0, Money.round2(total - bill.discount))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "合计：${Money.format(total)}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "应收：${Money.format(receivable)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                SectionCard("公司信息") {
                    InfoLine("公司名称", bill.companyName.ifBlank { "—" })
                    InfoLine("联系电话", bill.contactPhone.ifBlank { "—" })
                    InfoLine("客户经理", bill.salesManager.ifBlank { "—" })
                }

                SectionCard("显示选项") {
                    InfoLine("显示客户经理", if (bill.showManager) "开" else "关")
                    InfoLine("显示备注", if (bill.showRemark) "开" else "关")
                    InfoLine("显示广告", if (bill.showAd) "开" else "关")
                    InfoLine("显示水印", if (bill.showWatermark) "开" else "关")
                    InfoLine("显示多个电话", if (bill.showMultiPhones) "开" else "关")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (previewFull) {
        s.preview?.let {
            FullPreviewDialog(
                bitmap = it,
                onDismiss = { previewFull = false },
                onExport = viewModel::exportToGallery,
                onShare = viewModel::shareNow,
            )
        }
    }

    if (storageDenied) {
        AlertDialog(
            onDismissRequest = { storageDenied = false },
            title = { Text("需要存储权限") },
            text = { Text("保存图片到相册需要存储权限，请到系统设置中授权后重试。") },
            confirmButton = {
                TextButton(onClick = { storageDenied = false }) { Text("知道了") }
            },
        )
    }

    s.exportOutcome?.let { outcome ->
        AlertDialog(
            onDismissRequest = viewModel::consumeExportOutcome,
            title = { Text(if (outcome.saved) "导出成功" else "导出失败") },
            text = { Text(outcome.message) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeExportOutcome) { Text("完成") }
            },
            dismissButton = {
                if (outcome.saved && outcome.shareUri != null) {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, outcome.shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享单据"))
                        viewModel.consumeExportOutcome()
                    }) { Text("分享") }
                }
            },
        )
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
    onValueClick: (() -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = if (onValueClick != null) {
                Modifier.clickable { onValueClick() }
            } else {
                Modifier
            },
        )
    }
}

/** 电话标签：浅色圆角胶囊 + 电话图标，点击拨号。 */
@Composable
private fun PhoneTag(phone: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Call,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            phone,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** 全屏预览：支持双指缩放；点击图片外黑暗背景、右上角或返回关闭；长按图片弹出导出/分享菜单。 */
@Composable
private fun FullPreviewDialog(
    bitmap: android.graphics.Bitmap,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var showMenu by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                // 点击图片实际绘制区域之外的黑暗背景关闭（图片按 Fit 缩放后居中留边）
                .pointerInput(bitmap) {
                    detectTapGestures(
                        onTap = { tap ->
                            val cw = size.width.toFloat()
                            val ch = size.height.toFloat()
                            val fit = Math.min(cw / bitmap.width, ch / bitmap.height)
                            val dw = bitmap.width * fit
                            val dh = bitmap.height * fit
                            val left = (cw - dw) / 2f
                            val top = (ch - dh) / 2f
                            if (tap.x < left || tap.x > left + dw || tap.y < top || tap.y > top + dh) {
                                onDismiss()
                            }
                        },
                        onLongPress = {
                            showMenu = true
                        }
                    )
                },
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "单据预览",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                offset += pan
                            }
                        },
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("导出图片") },
                        onClick = {
                            showMenu = false
                            onExport()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("分享图片") },
                        onClick = {
                            showMenu = false
                            onShare()
                        },
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f)),
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
            }
        }
    }
}
