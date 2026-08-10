package com.example.quickbillmate.ui.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LabeledSwitch
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
        defaultDocCode = viewModel.defaultDocCode,
        defaultTitleSuffix = viewModel.defaultTitleSuffix,
        defaultDisclaimer = viewModel.defaultDisclaimer,
        versionName = viewModel.versionName,
        presets = presets,
        onThemeModeChange = { mode ->
            viewModel.updateThemeMode(mode)
            onThemeModeChange(mode)
        },
        onCompanySave = { company, phone, manager ->
            viewModel.updateCompany(company)
            viewModel.updatePhone(phone)
            viewModel.updateManager(manager)
        },
        onPresetChange = viewModel::updateDefaultPreset,
        onShowOptionsSave = { manager, remark, watermark, multi ->
            viewModel.updateShowOptions(manager, remark, watermark, multi)
        },
        onBillDefaultsSave = { docCode, titleSuffix, disclaimer ->
            viewModel.updateBillDefaults(docCode, titleSuffix, disclaimer)
        },
        onManagePresets = onManagePresets,
        onOpenUrl = { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
    defaultDocCode: String,
    defaultTitleSuffix: String,
    defaultDisclaimer: String,
    versionName: String,
    presets: List<StylePreset>,
    onThemeModeChange: (String) -> Unit,
    onCompanySave: (String, String, String) -> Unit,
    onPresetChange: (String) -> Unit,
    onShowOptionsSave: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onBillDefaultsSave: (String, String, String) -> Unit,
    onManagePresets: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var showThemeMenu by remember { mutableStateOf(false) }
    var showPresetPanel by remember { mutableStateOf(false) }
    var showCompanyDialog by remember { mutableStateOf(false) }
    var showShowOptionsDialog by remember { mutableStateOf(false) }
    var showBillDefaultsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val themeLabel = when (themeMode) {
        SettingsStore.THEME_DARK -> "深色"
        SettingsStore.THEME_LIGHT -> "浅色"
        else -> "跟随系统"
    }

    Scaffold(
        topBar = { AppTopBar(title = "快贝智单") },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            // 深色模式（下拉）
            Box {
                SettingRow(
                    icon = { Text("☾", style = MaterialTheme.typography.titleMedium) },
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

            // 默认图片样式（展开选择默认样式，并可进入管理）
            SettingRow(
                icon = { Icon(Icons.Default.Build, contentDescription = null) },
                title = "默认图片样式",
                subtitle = presetDisplayName(defaultPresetKey, presets),
                onClick = { showPresetPanel = !showPresetPanel },
            )
            if (showPresetPanel) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
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
            }

            // 默认公司信息（弹窗）
            SettingRow(
                icon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                title = "默认公司信息",
                subtitle = listOf(defaultCompany, defaultPhone, defaultManager)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { "未设置" },
                onClick = { showCompanyDialog = true },
            )

            // 默认显示选项（弹窗）
            SettingRow(
                icon = { Text("☑", style = MaterialTheme.typography.titleMedium) },
                title = "默认显示选项",
                subtitle = listOf(
                    "业务经理 ${if (defaultShowManager) "开" else "关"}",
                    "备注 ${if (defaultShowRemark) "开" else "关"}",
                    "水印 ${if (defaultShowWatermark) "开" else "关"}",
                    "多电话 ${if (defaultShowMultiPhones) "开" else "关"}",
                ).joinToString(" · "),
                onClick = { showShowOptionsDialog = true },
            )

            // 默认客单信息（弹窗）
            SettingRow(
                icon = { Text("№", style = MaterialTheme.typography.titleMedium) },
                title = "默认客单信息",
                subtitle = listOf(defaultDocCode, defaultTitleSuffix)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { "未设置" },
                onClick = { showBillDefaultsDialog = true },
            )

            // 关于
            SettingRow(
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                title = "关于",
                subtitle = "版本 $versionName",
                onClick = { showAboutDialog = true },
            )
        }
    }

    if (showCompanyDialog) {
        CompanyDefaultsDialog(
            company = defaultCompany,
            phone = defaultPhone,
            manager = defaultManager,
            onSave = { company, phone, manager ->
                onCompanySave(company, phone, manager)
                showCompanyDialog = false
            },
            onDismiss = { showCompanyDialog = false },
        )
    }

    if (showShowOptionsDialog) {
        ShowOptionsDialog(
            showManager = defaultShowManager,
            showRemark = defaultShowRemark,
            showWatermark = defaultShowWatermark,
            showMultiPhones = defaultShowMultiPhones,
            onSave = { manager, remark, watermark, multi ->
                onShowOptionsSave(manager, remark, watermark, multi)
                showShowOptionsDialog = false
            },
            onDismiss = { showShowOptionsDialog = false },
        )
    }

    if (showBillDefaultsDialog) {
        BillDefaultsDialog(
            docCode = defaultDocCode,
            titleSuffix = defaultTitleSuffix,
            disclaimer = defaultDisclaimer,
            onSave = { docCode, titleSuffix, disclaimer ->
                onBillDefaultsSave(docCode, titleSuffix, disclaimer)
                showBillDefaultsDialog = false
            },
            onDismiss = { showBillDefaultsDialog = false },
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
}

@Composable
private fun ShowOptionsDialog(
    showManager: Boolean,
    showRemark: Boolean,
    showWatermark: Boolean,
    showMultiPhones: Boolean,
    onSave: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var manager by remember { mutableStateOf(showManager) }
    var remark by remember { mutableStateOf(showRemark) }
    var watermark by remember { mutableStateOf(showWatermark) }
    var multiPhones by remember { mutableStateOf(showMultiPhones) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("默认显示选项") },
        text = {
            Column {
                LabeledSwitch("显示业务经理", manager, { manager = it })
                LabeledSwitch("显示备注", remark, { remark = it })
                LabeledSwitch("显示水印", watermark, { watermark = it })
                LabeledSwitch("显示多个电话", multiPhones, { multiPhones = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(manager, remark, watermark, multiPhones) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun BillDefaultsDialog(
    docCode: String,
    titleSuffix: String,
    disclaimer: String,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var docCodeText by remember { mutableStateOf(docCode) }
    var titleSuffixText by remember { mutableStateOf(titleSuffix) }
    var disclaimerText by remember { mutableStateOf(disclaimer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("默认客单信息") },
        text = {
            Column {
                LabeledField("默认编号代码", docCodeText, { docCodeText = it })
                Spacer(Modifier.height(8.dp))
                LabeledField("默认标题后缀", titleSuffixText, { titleSuffixText = it })
                Spacer(Modifier.height(8.dp))
                LabeledField("默认底部说明文案", disclaimerText, { disclaimerText = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(docCodeText.trim(), titleSuffixText.trim(), disclaimerText.trim())
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
            .padding(horizontal = 14.dp, vertical = 8.dp),
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

@Composable
private fun SettingRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
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
}

@Composable
private fun CompanyDefaultsDialog(
    company: String,
    phone: String,
    manager: String,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var companyText by remember { mutableStateOf(company) }
    var phoneText by remember { mutableStateOf(phone) }
    var managerText by remember { mutableStateOf(manager) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("默认公司信息") },
        text = {
            Column {
                LabeledField("默认公司名称", companyText, { companyText = it })
                Spacer(Modifier.height(8.dp))
                LabeledField(
                    "默认联系电话",
                    phoneText,
                    { phoneText = it },
                    keyboardType = KeyboardType.Phone,
                )
                Spacer(Modifier.height(8.dp))
                LabeledField("默认业务经理", managerText, { managerText = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(companyText.trim(), phoneText.trim(), managerText.trim()) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
