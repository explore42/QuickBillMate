package com.example.quickbillmate.ui.customers

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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

import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.DetailLine
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.EmptyState
import com.example.quickbillmate.ui.common.GroupSectionHeader
import com.example.quickbillmate.ui.common.InitialCircle
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LetterIndexBar
import com.example.quickbillmate.ui.common.IndexSection
import com.example.quickbillmate.ui.common.LabeledSwitch
import com.example.quickbillmate.ui.common.PhoneListEditor
import com.example.quickbillmate.ui.common.SearchableTopBar
import com.example.quickbillmate.ui.common.SelectionActionBar
import com.example.quickbillmate.util.PhoneUtil
import com.example.quickbillmate.util.InputLimits


private val CUSTOMER_TYPES = listOf("全屋整装", "装修队", "家装公司", "个人")

/** 客户分组：title 为分组标题（收藏 / 字母 / #）。 */
internal data class CustomerSection(
    val title: String,
    val customers: List<Customer>,
)

/**
 * 把已排序的客户列表按「收藏 → 字母 → #」分组。
 * 收藏客户只进“收藏”组，字母组只含非收藏客户，避免同一客户出现在两个分组。
 */
internal fun groupCustomers(customers: List<Customer>, letters: List<String>): List<CustomerSection> {
    val favorites = customers.filter { it.favorite }
    val byLetter = customers.zip(letters).filterNot { (customer, _) -> customer.favorite }.groupBy { it.second }
    return buildList {
        if (favorites.isNotEmpty()) add(CustomerSection("收藏", favorites))
        byLetter.keys.filter { it != "#" }.sorted().forEach { letter ->
            byLetter[letter]?.let { add(CustomerSection(letter, it.map { pair -> pair.first })) }
        }
        byLetter["#"]?.let { add(CustomerSection("#", it.map { pair -> pair.first })) }
    }
}

@Composable
fun CustomersScreen(
    onImportContacts: () -> Unit,
    onSelectionModeChange: (Boolean) -> Unit = {},
    scrollToTopTick: Int = 0,
    viewModel: CustomersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val customers by viewModel.customers.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) listState.animateScrollToItem(0)
    }
    val letters = remember(customers) { customers.map { it.pinyinInitial } }
    val grouped = remember(customers, letters) { groupCustomers(customers, letters) }
    val sections = remember(grouped) {
        grouped.map { section ->
            if (section.title == "收藏") {
                IndexSection("♥", "收藏")
            } else {
                IndexSection(section.title, section.title)
            }
        }
    }
    val firstBySection = remember(grouped) {
        buildMap {
            var index = 0
            grouped.forEach { section ->
                put(if (section.title == "收藏") "♥" else section.title, index)
                index += 1 + section.customers.size
            }
        }
    }
    var editing by remember { mutableStateOf<Customer?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Customer?>(null) }

    LaunchedEffect(viewModel.selectionMode) {
        onSelectionModeChange(viewModel.selectionMode)
    }

    val context = LocalContext.current
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
    val exportError = viewModel.exportError
    LaunchedEffect(exportError) {
        exportError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeExportError()
        }
    }

    BackHandler(enabled = viewModel.selectionMode) {
        viewModel.exitSelection()
    }

    Scaffold(
        topBar = {
            if (viewModel.selectionMode) {
                AppTopBar(
                    title = "已选中 ${viewModel.selectedIds.size} 项",
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "退出多选")
                        }
                    },
                    actions = {
                        val allVisibleSelected =
                            customers.isNotEmpty() && customers.all { it.id in viewModel.selectedIds }
                        TextButton(
                            onClick = { viewModel.toggleSelectAll() },
                            modifier = Modifier.testTag("select_all_toggle"),
                        ) {
                            Text(if (allVisibleSelected) "取消全选" else "全选")
                        }
                    },
                )
            } else {
            SearchableTopBar(
                title = "快贝智单",
                searchPlaceholder = "搜索姓名/电话/类型",
                query = viewModel.queryText,
                onQueryChange = viewModel::setQuery,
                actions = {
                    TextButton(onClick = onImportContacts) { Text("导入") }
                },
            )
            }
        },
        floatingActionButton = {
            if (!viewModel.selectionMode) {
                FloatingActionButton(onClick = { showNewDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "新增客户")
                }
            }
        },
        bottomBar = {
            if (viewModel.selectionMode) {
                SelectionActionBar(
                    canEdit = viewModel.selectedIds.size == 1,
                    onCopy = { viewModel.copySelected() },
                    onEdit = { viewModel.editSelected { editing = it } },
                    onExport = { viewModel.exportSelected() },
                    onDelete = {
                        viewModel.requestDelete()
                        showDeleteConfirm = true
                    },
                )
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
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    grouped.forEachIndexed { groupIndex, section ->
                        item(key = "header_${section.title}", contentType = { "sectionHeader" }) {
                            GroupSectionHeader(
                                title = section.title,
                                showTopDivider = groupIndex > 0,
                                allSelected =
                                    section.customers.isNotEmpty() &&
                                        section.customers.all { it.id in viewModel.selectedIds },
                                onSelectGroup = if (viewModel.selectionMode) {
                                    { viewModel.selectGroup(section.customers.map { it.id }.toSet()) }
                                } else {
                                    null
                                },
                            )
                        }
                        items(
                            section.customers,
                            key = { it.id },
                            contentType = { "customerCard" },
                        ) { customer ->
                            CustomerCard(
                                customer = customer,
                                selectionMode = viewModel.selectionMode,
                                selected = customer.id in viewModel.selectedIds,
                                onToggleSelection = { viewModel.toggleSelection(customer.id) },
                                onDetail = { detail = customer },
                                onDelete = { viewModel.enterSelection(customer.id) },
                            )
                        }
                    }
                }
                LetterIndexBar(
                    state = listState,
                    sections = sections,
                    firstIndexOf = { firstBySection[it] ?: -1 },
                )
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

    detail?.let { customer ->
        CustomerDetailDialog(
            customer = customer,
            onToggleFavorite = { newValue ->
                val updated = customer.copy(favorite = newValue)
                viewModel.saveCustomer(updated)
                detail = updated
            },
            onEdit = {
                detail = null
                editing = customer
            },
            onCall = PhoneUtil.splitPhones(customer.phone).firstOrNull()?.let { phone ->
                {
                    context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
                }
            },
            onDismiss = { detail = null },
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

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除客户",
            text = "确定删除选中的 ${viewModel.selectedIds.size} 条客户吗？此操作不可恢复。",
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
private fun CustomerDetailDialog(
    customer: Customer,
    onToggleFavorite: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onCall: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("客户详情") },
        text = {
            Column {
                DetailLine("姓名", customer.name)
                if (customer.type.isNotBlank()) DetailLine("客户类型", customer.type)
                if (customer.phone.isNotBlank()) DetailLine("电话", customer.phone)
                if (customer.remark.isNotBlank()) DetailLine("备注", customer.remark)
                Spacer(Modifier.height(6.dp))
                LabeledSwitch("收藏", customer.favorite, onToggleFavorite)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onEdit()
                    onDismiss()
                },
            ) { Text("修改") }
        },
        confirmButton = {
            if (onCall != null) {
                Button(onClick = onCall) { Text("呼叫") }
            }
        },
    )
}

