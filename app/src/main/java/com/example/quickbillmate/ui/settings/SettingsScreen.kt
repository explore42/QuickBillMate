package com.example.quickbillmate.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import androidx.compose.foundation.layout.Arrangement
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.DialogButtons
import com.example.quickbillmate.ui.common.DefaultInfoForm
import com.example.quickbillmate.ui.common.DefaultInfoValues
import com.example.quickbillmate.ui.common.SmallTextButton
import com.example.quickbillmate.ui.editor.presetDisplayName
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.Ds
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.THEME_COLOR_WALLPAPER
import com.example.quickbillmate.ui.theme.ThemeColorPresets
import com.example.quickbillmate.util.CrashRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

private const val PROJECT_URL = "https://github.com/explore42/QuickBillMate"

private val ThemeOptions = listOf("跟随系统", "浅色", "深色")
private val ThemeModes = listOf(
    SettingsStore.THEME_SYSTEM,
    SettingsStore.THEME_LIGHT,
    SettingsStore.THEME_DARK,
)

private val ThemeColorLabels = listOf("跟随壁纸") + ThemeColorPresets.map { it.label }
private val ThemeColorArgbs = listOf(THEME_COLOR_WALLPAPER) + ThemeColorPresets.map { it.argb }

private fun themeColorIndex(argb: Long): Int =
    ThemeColorArgbs.indexOf(argb).coerceAtLeast(0)

@Composable
fun SettingsScreen(
    onThemeModeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeKeyColorChange: (Long) -> Unit,
    onManagePresets: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val presets by viewModel.presets.collectAsState()
    val context = LocalContext.current
    val qrPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.onQrImagePicked(uri)
    }

    SettingsContent(
        themeMode = viewModel.themeMode,
        dynamicColor = viewModel.dynamicColor,
        themeKeyColor = viewModel.themeKeyColor,
        defaultCompany = viewModel.defaultCompany,
        defaultPhone = viewModel.defaultPhone,
        defaultManager = viewModel.defaultManager,
        defaultPresetKey = viewModel.defaultPresetKey,
        defaultShowManager = viewModel.defaultShowManager,
        defaultShowRemark = viewModel.defaultShowRemark,
        defaultShowWatermark = viewModel.defaultShowWatermark,
        defaultShowMultiPhones = viewModel.defaultShowMultiPhones,
        defaultShowAd = viewModel.defaultShowAd,
        defaultRemark = viewModel.defaultRemark,
        defaultWatermarkText = viewModel.defaultWatermarkText,
        defaultShowContactPhone = viewModel.defaultShowContactPhone,
        defaultDocCode = viewModel.defaultDocCode,
        defaultTitleSuffix = viewModel.defaultTitleSuffix,
        defaultAdText = viewModel.defaultAdText,
        versionName = viewModel.versionName,
        presets = presets,
        qrBitmap = viewModel.qrBitmap,
        pendingCropBitmap = viewModel.pendingCrop,
        crashLogs = viewModel.crashLogs,
        onThemeModeChange = { mode ->
            viewModel.updateThemeMode(mode)
            onThemeModeChange(mode)
        },
        onDynamicColorChange = { enabled ->
            viewModel.updateDynamicColor(enabled)
            onDynamicColorChange(enabled)
        },
        onThemeKeyColorChange = { argb ->
            viewModel.updateThemeKeyColor(argb)
            onThemeKeyColorChange(argb)
        },
        onPresetChange = viewModel::updateDefaultPreset,
        onDefaultsSave = viewModel::updateDefaults,
        onManagePresets = onManagePresets,
        onPickQrImage = { qrPicker.launch(arrayOf("image/*")) },
        onRemoveQrImage = viewModel::removeQrImage,
        onCropSave = viewModel::saveQrImage,
        onCropCancel = viewModel::consumeCrop,
        onClearCrashLogs = viewModel::clearCrashLogs,
        onCopyCrashLogs = viewModel::copyCrashLogs,
        onOpenUrl = { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        },
    )
}

