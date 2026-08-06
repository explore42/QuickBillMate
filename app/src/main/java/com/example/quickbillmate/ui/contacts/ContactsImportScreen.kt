package com.example.quickbillmate.ui.contacts

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsImportScreen(
    onBack: () -> Unit,
    viewModel: ContactsImportViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.load()
        } else {
            permissionDenied = true
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("从通讯录导入") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.importSelected() },
                        enabled = viewModel.selected.isNotEmpty() && !viewModel.importing,
                    ) {
                        Text(if (viewModel.importing) "导入中…" else "导入")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (viewModel.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.candidates.isEmpty()) {
                EmptyState(Icons.Default.Search, "通讯录中没有有电话号码的联系人")
            } else {
                val filtered = viewModel.filtered
                OutlinedTextField(
                    value = viewModel.query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("搜索姓名/号码") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = filtered.isNotEmpty() && filtered.all {
                            "${it.name}\u0000${it.phone}" in viewModel.selected
                        },
                        onCheckedChange = { checked ->
                            viewModel.toggleAll(filtered, checked)
                        },
                    )
                    Text("全选（仅当前筛选结果）", style = MaterialTheme.typography.bodyMedium)
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.name + it.phone }) { candidate ->
                        val key = "${candidate.name}\u0000${candidate.phone}"
                        val checked = key in viewModel.selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggle(key, !checked) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { viewModel.toggle(key, it) })
                            Column(modifier = Modifier.weight(1f)) {
                                Text(candidate.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    candidate.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (permissionDenied) {
        AlertDialog(
            onDismissRequest = { permissionDenied = false },
            title = { Text("需要通讯录权限") },
            text = { Text("用于从通讯录选择联系人导入客户库。您也可以稍后在系统设置中开启。") },
            confirmButton = {
                TextButton(onClick = {
                    permissionDenied = false
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                    )
                }) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = {
                    permissionDenied = false
                    onBack()
                }) { Text("取消") }
            },
        )
    }

    viewModel.pendingMerge?.let { pending ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelMerge() },
            title = { Text("发现同名客户") },
            text = {
                Text("选中的联系人中有 ${viewModel.pendingMergeConflictCount} 条与客户库中已有客户同名。是否合并到现有客户（追加不同号码）？")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.importWithMerge(mergeSameName = true) }) { Text("合并") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.importWithMerge(mergeSameName = false) }) { Text("跳过同名") }
            },
        )
    }

    viewModel.importResult?.let { outcome ->
        val parts = buildList {
            add("成功 ${outcome.inserted} 条")
            if (outcome.merged > 0) add("合并 ${outcome.merged} 条")
            if (outcome.skipped > 0) add("已存在跳过 ${outcome.skipped} 条")
        }
        AlertDialog(
            onDismissRequest = viewModel::consumeResult,
            title = { Text("导入完成") },
            text = { Text(parts.joinToString(" / ")) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.consumeResult()
                    onBack()
                }) { Text("返回客户") }
            },
        )
    }
}