@Composable
private fun CustomerCard(
    customer: Customer,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onDetail: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelection() })
            Spacer(Modifier.width(8.dp))
        }
        InitialCircle(customer.name.trim().firstOrNull()?.toString() ?: "?")
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = { if (selectionMode) onToggleSelection() else onDetail() },
                    onLongClick = { if (selectionMode) onToggleSelection() else onDelete() },
                )
                .padding(top = 6.dp, bottom = 6.dp),
        ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(customer.name, style = MaterialTheme.typography.titleSmall)
                    if (customer.type.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                customer.type,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }

                }
                if (customer.phone.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    var phones by remember(initial.phone) {
        mutableStateOf(
            initial.phone.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("") }
        )
    }
    var type by remember { mutableStateOf(initial.type) }
    var remark by remember { mutableStateOf(initial.remark) }
    var favorite by remember { mutableStateOf(initial.favorite) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "新增客户" else "编辑客户") },
        text = {
            Column {
                LabeledField("姓名*", name, { if (it.length <= InputLimits.NAME) name = it })
                Spacer(Modifier.height(8.dp))
                Text("电话", style = MaterialTheme.typography.titleSmall)
                PhoneListEditor(phones = phones, onChange = { phones = it })
                Spacer(Modifier.height(8.dp))
                LabeledField("客户类型", type, { if (it.length <= InputLimits.CUSTOMER_TYPE) type = it })
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
                LabeledField("备注", remark, { if (it.length <= InputLimits.REMARK) remark = it })
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
                error = when {
                    name.isBlank() -> "姓名不能为空"
                    else -> {
                        val invalidIndex = phones.indexOfFirst { p ->
                            p.isNotBlank() && !PhoneUtil.isValidPhone(PhoneUtil.normalizePhone(p))
                        }
                        if (invalidIndex >= 0) "第 ${invalidIndex + 1} 个电话格式不正确" else null
                    }
                }
                if (error == null) {
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            phone = phones.map { PhoneUtil.normalizePhone(it) }
                                .filter { it.isNotEmpty() }
                                .joinToString(","),
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
