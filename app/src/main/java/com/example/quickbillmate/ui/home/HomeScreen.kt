package com.example.quickbillmate.ui.home

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.EmptyState
import com.example.quickbillmate.ui.common.InfoDialog

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

    Scaffold(
        topBar = {
            if (viewModel.selectionMode) {
                TopAppBar(
                    title = { Text("已选中 ${viewModel.selectedIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "退出多选")
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(2.dp))
                            Text("全选")
                        }
                    },
                )
            } else {
                TopAppBar(title = { Text("快贝智单") })
            }
        },
        bottomBar = {
            if (viewModel.selectionMode) {
                SelectionActionBar(
                    canEdit = viewModel.selectedIds.size == 1,
                    onCopy = { viewModel.copySelected() },
                    onEdit = { viewModel.editSelected(onOpenBill) },
                    onExport = { viewModel.exportSelected() },
                    onDelete = { showDeleteConfirm = true },
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!viewModel.selectionMode) {
                Button(
                    onClick = onNewBill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("home_new_bill"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("新建销售清单")
                }

                Text(
                    text = "最近单据",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (bills.isEmpty() && !viewModel.selectionMode) {
                EmptyState(
                    icon = Icons.Default.ShoppingCart,
                    text = "还没有单据，点击上方新建",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(bills, key = { it.bill.id }) { homeBill ->
                        val bill = homeBill.bill
                        val selected = bill.id in viewModel.selectedIds
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            if (viewModel.selectionMode) {
                                                viewModel.toggleSelection(bill.id)
                                            } else {
                                                onOpenBill(bill.id)
                                            }
                                        },
                                        onLongClick = {
                                            if (viewModel.selectionMode) {
                                                viewModel.toggleSelection(bill.id)
                                            } else {
                                                viewModel.enterSelection(bill.id)
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
                                if (viewModel.selectionMode) {
                                    Spacer(Modifier.width(8.dp))
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = { viewModel.toggleSelection(bill.id) },
                                    )
                                }
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
            text = "确定删除选中的 ${viewModel.selectedIds.size} 条单据及其商品行吗？此操作不可恢复。",
            onConfirm = {
                viewModel.confirmDelete()
                showDeleteConfirm = false
            },
            onDismiss = {
                viewModel.cancelDelete()
                showDeleteConfirm = false
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