/** 设置页纯界面层：数据全部由参数传入，可在 Android Studio 中直接预览调试。 */
@Composable
fun SettingsContent(
    themeMode: String,
    dynamicColor: Boolean,
    themeKeyColor: Long,
    defaultCompany: String,
    defaultPhone: String,
    defaultManager: String,
    defaultPresetKey: String,
    defaultShowManager: Boolean,
    defaultShowRemark: Boolean,
    defaultShowWatermark: Boolean,
    defaultShowMultiPhones: Boolean,
    defaultShowAd: Boolean,
    defaultRemark: String,
    defaultWatermarkText: String,
    defaultShowContactPhone: Boolean,
    defaultDocCode: String,
    defaultTitleSuffix: String,
    defaultAdText: String,
    versionName: String,
    presets: List<StylePreset>,
    qrBitmap: Bitmap?,
    pendingCropBitmap: Bitmap?,
    crashLogs: List<CrashRecord>,
    onThemeModeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeKeyColorChange: (Long) -> Unit,
    onPresetChange: (String) -> Unit,
    onDefaultsSave: (DefaultInfoValues) -> Unit,
    onManagePresets: () -> Unit,
    onPickQrImage: () -> Unit,
    onRemoveQrImage: () -> Unit,
    onCropSave: (Bitmap) -> Unit,
    onCropCancel: () -> Unit,
    onClearCrashLogs: () -> Unit,
    onCopyCrashLogs: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var showDefaultsDialog by remember { mutableStateOf(false) }
    var showCrashLogsDialog by remember { mutableStateOf(false) }
    var showClearCrashConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val themeLabel = when (themeMode) {
        SettingsStore.THEME_DARK -> "深色"
        SettingsStore.THEME_LIGHT -> "浅色"
        else -> "跟随系统"
    }
    val themeIndex = when (themeMode) {
        SettingsStore.THEME_DARK -> 2
        SettingsStore.THEME_LIGHT -> 1
        else -> 0
    }

    Scaffold(
        topBar = { AppTopBar(title = "快贝智单", showLogo = true) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(Ds.md),
        ) {
            SettingsGroup {
                // 深色模式（Miuix 下拉选择）
                OverlayDropdownPreference(
                    title = "深色模式",
                    summary = themeLabel,
                    items = ThemeOptions,
                    selectedIndex = themeIndex,
                    onSelectedIndexChange = { index ->
                        onThemeModeChange(ThemeModes[index])
                    },
                    startAction = { SettingsIcon(MiuixIcons.Theme) },
                )
                // 动态取色（Monet）：默认开启，可用壁纸或预置种子色
                SwitchPreference(
                    title = "动态取色",
                    summary = if (dynamicColor) "根据壁纸或主题色生成配色" else "使用默认配色",
                    checked = dynamicColor,
                    onCheckedChange = onDynamicColorChange,
                    startAction = { SettingsIcon(MiuixIcons.Layers) },
                )
                if (dynamicColor) {
                    OverlayDropdownPreference(
                        title = "主题色",
                        summary = ThemeColorLabels[themeColorIndex(themeKeyColor)],
                        items = ThemeColorLabels,
                        selectedIndex = themeColorIndex(themeKeyColor),
                        onSelectedIndexChange = { index ->
                            onThemeKeyColorChange(ThemeColorArgbs[index])
                        },
                        startAction = { SettingsIcon(MiuixIcons.Theme) },
                    )
                }
            }

            SettingsGroup {
                // 默认图片样式（下拉选择；管理入口独立成行）
                val presetNames = StylePresets.builtIns.map { it.name } + presets.map { it.name }
                val presetKeys = StylePresets.builtIns.map { it.key } + presets.map { "custom:${it.id}" }
                val presetIndex = presetKeys.indexOf(defaultPresetKey).coerceAtLeast(0)
                OverlayDropdownPreference(
                    title = "默认图片样式",
                    summary = presetDisplayName(defaultPresetKey, presets),
                    items = presetNames,
                    selectedIndex = presetIndex,
                    onSelectedIndexChange = { index ->
                        onPresetChange(presetKeys[index])
                    },
                    startAction = { SettingsIcon(MiuixIcons.Tune) },
                )
                ArrowPreference(
                    title = "图片样式管理",
                    summary = "内置与自定义预设管理",
                    startAction = { SettingsIcon(MiuixIcons.Tune) },
                    onClick = onManagePresets,
                )
                // 默认信息（弹窗）
                ArrowPreference(
                    title = "默认信息",
                    summary = listOf(
                        defaultCompany,
                        defaultManager,
                        defaultPhone,
                    ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "未设置" },
                    startAction = { SettingsIcon(MiuixIcons.Store) },
                    onClick = { showDefaultsDialog = true },
                )
            }

            SettingsGroup {
                // 崩溃日志（本地，无联网）
                ArrowPreference(
                    title = "崩溃日志",
                    summary = if (crashLogs.isEmpty()) "无" else "最近 ${crashLogs.size} 条",
                    startAction = { SettingsIcon(MiuixIcons.Report) },
                    onClick = { showCrashLogsDialog = true },
                )
                // 关于
                ArrowPreference(
                    title = "关于",
                    summary = "版本 $versionName",
                    startAction = { SettingsIcon(MiuixIcons.Info) },
                    onClick = { showAboutDialog = true },
                )
            }
        }
    }

    if (showDefaultsDialog) {
        DefaultInfoDialog(
            values = DefaultInfoValues(
                titleSuffix = defaultTitleSuffix,
                docCode = defaultDocCode,
                showMultiPhones = defaultShowMultiPhones,
                companyName = defaultCompany,
                manager = defaultManager,
                showManager = defaultShowManager,
                contactPhone = defaultPhone,
                showContactPhone = defaultShowContactPhone,
                showRemark = defaultShowRemark,
                showAd = defaultShowAd,
                remark = defaultRemark,
                adText = defaultAdText,
                watermarkText = defaultWatermarkText,
                showWatermark = defaultShowWatermark,
            ),
            qrBitmap = qrBitmap,
            onPickQrImage = onPickQrImage,
            onRemoveQrImage = onRemoveQrImage,
            onSave = {
                onDefaultsSave(it)
                showDefaultsDialog = false
            },
            onDismiss = { showDefaultsDialog = false },
        )
    }

    if (showAboutDialog) {
        OverlayDialog(
            title = "关于",
            summary = "快贝智单 QuickBillMate",
            show = true,
            onDismissRequest = { showAboutDialog = false },
        ) {
            Column {
                Text(
                    "版本 $versionName",
                    style = AppThemeTypography.bodySmall,
                    color = AppThemeColors.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "开源协议：Apache License 2.0",
                    style = AppThemeTypography.bodySmall,
                    color = AppThemeColors.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text("开源地址：", style = AppThemeTypography.bodySmall)
                TextButton(
                    text = PROJECT_URL,
                    onClick = { onOpenUrl(PROJECT_URL) },
                )
                Spacer(Modifier.height(8.dp))
                DialogButtons(
                    confirmText = "关闭",
                    cancelText = null,
                    onConfirm = { showAboutDialog = false },
                )
            }
        }
    }

    if (showCrashLogsDialog) {
        OverlayDialog(
            title = "崩溃日志",
            show = true,
            onDismissRequest = { showCrashLogsDialog = false },
        ) {
            if (crashLogs.isEmpty()) {
                Text("暂无崩溃记录")
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    crashLogs.forEachIndexed { index, record ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                record.timeMillis.toCrashTimeText(),
                                style = AppThemeTypography.labelSmall,
                                color = AppThemeColors.onSurfaceVariant,
                            )
                            Text(record.summary, style = AppThemeTypography.bodySmall)
                        }
                        if (index != crashLogs.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (crashLogs.isNotEmpty()) {
                            SmallTextButton(
                                text = "复制全部",
                                onClick = onCopyCrashLogs,
                            )
                            SmallTextButton(
                                text = "清除",
                                onClick = { showClearCrashConfirm = true },
                            )
                        }
                        DialogButtons(
                            confirmText = "关闭",
                            cancelText = null,
                            onConfirm = { showCrashLogsDialog = false },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (showClearCrashConfirm) {
        ConfirmDialog(
            title = "清除崩溃日志",
            text = "确定清除全部 ${crashLogs.size} 条崩溃日志吗？",
            confirmText = "确认清除",
            destructive = true,
            onConfirm = {
                onClearCrashLogs()
                showClearCrashConfirm = false
            },
            onDismiss = { showClearCrashConfirm = false },
        )
    }

    pendingCropBitmap?.let { bitmap ->
        QrCropDialog(
            bitmap = bitmap,
            onCancel = onCropCancel,
            onSave = onCropSave,
        )
    }
}

/** 毫秒时间戳 → 本地时间文本（崩溃日志弹窗展示）。 */
private fun Long.toCrashTimeText(): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(this))

/** 设置分组：仅卡片容器（无小标题），卡片间距由外层 spacedBy 提供。 */
@Composable
private fun SettingsGroup(
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Ds.screen),
        colors = CardDefaults.defaultColors(color = AppThemeColors.surfaceContainer),
    ) {
        Column { content() }
    }
}

/** 设置项统一起始图标。 */
@Composable
private fun SettingsIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = AppThemeColors.onBackground,
        modifier = Modifier.padding(end = Ds.md),
    )
}

