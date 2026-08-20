package com.example.quickbillmate.ui.customers

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.DetailLine
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.DialogButtons
import com.example.quickbillmate.ui.common.DialogScrollColumn
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.EmptyState
import com.example.quickbillmate.ui.common.GroupSectionHeader
import com.example.quickbillmate.ui.common.InitialCircle
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LetterIndexBar
import com.example.quickbillmate.ui.common.IndexSection
import com.example.quickbillmate.ui.common.LabeledSwitch
import com.example.quickbillmate.ui.common.LocalHaptics
import com.example.quickbillmate.ui.common.TopBarActionTextButton
import com.example.quickbillmate.ui.common.PhoneListEditor
import com.example.quickbillmate.ui.common.PhoneTag
import com.example.quickbillmate.ui.common.SearchableTopBar
import com.example.quickbillmate.ui.common.SelectionChip
import com.example.quickbillmate.ui.common.TagChip
import com.example.quickbillmate.ui.common.SmallTextButton
import com.example.quickbillmate.ui.common.SelectionActionBar
import com.example.quickbillmate.util.PhoneUtil
import com.example.quickbillmate.util.InputLimits
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.pressable
import top.yukonga.miuix.kmp.utils.scrollEndHaptic


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
                            Icon(MiuixIcons.Basic.Close, contentDescription = "退出多选")
                        }
                    },
                    actions = {
                        val allVisibleSelected =
                            customers.isNotEmpty() && customers.all { it.id in viewModel.selectedIds }
                        TopBarActionTextButton(
                            text = if (allVisibleSelected) "取消全选" else "全选",
                            onClick = { viewModel.toggleSelectAll() },
                            modifier = Modifier.testTag("select_all_toggle"),
                        )
                    },
                )
            } else {
            SearchableTopBar(
                title = "快贝智单",
                searchPlaceholder = "搜索姓名/电话/类型",
                query = viewModel.queryText,
                onQueryChange = viewModel::setQuery,
                actions = {
                    TextButton(
                        text = "导入",
                        onClick = onImportContacts,
                        minHeight = 32.dp,
                        insideMargin = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    )
                },
            )
            }
        },
        floatingActionButton = {
            if (!viewModel.selectionMode) {
                FloatingActionButton(
                    onClick = { showNewDialog = true },
                    // padding 放链首：语义节点边界=可视圆钮，中心可被测试/无障碍正确点中
                    modifier = Modifier
                        .padding(bottom = 84.dp)
                        .pressable(interactionSource = remember { MutableInteractionSource() }),
                ) {
                    Icon(MiuixIcons.Add, contentDescription = "新增客户")
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.selectionMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
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
                icon = MiuixIcons.Edit,
                text = if (viewModel.queryText.isNotBlank()) "没有找到匹配的客户" else "还没有客户，点击右下角新增",
                modifier = Modifier.padding(padding),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .overScrollVertical()
                        .scrollEndHaptic(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
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
                                modifier = Modifier.animateItem(),
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
            onDial = { phone ->
                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
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
            destructive = true,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerDetailDialog(
    customer: Customer,
    onToggleFavorite: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDial: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val phones = PhoneUtil.splitPhones(customer.phone)
    // 与商品详情一致：居中对话框，底部仅「修改」主按钮，点弹窗外关闭
    OverlayDialog(
        title = "客户详情",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column {
            DetailLine("姓名", customer.name)
            if (customer.type.isNotBlank()) DetailLine("客户类型", customer.type)
            if (phones.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        "电话",
                        style = AppThemeTypography.bodyMedium,
                        color = AppThemeColors.onSurfaceVariant,
                        modifier = Modifier.width(96.dp),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        phones.forEach { phone ->
                            PhoneTag(phone = phone, onClick = { onDial(phone) })
                        }
                    }
                }
            }
            if (customer.remark.isNotBlank()) DetailLine("备注", customer.remark)
            Spacer(Modifier.height(6.dp))
            LabeledSwitch("收藏", customer.favorite, onToggleFavorite)
            Spacer(Modifier.height(12.dp))
            DialogButtons(
                confirmText = "修改",
                cancelText = null,
                onConfirm = {
                    onEdit()
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun CustomerCard(
    customer: Customer,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onDetail: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 整行可点击（含头像区域），长按进入多选；圆角涟漪+下沉按压
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .indication(interactionSource, SinkFeedback(sinkAmount = 0.97f))
            .combinedClickable(
                interactionSource = interactionSource,
                onClick = { if (selectionMode) onToggleSelection() else onDetail() },
                onLongClick = {
                    if (selectionMode) {
                        onToggleSelection()
                    } else {
                        haptics.longPress()
                        onDelete()
                    }
                },
            )
            .padding(end = 32.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Checkbox(
                state = if (selected) ToggleableState.On else ToggleableState.Off,
                onClick = { onToggleSelection() },
            )
            Spacer(Modifier.width(8.dp))
        }
        InitialCircle(customer.name.trim().firstOrNull()?.toString() ?: "?")
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(customer.name, style = AppThemeTypography.titleSmall)
                if (customer.type.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    TagChip(customer.type)
                }
            }
            if (customer.phone.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    customer.phone,
                    style = AppThemeTypography.bodySmall,
                    color = AppThemeColors.onSurfaceVariant,
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

    OverlayDialog(
        title = if (initial.id == 0L) "新增客户" else "编辑客户",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        DialogScrollColumn {
            LabeledField("姓名*", name, { if (it.length <= InputLimits.NAME) name = it })
            Spacer(Modifier.height(12.dp))
            Text("电话", style = AppThemeTypography.titleSmall)
            PhoneListEditor(phones = phones, onChange = { phones = it })
            Spacer(Modifier.height(12.dp))
            LabeledField("客户类型", type, { if (it.length <= InputLimits.CUSTOMER_TYPE) type = it })
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CUSTOMER_TYPES.forEach { preset ->
                    SelectionChip(
                        text = preset,
                        selected = type == preset,
                        onClick = { type = preset },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LabeledField("备注", remark, { if (it.length <= InputLimits.REMARK) remark = it })
            Spacer(Modifier.height(4.dp))
            LabeledSwitch("收藏", favorite, { favorite = it })
            error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = AppThemeColors.error, style = AppThemeTypography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            DialogButtons(
                confirmText = "保存",
                onCancel = onDismiss,
                onConfirm = {
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
                },
            )
        }
    }
}
