package com.example.quickbillmate.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.EmptyState
import com.example.quickbillmate.ui.common.GroupSectionHeader
import com.example.quickbillmate.ui.common.IndexSection
import com.example.quickbillmate.ui.common.SearchableTopBar
import com.example.quickbillmate.ui.common.SelectionActionBar
import com.example.quickbillmate.ui.common.TimeIndexBar
import com.example.quickbillmate.ui.common.monthBubble
import com.example.quickbillmate.ui.common.monthKey
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.pressable
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 单据分组：key 为索引标识，title 为分组标题。 */
internal data class BillSection(
    val key: String,
    val title: String,
    val bills: List<HomeBill>,
)

/** 把单据列表按「收藏 → 时间（月份，新在前）」分组。 */
internal fun groupBills(bills: List<HomeBill>): List<BillSection> {
    val favorites = bills.filter { it.bill.favorite }
    val byMonth = bills.filterNot { it.bill.favorite }.groupBy { monthKey(it.bill.docDate) }
    return buildList {
        if (favorites.isNotEmpty()) add(BillSection("♥", "收藏", favorites))
        byMonth.keys
            .sortedWith(compareBy<String> { it == "其他" }.thenByDescending { it })
            .forEach { month ->
                byMonth[month]?.let { add(BillSection(month, monthBubble(month), it)) }
            }
    }
}

@Composable
fun HomeScreen(
    onNewBill: () -> Unit,
    onOpenBill: (Long) -> Unit,
    onSelectionModeChange: (Boolean) -> Unit,
    scrollToTopTick: Int = 0,
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
        scrollToTopTick = scrollToTopTick,
        selectionMode = viewModel.selectionMode,
        onSelectGroup = viewModel::selectGroup,
        selectedIds = viewModel.selectedIds,
        searchQuery = viewModel.searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onNewBill = onNewBill,
        onOpenBill = onOpenBill,
        onEnterSelection = viewModel::enterSelection,
        onToggleSelection = viewModel::toggleSelection,
        onExitSelection = viewModel::exitSelection,
        onToggleSelectAll = viewModel::toggleSelectAll,
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

/** 单据页纯界面层：数据与回调全部由参数传入，可在 Android Studio 中直接预览调试。 */
@Composable
fun HomeContent(
    bills: List<HomeBill>,
    scrollToTopTick: Int = 0,
    selectionMode: Boolean,
    onSelectGroup: (Set<Long>) -> Unit,
    selectedIds: Set<Long>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNewBill: () -> Unit,
    onOpenBill: (Long) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onExitSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
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

    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) listState.animateScrollToItem(0)
    }
    val grouped = remember(bills) { groupBills(bills) }
    val sections = remember(grouped) {
        grouped.map { section ->
            if (section.key == "♥") {
                IndexSection("♥", "收藏")
            } else {
                IndexSection(section.key, monthBubble(section.key))
            }
        }
    }
    val firstBySection = remember(grouped) {
        buildMap {
            var index = 0
            grouped.forEach { section ->
                put(section.key, index)
                index += 1 + section.bills.size
            }
        }
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                AppTopBar(
                    title = "已选中 ${selectedIds.size} 项",
                    navigationIcon = {
                        IconButton(onClick = onExitSelection) {
                            Icon(MiuixIcons.Basic.Close, contentDescription = "退出多选")
                        }
                    },
                    actions = {
                        val allVisibleSelected = bills.isNotEmpty() && bills.all { it.bill.id in selectedIds }
                        TextButton(
                            text = if (allVisibleSelected) "取消全选" else "全选",
                            onClick = onToggleSelectAll,
                            modifier = Modifier.testTag("select_all_toggle"),
                        )
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
                    // padding 放链首：语义节点边界=可视圆钮，中心可被测试/无障碍正确点中
                    modifier = Modifier
                        .padding(bottom = 84.dp)
                        .testTag("home_new_bill")
                        .pressable(interactionSource = remember { MutableInteractionSource() }),
                ) {
                    Icon(
                        MiuixIcons.Add,
                        contentDescription = "新建单据",
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
                icon = MiuixIcons.Store,
                text = if (searchQuery.isNotBlank()) "没有找到匹配的单据" else "还没有单据，点击右下角新建",
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
                        item(key = "header_${section.key}", contentType = { "sectionHeader" }) {
                            GroupSectionHeader(
                                title = section.title,
                                showTopDivider = groupIndex > 0,
                                allSelected =
                                    section.bills.isNotEmpty() &&
                                        section.bills.all { it.bill.id in selectedIds },
                                onSelectGroup = if (selectionMode) {
                                    { onSelectGroup(section.bills.map { it.bill.id }.toSet()) }
                                } else {
                                    null
                                },
                            )
                        }
                        items(
                            section.bills,
                            key = { it.bill.id },
                            contentType = { "billCard" },
                        ) { homeBill ->
                        val bill = homeBill.bill
                        val selected = bill.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                .padding(start = 0.dp, end = 32.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selectionMode) {
                                Checkbox(
                                    state = if (selected) ToggleableState.On else ToggleableState.Off,
                                    onClick = { onToggleSelection(bill.id) },
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = bill.customerName.ifBlank { "未填写客户" },
                                        style = AppThemeTypography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = bill.docDate,
                                        style = AppThemeTypography.bodySmall,
                                        color = AppThemeColors.outline,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${homeBill.docNumber} · ${homeBill.itemCount} 项",
                                    style = AppThemeTypography.bodySmall,
                                    color = AppThemeColors.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "¥${homeBill.receivableText}",
                                    style = AppThemeTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppThemeColors.primary,
                                )
                            }
                        }
                    }
                    }
                }
                TimeIndexBar(
                    state = listState,
                    sections = sections,
                    firstIndexOf = { firstBySection[it] ?: -1 },
                )
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除单据",
            destructive = true,
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