@Composable
private fun DefaultInfoDialog(
    values: DefaultInfoValues,
    qrBitmap: Bitmap?,
    onPickQrImage: () -> Unit,
    onRemoveQrImage: () -> Unit,
    onSave: (DefaultInfoValues) -> Unit,
    onDismiss: () -> Unit,
) {
    var local by remember(values) { mutableStateOf(values) }

    OverlayDialog(
        title = "默认信息",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            DefaultInfoForm(values = local, onChange = { local = it })

            Spacer(Modifier.height(14.dp))
            Text(
                text = "微信二维码",
                style = AppThemeTypography.titleSmall,
                color = AppThemeColors.primary,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppThemeColors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "微信二维码预览",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            "未设置",
                            style = AppThemeTypography.bodySmall,
                            color = AppThemeColors.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    TextButton(
                        text = if (qrBitmap == null) "上传图片" else "更换图片",
                        onClick = onPickQrImage,
                    )
                    if (qrBitmap != null) {
                        TextButton(
                            text = "移除",
                            onClick = onRemoveQrImage,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "上传微信收款码后，会先裁剪为正方形再显示在所有单据左上角。",
                style = AppThemeTypography.bodySmall,
                color = AppThemeColors.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            DialogButtons(
                confirmText = "保存",
                onCancel = onDismiss,
                onConfirm = {
                    onSave(
                        local.copy(
                            titleSuffix = local.titleSuffix.trim(),
                            docCode = local.docCode.trim(),
                            companyName = local.companyName.trim(),
                            manager = local.manager.trim(),
                            contactPhone = local.contactPhone.trim(),
                            adText = local.adText.trim(),
                        )
                    )
                },
            )
        }
    }
}
