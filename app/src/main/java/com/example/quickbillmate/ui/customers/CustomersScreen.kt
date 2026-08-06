package com.example.quickbillmate.ui.customers

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.EmptyState
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LabeledSwitch
import com.example.quickbillmate.ui.common.SearchableTopBar

private val CUSTOMER_TYPES = listOf("全屋整装", "装修队", "家装公司", "个人")

@Composable
fun CustomersScreen(
    onImportContacts: () -> Unit,
    viewModel: CustomersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val customers by viewModel.customers.collectAsState()
    var editing by remember { mutableStateOf<Customer?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Customer?>(null) }

    Scaffold(
        topBar = {
            SearchableTopBar(
                title = "客户",
                searchPlaceholder = "搜索姓名/电话/类型",
                query = viewModel.queryText,
                onQueryChange = viewModel::setQuery,
                actions = {
                    TextButton(onClick = onImportContacts) { Text("导入") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新增客户")
            }
        },
    ) { padding ->
        if (customers.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Edit,
                text = if (viewModel.queryText.isNotBlank()) "没有找到匹配的客户" else "还没有客户，点击右下角新增",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(customers, key = { it.id }) { customer ->
                    CustomerCard(
                        customer = customer,
                        onEdit = { editing = customer },
                        onDelete = { pendingDelete = customer },
                        onToggleFavorite = { viewModel.toggleFavorite(customer) },
                    )
                }
            }
        }
    }

    if (showNewDialog) {
        CustomerEditDialog(
            initial = Customer(name = ""),
            onSave = { customer ->
                viewModel.saveCustomer(customer)
                showNewDialog = false
            },
            onDismiss = { showNewDialog = false },
        )
    }

    editing?.let { customer ->
        CustomerEditDialog(
            initial = customer,
            onSave = { updated ->
                viewModel.saveCustomer(updated)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    pendingDelete?.let { customer ->
        ConfirmDialog(
            title = "删除客户",
            text = "确定删除客户“${customer.name}”吗？",
            onConfirm = {
                viewModel.deleteCustomer(customer)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun CustomerCard(
    customer: Customer,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = onEdit,
                        onLongClick = onDelete,
                    )
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(customer.name, style = MaterialTheme.typography.titleSmall)
                    if (customer.type.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text(customer.type) },
                        )
                    }
                    if (customer.fromContacts) {
                        AssistChip(
                            onClick = {},
                            label = { Text("通讯录") },
                        )
                    }
                }
                if (customer.phone.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = if (customer.favorite) "取消收藏" else "收藏",
                    tint = if (customer.favorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun CustomerEditDialog(
    initial: Customer,
    onSave: (Customer) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var phone by remember { mutableStateOf(initial.phone) }
    var type by remember { mutableStateOf(initial.type) }
    var remark by remember { mutableStateOf(initial.remark) }
    var favorite by remember { mutableStateOf(initial.favorite) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "新增客户" else "编辑客户") },
        text = {
            Column {
                LabeledField("姓名*", name, { name = it })
                Spacer(Modifier.height(8.dp))
                LabeledField(
                    "电话（多个用逗号分隔）",
                    phone,
                    { phone = it },
                    keyboardType = KeyboardType.Phone,
                )
                Spacer(Modifier.height(8.dp))
                LabeledField("客户类型", type, { type = it })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CUSTOMER_TYPES.forEach { preset ->
                        FilterChip(
                            selected = type == preset,
                            onClick = { type = preset },
                            label = { Text(preset) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                LabeledField("备注", remark, { remark = it })
                Spacer(Modifier.height(4.dp))
                LabeledSwitch("收藏", favorite, { favorite = it })
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = if (name.isBlank()) "姓名不能为空" else null
                if (error == null) {
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            phone = phone.trim(),
                            type = type.trim(),
                            remark = remark.trim(),
                            favorite = favorite,
                        )
                    )
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
