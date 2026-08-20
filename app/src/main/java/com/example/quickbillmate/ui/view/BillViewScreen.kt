package com.example.quickbillmate.ui.view

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.DialogButtons
import com.example.quickbillmate.ui.common.LocalHaptics
import com.example.quickbillmate.ui.common.MiuixMenuPopup
import com.example.quickbillmate.ui.common.PhoneTag
import com.example.quickbillmate.ui.common.SectionCard
import com.example.quickbillmate.ui.editor.presetDisplayName
import com.example.quickbillmate.util.BillNumber
import com.example.quickbillmate.util.Money
import com.example.quickbillmate.util.PhoneUtil
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.icon.extended.Share
import kotlinx.coroutines.launch
import kotlin.math.abs
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    var previewFull by remember { mutableStateOf(false) }
    var showPreviewMenu by remember { mutableStateOf(false) }

    fun dial(phone: String) {
        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
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

    // 底部操作栏与底部导航栏一致的模糊材质：内容捕获层 + 半透明覆盖
    val backgroundColor = MiuixTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "单据详情",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },

            )
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
                    .layerBackdrop(backdrop)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val previewBitmap = s.preview
                if (previewBitmap == null) {
                    // 首张预览渲染完成前的占位动画（慢速设备上渲染耗时更久）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box {
                        // 图片自带轻微卡片观感（圆角 + 阴影），仅显示层，不影响导出位图
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "单据预览",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = { previewFull = true },
                                    onLongClick = { showPreviewMenu = true },
                                )
                                .testTag("view_preview"),
                        )
                        MiuixMenuPopup(
                            expanded = showPreviewMenu,
                            onDismiss = { showPreviewMenu = false },
                            items = listOf(
                                "保存图片" to {
                                    showPreviewMenu = false
                                    viewModel.exportToGallery()
                                },
                                "分享图片" to {
                                    showPreviewMenu = false
                                    viewModel.shareNow()
                                },
                            ),
                        )
                    }
                }

                SectionCard("客户信息") {
                    InfoLine("客户名称", bill.customerName.ifBlank { "—" })
                    val phones = PhoneUtil.splitPhones(bill.customerPhone)
                    val dialPhones = when {
                        !bill.showCustomerPhone -> emptyList()
                        bill.showMultiPhones -> phones
                        else -> phones.take(1)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(
                            "客户电话",
                            style = AppThemeTypography.bodyMedium,
                            color = AppThemeColors.onSurfaceVariant,
                            modifier = Modifier.width(96.dp),
                        )
                        if (dialPhones.isEmpty()) {
                            Text("—", style = AppThemeTypography.bodyMedium)
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
                    if (bill.remark.isNotBlank()) InfoLine("备注", bill.remark)
                    InfoLine("标题后缀", bill.titleSuffix)
                    if (bill.adText.isNotBlank()) InfoLine("广告文案", bill.adText)
                    InfoLine("图片样式", presetDisplayName(bill.presetKey, s.presets))
                }

                SectionCard("商品信息") {
                    if (s.items.isEmpty()) {
                        Text("无商品行", color = AppThemeColors.outline)
                    } else {
                        s.items.forEachIndexed { index, item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${index + 1}.",
                                    style = AppThemeTypography.bodySmall,
                                    color = AppThemeColors.outline,
                                    modifier = Modifier.width(32.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name.ifBlank { "（未填写名称）" }, style = AppThemeTypography.bodyMedium)
                                    val detail = listOf(item.spec, item.unit, item.pack)
                                        .filter { it.isNotBlank() }
                                        .joinToString("  ")
                                    if (detail.isNotBlank()) {
                                        Text(
                                            detail,
                                            style = AppThemeTypography.bodySmall,
                                            color = AppThemeColors.onSurfaceVariant,
                                        )
                                    }
                                    if (item.note.isNotBlank()) {
                                        Text(
                                            "备注：${item.note}",
                                            style = AppThemeTypography.bodySmall,
                                            color = AppThemeColors.onSurfaceVariant,
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${item.qty.toLong()} × ${Money.format(item.price)}",
                                        style = AppThemeTypography.bodySmall,
                                        color = AppThemeColors.onSurfaceVariant,
                                    )
                                    Text(
                                        Money.format(if (item.qty <= 0) 0.0 else Money.round2(item.qty * item.price)),
                                        style = AppThemeTypography.bodyMedium,
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
                        Text(
                            "合计：${Money.format(total)}",
                            style = AppThemeTypography.bodyMedium,
                        )
                        if (bill.discount != 0.0) {
                            Text(
                                "优惠：${Money.format(bill.discount)}",
                                style = AppThemeTypography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Text(
                            "应收：${Money.format(receivable)}（${Money.toChineseAmount(receivable)}）",
                            style = AppThemeTypography.titleSmall,
                            color = AppThemeColors.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                SectionCard("公司信息") {
                    InfoLine("公司名称", bill.companyName.ifBlank { "—" })
                    InfoLine("联系电话", bill.contactPhone.ifBlank { "—" })
                    InfoLine("客户经理", bill.salesManager.ifBlank { "—" })
                }

                SectionCard("显示选项") {
                    //（底部操作栏为覆盖层，列表末尾留出滑过空间）
                    InfoLine("显示客户经理", if (bill.showManager) "开" else "关")
                    InfoLine("显示备注", if (bill.showRemark) "开" else "关")
                    InfoLine("显示广告", if (bill.showAd) "开" else "关")
                    InfoLine("显示水印", if (bill.showWatermark) "开" else "关")
                    InfoLine(
                        "客户电话",
                        when {
                            !bill.showCustomerPhone -> "不显示"
                            bill.showMultiPhones -> "显示多个"
                            else -> "显示一个"
                        },
                    )
                }
                Spacer(Modifier.height(96.dp))
            }
        }
    }

        // 修改 / 保存图片为纯图标按钮；分享图片（最重要）为 icon+文字主色实心并占满剩余宽度，单行排列
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .textureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    blurRadius = 24f,
                )
                .background(AppThemeColors.surfaceContainer.copy(alpha = 0.6f))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { onEdit(billId) },
                enabled = !s.exporting,
                minHeight = 44.dp,
                minWidth = 44.dp,
                insideMargin = PaddingValues(10.dp),
                colors = ButtonDefaults.buttonColors(
                    color = Color.Transparent,
                    contentColor = MiuixTheme.colorScheme.primary,
                ),
            ) {
                Icon(MiuixIcons.Edit, contentDescription = "修改")
            }
            Button(
                onClick = { viewModel.exportToGallery() },
                enabled = !s.exporting,
                minHeight = 44.dp,
                minWidth = 44.dp,
                insideMargin = PaddingValues(10.dp),
                colors = ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.secondaryContainer,
                    contentColor = MiuixTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                if (s.exporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        size = 18.dp,
                    )
                } else {
                    Icon(MiuixIcons.Download, contentDescription = "保存图片")
                }
            }
            Button(
                onClick = { viewModel.shareNow() },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.weight(1f),
                enabled = !s.exporting,
                minHeight = 44.dp,
            ) {
                Icon(MiuixIcons.Share, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("分享图片", maxLines = 1)
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

    s.exportOutcome?.let { outcome ->
        OverlayDialog(
            title = if (outcome.saved) "保存成功" else "保存失败",
            summary = outcome.message,
            show = true,
            onDismissRequest = viewModel::consumeExportOutcome,
        ) {
            Spacer(Modifier.height(8.dp))
            if (outcome.saved && outcome.shareUri != null) {
                DialogButtons(
                    confirmText = "完成",
                    cancelText = "分享",
                    onCancel = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, outcome.shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享单据"))
                        viewModel.consumeExportOutcome()
                    },
                    onConfirm = viewModel::consumeExportOutcome,
                )
            } else {
                DialogButtons(
                    confirmText = "完成",
                    cancelText = null,
                    onConfirm = viewModel::consumeExportOutcome,
                )
            }
        }
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
            style = AppThemeTypography.bodyMedium,
            color = AppThemeColors.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            value,
            style = AppThemeTypography.bodyMedium,
            modifier = if (onValueClick != null) {
                Modifier.clickable { onValueClick() }
            } else {
                Modifier
            },
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
        val scope = rememberCoroutineScope()
        val haptics = LocalHaptics.current

        // 进入过渡：scale 0.92→1 + 淡入
        val enter = remember { Animatable(0f) }
        LaunchedEffect(Unit) { enter.animateTo(1f, tween(220)) }

        // 下拉关闭：跟手下坠+缩小+背景变透明；松手按位移阈值判定 spring 回弹或收束关闭
        val dragY = remember { Animatable(0f) }
        val dismissThresholdPx = 240f

        fun settle() {
            if (dragY.value > dismissThresholdPx) {
                haptics.tick()
                scope.launch {
                    dragY.animateTo(
                        targetValue = 1600f,
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                    )
                    onDismiss()
                }
            } else {
                scope.launch {
                    dragY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // 背景透明度随下拉进度联动（最低保留 0.25，避免瞬间全透明）
                .graphicsLayer { alpha = (1f - (dragY.value / 900f)).coerceIn(0.25f, 1f) }
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
                // 下拉进度：图片缩到 0.72、竖向位移按 0.55 阻尼跟手
                val dragScale = 1f - (dragY.value / 1400f).coerceIn(0f, 0.28f)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "单据预览",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val enterScale = 0.92f + 0.08f * enter.value
                            val s = scale * dragScale * enterScale
                            scaleX = s
                            scaleY = s
                            translationX = offset.x
                            translationY = offset.y + dragY.value * 0.55f
                        }
                        // 单一手势处理器：多指/已放大 → 缩放平移；单指未放大 → 下拉关闭。
                        // 不能与 detectTransformGestures 叠加——后者会先消费移动事件，导致下拉永远不触发。
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var multiTouch = false
                                var dragged = false
                                var slopY = 0f
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size > 1) multiTouch = true
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val pressed = event.changes.any { it.pressed }
                                    if (multiTouch || scale > 1.01f) {
                                        // 缩放/平移（放大态）；缩回原尺寸时归位
                                        scale = (scale * zoom).coerceIn(1f, 4f)
                                        offset += pan
                                        if (scale <= 1.01f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        }
                                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    } else {
                                        // 单指未放大：超过触摸阈值后竖向拖拽 → 下拉关闭；
                                        // 未超阈值不消费，保住外层长按菜单与点击关闭
                                        slopY += pan.y
                                        if (!dragged && abs(slopY) > viewConfiguration.touchSlop) dragged = true
                                        if (dragged && pan.y != 0f) {
                                            val target = (dragY.value + pan.y).coerceAtLeast(0f)
                                            scope.launch { dragY.snapTo(target) }
                                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                                        }
                                    }
                                    if (!pressed) break
                                }
                                if (!multiTouch && scale <= 1.01f && dragged) settle()
                            }
                        },
                )
                MiuixMenuPopup(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    items = listOf(
                        "保存图片" to {
                            showMenu = false
                            onExport()
                        },
                        "分享图片" to {
                            showMenu = false
                            onShare()
                        },
                    ),
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f)),
            ) {
                Icon(MiuixIcons.Basic.Close, contentDescription = "关闭", tint = Color.White)
            }
        }
    }
}
