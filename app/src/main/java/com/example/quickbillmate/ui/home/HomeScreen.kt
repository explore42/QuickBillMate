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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewBill: () -> Unit,
    onOpenBill: (Long) -> Unit,
    onToggleTheme: () -> Unit,
    darkTheme: Boolean,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val bills by viewModel.bills.collectAsState()
    var menuBill by remember { mutableStateOf<Bill?>(null) }
    var pendingDelete by remember { mutableStateOf<Bill?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("快贝智单") },
                actions = {
                    TextButton(onClick = onToggleTheme) {
                        Text(if (darkTheme) "☀ 浅色" else "☾ 深色")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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

            if (bills.isEmpty()) {
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
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = { onOpenBill(bill.id) },
                                        onLongClick = { menuBill = bill },
                                    )
                                    .padding(14.dp),
                            ) {
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
                        }
                    }
                }
            }
        }
    }

    menuBill?.let { bill ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { menuBill = null },
        ) {
            DropdownMenuItem(
                text = { Text("编辑") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    menuBill = null
                    onOpenBill(bill.id)
                },
            )
            DropdownMenuItem(
                text = { Text("删除") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    menuBill = null
                    pendingDelete = bill
                },
            )
        }
    }

    pendingDelete?.let { bill ->
        ConfirmDialog(
            title = "删除单据",
            text = "确定删除该单据及其全部商品行吗？此操作不可恢复。",
            onConfirm = {
                viewModel.deleteBill(bill)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
