package com.example.quickbillmate.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.quickbillmate.ui.common.DefaultInfoForm
import com.example.quickbillmate.ui.common.DefaultInfoValues
import com.example.quickbillmate.ui.editor.presetDisplayName

private const val PROJECT_URL = "https://github.com/explore42/QuickBillMate"

@Composable
fun SettingsScreen(
    onThemeModeChange: (String) -> Unit,
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
        onThemeModeChange = { mode ->
            viewModel.updateThemeMode(mode)
            onThemeModeChange(mode)
        },
        onPresetChange = viewModel::updateDefaultPreset,
        onDefaultsSave = viewModel::updateDefaults,
        onManagePresets = onManagePresets,
        onPickQrImage = { qrPicker.launch(arrayOf("image/*")) },
        onRemoveQrImage = viewModel::removeQrImage,
        onCropSave = viewModel::saveQrImage,
        onCropCancel = viewModel::consumeCrop,
        onOpenUrl = { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        },
    )
}

/** 设置页纯界面层：数据全部由参数传入，可在 Android Studio 中直接预览调试。 */
@Composable
fun SettingsContent(
    themeMode: String,
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
    onThemeModeChange: (String) -> Unit,
    onPresetChange: (String) -> Unit,
    onDefaultsSave: (DefaultInfoValues) -> Unit,
    onManagePresets: () -> Unit,
    onPickQrImage: () -> Unit,
    onRemoveQrImage: () -> Unit,
    onCropSave: (Bitmap) -> Unit,
    onCropCancel: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var showThemeMenu by remember { mutableStateOf(false) }
    var showPresetPanel by remember { mutableStateOf(false) }
    var showDefaultsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val themeLabel = when (themeMode) {
        SettingsStore.THEME_DARK -> "深色"
        SettingsStore.THEME_LIGHT -> "浅色"
        else -> "跟随系统"
    }

    Scaffold(
        topBar = { AppTopBar(title = "快贝智单", showLogo = true) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp),
        ) {
            // 深色模式（下拉）
            Box {
                SettingRow(
                    icon = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                    title = "深色模式",
                    subtitle = themeLabel,
                    onClick = { showThemeMenu = true },
                )
                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false },
                ) {
                    listOf(
                        SettingsStore.THEME_SYSTEM to "跟随系统",
                        SettingsStore.THEME_LIGHT to "浅色",
                        SettingsStore.THEME_DARK to "深色",
                    ).forEach { (mode, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onThemeModeChange(mode)
                                showThemeMenu = false
                            },
                        )
                    }
                }
            }
            HorizontalDivider()

            // 默认图片样式（展开选择默认样式，并可进入管理）
            SettingRow(
                icon = { Icon(Icons.Default.Build, contentDescription = null) },
                title = "默认图片样式",
                subtitle = presetDisplayName(defaultPresetKey, presets),
                onClick = { showPresetPanel = !showPresetPanel },
            )
            if (showPresetPanel) {
                HorizontalDivider()
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                    StylePresets.builtIns.forEach { preset ->
                        PresetSelectRow(
                            name = preset.name,
                            selected = defaultPresetKey == preset.key,
                            onClick = {
                                onPresetChange(preset.key)
                                showPresetPanel = false
                            },
                        )
                    }
                    presets.forEach { preset ->
                        PresetSelectRow(
                            name = preset.name,
                            selected = defaultPresetKey == "custom:${preset.id}",
                            onClick = {
                                onPresetChange("custom:${preset.id}")
                                showPresetPanel = false
                            },
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    TextButton(
                        onClick = {
                            showPresetPanel = false
                            onManagePresets()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("图片样式管理")
                    }
                }
            }
            HorizontalDivider()

            // 默认信息（弹窗）
            SettingRow(
                icon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                title = "默认信息",
                subtitle = listOf(
                    defaultCompany,
                    defaultManager,
                    defaultPhone,
                ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "未设置" },
                onClick = { showDefaultsDialog = true },
            )
            HorizontalDivider()

            // 关于
            SettingRow(
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                title = "关于",
                subtitle = "版本 $versionName",
                onClick = { showAboutDialog = true },
            )
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
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于") },
            text = {
                Column {
                    Text("快贝智单 QuickBillMate")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "版本 $versionName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "开源协议：Apache License 2.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("开源地址：", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { onOpenUrl(PROJECT_URL) }) {
                        Text(PROJECT_URL, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("关闭") }
            },
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("默认信息") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DefaultInfoForm(values = local, onChange = { local = it })

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "微信二维码",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        OutlinedButton(onClick = onPickQrImage) {
                            Text(if (qrBitmap == null) "上传图片" else "更换图片")
                        }
                        if (qrBitmap != null) {
                            TextButton(onClick = onRemoveQrImage) { Text("移除") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "上传微信收款码后，会先裁剪为正方形再显示在所有单据左上角。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
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
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun PresetSelectRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(name, modifier = Modifier.weight(1f))
        if (selected) {
            Text(
                "当前默认",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 无缝列表行：无卡片背景与圆角，行间由列表外层分割线分隔。 */
@Composable
private fun SettingRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}
