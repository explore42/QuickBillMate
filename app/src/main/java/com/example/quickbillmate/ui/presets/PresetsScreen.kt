package com.example.quickbillmate.ui.presets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.MiuixMenuPopup
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More

@Composable
fun PresetsScreen(
    onBack: () -> Unit,
    onNewPreset: () -> Unit,
    onEditPreset: (Long) -> Unit,
    viewModel: PresetsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val presets by viewModel.presets.collectAsState()
    var menuFor by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<StylePreset?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "图片样式",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewPreset) {
                Icon(MiuixIcons.Add, contentDescription = "新建图片样式")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("内置预设", style = AppThemeTypography.titleSmall, color = AppThemeColors.primary)
            }
            items(StylePresets.builtIns, key = { it.key }) { preset ->
                PresetCard(
                    name = preset.name,
                    tag = "内置",
                    isDefault = viewModel.defaultKey == preset.key,
                    preview = viewModel.previews[preset.key],
                    onMenuClick = { menuFor = preset.key },
                    menuExpanded = menuFor == preset.key,
                    onMenuDismiss = { menuFor = null },
                    menuItems = listOf(
                        "复制" to {
                            viewModel.duplicatePreset(preset.key, presets)
                            menuFor = null
                        },
                    ),
                )
            }

            if (presets.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("我的预设", style = AppThemeTypography.titleSmall, color = AppThemeColors.primary)
                }
                items(presets, key = { it.id }) { preset ->
                    val key = "custom:${preset.id}"
                    PresetCard(
                        name = preset.name,
                        tag = "自定义",
                        isDefault = viewModel.defaultKey == key,
                        preview = viewModel.previews[key],
                        onMenuClick = { menuFor = key },
                        menuExpanded = menuFor == key,
                        onMenuDismiss = { menuFor = null },
                        menuItems = listOf(
                            "复制" to {
                                viewModel.duplicatePreset(key, presets)
                                menuFor = null
                            },
                            "编辑" to {
                                menuFor = null
                                onEditPreset(preset.id)
                            },
                            "删除" to {
                                pendingDelete = preset
                                menuFor = null
                            },
                        ),
                    )
                }
            }
        }
    }

    pendingDelete?.let { preset ->
        ConfirmDialog(
            title = "删除预设",
            text = "确定删除预设“${preset.name}”吗？引用它的单据将回退到“经典单据”。",
            onConfirm = {
                viewModel.deletePreset(preset)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun PresetCard(
    name: String,
    tag: String,
    isDefault: Boolean,
    preview: android.graphics.Bitmap?,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    menuItems: List<Pair<String, () -> Unit>>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            preview?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .width(90.dp)
                        .height(64.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = AppThemeTypography.titleSmall)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        tag,
                        style = AppThemeTypography.labelSmall,
                        color = AppThemeColors.onSurfaceVariant,
                    )
                }
                if (isDefault) {
                    Text(
                        "当前默认",
                        style = AppThemeTypography.bodySmall,
                        color = AppThemeColors.primary,
                    )
                }
            }
            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(MiuixIcons.More, contentDescription = "更多")
                }
                MiuixMenuPopup(
                    expanded = menuExpanded,
                    onDismiss = onMenuDismiss,
                    items = menuItems,
                )
            }
        }
    }
}
