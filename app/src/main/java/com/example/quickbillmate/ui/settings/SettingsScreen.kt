package com.example.quickbillmate.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.repository.SettingsStore
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.SectionCard
import com.example.quickbillmate.ui.editor.presetDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onThemeModeChange: (String) -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val presets by viewModel.presets.collectAsState()
    var showPresetPicker by remember { mutableStateOf(false) }

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
            SectionCard("深色模式") {
                listOf(
                    SettingsStore.THEME_SYSTEM to "跟随系统",
                    SettingsStore.THEME_LIGHT to "浅色",
                    SettingsStore.THEME_DARK to "深色",
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateThemeMode(mode)
                                onThemeModeChange(mode)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = viewModel.themeMode == mode,
                            onClick = {
                                viewModel.updateThemeMode(mode)
                                onThemeModeChange(mode)
                            },
                        )
                        Text(label)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("默认公司信息") {
                LabeledField("默认公司名称", viewModel.defaultCompany, viewModel::updateCompany, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LabeledField("默认联系电话", viewModel.defaultPhone, viewModel::updatePhone, Modifier.fillMaxWidth(), KeyboardType.Phone)
                Spacer(Modifier.height(8.dp))
                LabeledField("默认业务经理", viewModel.defaultManager, viewModel::updateManager, Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("默认样式预设") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPresetPicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        presetDisplayName(viewModel.defaultPresetKey, presets),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            Spacer(Modifier.height(10.dp))

            SectionCard("关于") {
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
            }
        }
    }

    if (showPresetPicker) {
        AlertDialog(
            onDismissRequest = { showPresetPicker = false },
            title = { Text("选择默认预设") },
            text = {
                Column {
                    StylePresets.builtIns.forEach { preset ->
                        PresetChoiceRow(
                            name = preset.name,
                            selected = viewModel.defaultPresetKey == preset.key,
                            onClick = {
                                viewModel.updateDefaultPreset(preset.key)
                                showPresetPicker = false
                            },
                        )
                    }
                    presets.forEach { preset ->
                        PresetChoiceRow(
                            name = preset.name,
                            selected = viewModel.defaultPresetKey == "custom:${preset.id}",
                            onClick = {
                                viewModel.updateDefaultPreset("custom:${preset.id}")
                                showPresetPicker = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetPicker = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun PresetChoiceRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(name)
    }
}
