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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
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
import androidx.compose.ui.state.ToggleableState
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
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

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
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.importSelected() },
                        enabled = viewModel.selected.isNotEmpty() && !viewModel.importing,
                        minHeight = 32.dp,
                        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        if (viewModel.importing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                size = 16.dp,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
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
                EmptyState(MiuixIcons.Basic.Search, "通讯录中没有有电话号码的联系人")
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
                        state = if (importableFiltered.isNotEmpty() && importableFiltered.all {
                                keyOf(it) in viewModel.selected
                            }) ToggleableState.On else ToggleableState.Off,
                        onClick = {
                            val allSelected = importableFiltered.all { keyOf(it) in viewModel.selected }
                            viewModel.toggleAll(viewModel.filtered, !allSelected)
                        },
                    )
                    Text("全选（仅当前筛选结果）", style = AppThemeTypography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "已选中 ${viewModel.selected.size} 项",
                        style = AppThemeTypography.bodySmall,
                        color = AppThemeColors.primary,
                    )
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .overScrollVertical()
                            .scrollEndHaptic(),
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
        OverlayDialog(
            title = "需要通讯录权限",
            summary = "用于从通讯录选择联系人导入客户库。您也可以稍后在系统设置中开启。",
            show = true,
            onDismissRequest = { permissionDenied = false },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = "取消",
                    onClick = {
                        permissionDenied = false
                        onBack()
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "去设置",
                    onClick = {
                        permissionDenied = false
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }

    viewModel.importResult?.let { outcome ->
        val parts = buildList {
            add("新增 ${outcome.inserted} 条")
            if (outcome.merged > 0) add("合并 ${outcome.merged} 条")
        }
        OverlayDialog(
            title = "导入完成",
            summary = parts.joinToString(" / "),
            show = true,
            onDismissRequest = viewModel::consumeResult,
        ) {
            TextButton(
                text = "返回客户",
                onClick = {
                    viewModel.consumeResult()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
private fun ImportRow(
    candidate: ContactsImporter.Candidate,
    imported: Boolean,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val grey = AppThemeColors.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            state = if (checked) ToggleableState.On else ToggleableState.Off,
            onClick = { onToggle() },
            enabled = !imported,
        )
        Spacer(Modifier.width(8.dp))
        InitialCircle(candidate.name.trim().firstOrNull()?.toString() ?: "?")
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                candidate.name,
                style = AppThemeTypography.bodyLarge,
                color = if (imported) grey else AppThemeColors.onSurface,
            )
            Text(
                candidate.phone,
                style = AppThemeTypography.bodySmall,
                color = if (imported) grey else AppThemeColors.onSurfaceVariant,
            )
        }
    }
}

private fun keyOf(candidate: ContactsImporter.Candidate): String = "${candidate.name}\u0000${candidate.phone}"
