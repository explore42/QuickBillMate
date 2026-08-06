package com.example.quickbillmate.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.EmptyState
import com.example.quickbillmate.ui.common.SearchableTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewBill: () -> Unit,
    onOpenBill: (Long) -> Unit,
    onSelectionModeChange: (Boolean) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val bills by viewModel.bills.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.selectionMode) {
        onSelectionModeChange(viewModel.selectionMode)
    }

    val copyMessage = viewModel.copyMessage
    LaunchedEffect(copyMessage) {
        copyMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeCopyMessage()
        }
    }

    val exportMessage = viewModel.exportMessage
    LaunchedEffect(exportMessage) {
        exportMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeExportMessage()
        }
    }

    HomeContent(
        bills = bills,
        selectionMode = viewModel.selectionMode,
        selectedIds = viewModel.selectedIds,
        searchQuery = viewModel.searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onNewBill = onNewBill,
        onOpenBill = onOpenBill,
        onEnterSelection = viewModel::enterSelection,
        onToggleSelection = viewModel::toggleSelection,
        onExitSelection = viewModel::exitSelection,
        onSelectAll = viewModel::selectAll,
        onCopy = viewModel::copySelected,
        onEdit = { viewModel.editSelected(onOpenBill) },
        onExport = viewModel::exportSelected,
        onDeleteRequest = {
            viewModel.requestDelete()
            showDeleteConfirm = true
        },
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        showDeleteConfirm = showDeleteConfirm,
        onDismissDeleteConfirm = { showDeleteConfirm = false },
    )
}

/** 首页纯界面层：数据与回调全部由参数传入，可在 Android Studio 中直接预览调试。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    bills: List<HomeBill>,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNewBill: () -> Unit,
    onOpenBill: (Long) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onEdit: (Long) -> Unit,
    onExport: () -> Unit,
    onDeleteRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    showDeleteConfirm: Boolean,
    onDismissDeleteConfirm: () -> Unit,
) {
    // 多选状态下，系统返回手势/按钮改为退出多选，而不是退出应用
    BackHandler(enabled = selectionMode) {
        onExitSelection()
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                AppTopBar(
                    title = "已选中 ${selectedIds.size} 项",
                    navigationIcon = {
                        IconButton(onClick = onExitSelection) {
                            Icon(Icons.Default.Close, contentDescription = "退出多选")
                        }
                    },
                    actions = {
                        TextButton(onClick = onSelectAll) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(2.dp))
                            Text("全选")
                        }
                    },
                )
            } else {
                SearchableTopBar(
                    title = "快贝智单",
                    searchPlaceholder = "搜索商品 / 客户 / 时间",
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = onNewBill,
                    modifier = Modifier.testTag("home_new_bill"),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "新建销售清单",
                    )
                }
            }
        },
        bottomBar = {
            if (selectionMode) {
                SelectionActionBar(
                    canEdit = selectedIds.size == 1,
                    onCopy = onCopy,
                    onEdit = { onEdit(selectedIds.firstOrNull() ?: 0L) },
                    onExport = onExport,
                    onDelete = onDeleteRequest,
                )
            }
        },
    ) { padding ->
        if (bills.isEmpty()) {
            EmptyState(
                icon = Icons.Default.ShoppingCart,
                text = if (searchQuery.isNotBlank()) "没有找到匹配的单据" else "还没有单据，点击右下角新建",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(bills, key = { it.bill.id }, contentType = { "billCard" }) { homeBill ->
                    val bill = homeBill.bill
                    val selected = bill.id in selectedIds
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        if (selectionMode) {
                                            onToggleSelection(bill.id)
                                        } else {
                                            onOpenBill(bill.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (selectionMode) {
                                            onToggleSelection(bill.id)
                                        } else {
                                            onEnterSelection(bill.id)
                                        }
                                    },
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = bill.customerName.ifBlank { "未填写客户" },
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = bill.docDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${homeBill.docNumber} · ${homeBill.itemCount} 项 · ¥${homeBill.receivableText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (selectionMode) {
                                Spacer(Modifier.width(8.dp))
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { onToggleSelection(bill.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除单据",
            text = "确定删除选中的 ${selectedIds.size} 条单据及其商品行吗？此操作不可恢复。",
            onConfirm = {
                onConfirmDelete()
                onDismissDeleteConfirm()
            },
            onDismiss = {
                onCancelDelete()
                onDismissDeleteConfirm()
            },
        )
    }
}

@Composable
private fun SelectionActionBar(
    canEdit: Boolean,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionAction(Icons.Default.Add, "复制", true, onCopy)
        SelectionAction(Icons.Default.Edit, "编辑", canEdit, onEdit)
        SelectionAction(Icons.Default.ShoppingCart, "导出", true, onExport)
        SelectionAction(Icons.Default.Delete, "删除", true, onDelete)
    }
}

@Composable
private fun SelectionAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = { if (enabled) onClick() },
                onLongClick = null,
            )
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        )
    }
}
