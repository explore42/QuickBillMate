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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.editor.presetDisplayName

private const val PROJECT_URL = "https://github.com/explore42/QuickBillMate"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onThemeModeChange: (String) -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val presets by viewModel.presets.collectAsState()
    val context = LocalContext.current
    var showThemeMenu by remember { mutableStateOf(false) }
    var showPresetMenu by remember { mutableStateOf(false) }
    var showCompanyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val themeLabel = when (viewModel.themeMode) {
        SettingsStore.THEME_DARK -> "深色"
        SettingsStore.THEME_LIGHT -> "浅色"
        else -> "跟随系统"
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
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
                                viewModel.updateThemeMode(mode)
                                onThemeModeChange(mode)
                                showThemeMenu = false
                            },
                        )
                    }
                }
            }

            // 默认样式预设（下拉）
            Box {
                SettingRow(
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    title = "默认样式预设",
                    subtitle = presetDisplayName(viewModel.defaultPresetKey, presets),
                    onClick = { showPresetMenu = true },
                )
                DropdownMenu(
                    expanded = showPresetMenu,
                    onDismissRequest = { showPresetMenu = false },
                ) {
                    StylePresets.builtIns.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.name) },
                            onClick = {
                                viewModel.updateDefaultPreset(preset.key)
                                showPresetMenu = false
                            },
                        )
                    }
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.name) },
                            onClick = {
                                viewModel.updateDefaultPreset("custom:${preset.id}")
                                showPresetMenu = false
                            },
                        )
                    }
                }
            }

            // 默认公司信息（弹窗）
            SettingRow(
                icon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                title = "默认公司信息",
                subtitle = listOf(viewModel.defaultCompany, viewModel.defaultPhone, viewModel.defaultManager)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { "未设置" },
                onClick = { showCompanyDialog = true },
            )

            // 关于
            SettingRow(
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                title = "关于",
                subtitle = "版本 ${viewModel.versionName}",
                onClick = { showAboutDialog = true },
            )
        }
    }

    if (showCompanyDialog) {
        CompanyDefaultsDialog(
            company = viewModel.defaultCompany,
            phone = viewModel.defaultPhone,
            manager = viewModel.defaultManager,
            onSave = { company, phone, manager ->
                viewModel.updateCompany(company)
                viewModel.updatePhone(phone)
                viewModel.updateManager(manager)
                showCompanyDialog = false
            },
            onDismiss = { showCompanyDialog = false },
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
                        "版本 ${viewModel.versionName}",
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
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL))
                        )
                    }) {
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
