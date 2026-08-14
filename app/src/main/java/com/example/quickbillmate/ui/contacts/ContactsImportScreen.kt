package com.example.quickbillmate.ui.contacts

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.importexport.ContactsImporter
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.CompactSearchField
import com.example.quickbillmate.ui.common.EmptyState
import com.example.quickbillmate.ui.common.GroupSectionHeader
import com.example.quickbillmate.ui.common.InitialCircle
import com.example.quickbillmate.ui.common.IndexSection
import com.example.quickbillmate.ui.common.LetterIndexBar

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

    val sections = viewModel.sections()
    val firstBySection = remember(sections) {
        buildMap {
            var index = 0
            sections.forEach { section ->
                put(section.key, index)
                index += 1 + section.items.size
            }
        }
    }
    val indexSections = remember(sections) {
        sections.map { section ->
            if (section.imported) IndexSection("✓", "已导入") else IndexSection(section.key, section.key)
        }
    }
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "从通讯录导入",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                val importableFiltered = viewModel.filtered.filterNot { viewModel.isImported(it) }
                val searchFocusRequester = remember { FocusRequester() }
                CompactSearchField(
                    query = viewModel.query,
                    placeholder = "搜索姓名/号码",
                    onQueryChange = viewModel::onQueryChange,
                    focusRequester = searchFocusRequester,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = importableFiltered.isNotEmpty() && importableFiltered.all {
                            keyOf(it) in viewModel.selected
                        },
                        onCheckedChange = { checked ->
                            viewModel.toggleAll(viewModel.filtered, checked)
                        },
                    )
                    Text("全选（仅当前筛选结果）", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "已选中 ${viewModel.selected.size} 项",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        sections.forEachIndexed { groupIndex, section ->
                            item(key = "header_${section.key}", contentType = { "sectionHeader" }) {
                                GroupSectionHeader(
                                    title = section.title,
                                    showTopDivider = groupIndex > 0,
                                )
                            }
                            items(section.items, key = { keyOf(it) }) { candidate ->
                                ImportRow(
                                    candidate = candidate,
                                    imported = section.imported,
                                    checked = keyOf(candidate) in viewModel.selected,
                                    onToggle = { viewModel.toggle(keyOf(candidate), keyOf(candidate) !in viewModel.selected) },
                                )
                            }
                        }
                    }
                    LetterIndexBar(
                        state = listState,
                        sections = indexSections,
                        firstIndexOf = { firstBySection[it] ?: -1 },
                    )
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

    viewModel.importResult?.let { outcome ->
        val parts = buildList {
            add("新增 ${outcome.inserted} 条")
            if (outcome.merged > 0) add("合并 ${outcome.merged} 条")
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

@Composable
private fun ImportRow(
    candidate: ContactsImporter.Candidate,
    imported: Boolean,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val grey = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = !imported)
        Spacer(Modifier.width(8.dp))
        InitialCircle(candidate.name.trim().firstOrNull()?.toString() ?: "?")
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                candidate.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (imported) grey else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                candidate.phone,
                style = MaterialTheme.typography.bodySmall,
                color = if (imported) grey else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun keyOf(candidate: ContactsImporter.Candidate): String = "${candidate.name}\u0000${candidate.phone}"
