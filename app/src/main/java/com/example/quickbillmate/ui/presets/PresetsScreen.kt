package com.example.quickbillmate.ui.presets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.quickbillmate.ui.common.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsScreen(
    onBack: () -> Unit,
    onNewPreset: () -> Unit,
    onEditPreset: (Long) -> Unit,
    viewModel: PresetsViewModel = viewModel(),
) {
    val presets by viewModel.presets.collectAsState()
    var menuFor by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<StylePreset?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("样式预设") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("点击卡片可设为默认预设", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onNewPreset, modifier = Modifier.fillMaxWidth()) {
                    Text("新建预设")
                }
            }

            item {
                Text("内置预设", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            items(StylePresets.builtIns, key = { it.key }) { preset ->
                PresetCard(
                    name = preset.name,
                    tag = "内置",
                    isDefault = viewModel.defaultKey == preset.key,
                    preview = viewModel.previews[preset.key],
                    onClick = {
                        viewModel.setDefault(preset.key)
                    },
                    onMenuClick = { menuFor = preset.key },
                )
            }

            if (presets.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("我的预设", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                items(presets, key = { it.id }) { preset ->
                    val key = "custom:${preset.id}"
                    PresetCard(
                        name = preset.name,
                        tag = "自定义",
                        isDefault = viewModel.defaultKey == key,
                        preview = viewModel.previews[key],
                        onClick = { viewModel.setDefault(key) },
                        onMenuClick = { menuFor = key },
                    )
                }
            }
        }
    }

    menuFor?.let { key ->
        val customId = key.removePrefix("custom:").toLongOrNull()
        val custom = customId?.let { id -> presets.firstOrNull { it.id == id } }
        DropdownMenu(
            expanded = true,
            onDismissRequest = { menuFor = null },
        ) {
            DropdownMenuItem(
                text = { Text("复制") },
                onClick = {
                    viewModel.duplicatePreset(key, presets)
                    menuFor = null
                },
            )
            if (custom != null) {
                DropdownMenuItem(
                    text = { Text("编辑") },
                    onClick = {
                        menuFor = null
                        onEditPreset(custom.id)
                    },
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = {
                        pendingDelete = custom
                        menuFor = null
                    },
                )
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
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
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
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (isDefault) "当前默认" else "点击设为默认",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
        }
    }
}
