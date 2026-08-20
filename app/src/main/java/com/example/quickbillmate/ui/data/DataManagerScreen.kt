package com.example.quickbillmate.ui.data

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.importexport.DataCategory
import com.example.quickbillmate.importexport.DefaultsGroup
import com.example.quickbillmate.navigation.PendingImport
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.DatePickerDialog
import com.example.quickbillmate.ui.common.DialogButtons
import com.example.quickbillmate.ui.common.DialogScrollColumn
import com.example.quickbillmate.ui.common.InfoDialog
import com.example.quickbillmate.ui.common.SelectionChip
import com.example.quickbillmate.ui.common.SmallTextButton
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.Ds
import com.example.quickbillmate.util.BillNumber
import com.example.quickbillmate.util.Money
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Back
import java.time.LocalDate
import java.time.YearMonth

/** 选择导出页状态。 */
private sealed interface SelectionState {
    data class Products(val all: List<Product>, val checked: Set<Long>) : SelectionState
    data class Customers(val all: List<Customer>, val checked: Set<Long>) : SelectionState
    data class Bills(val all: List<Bill>, val amounts: Map<Long, Double>, val checked: Set<Long>) : SelectionState
    data class Defaults(val checked: Set<DefaultsGroup>) : SelectionState
}

private enum class BillRangePreset(val label: String) {
    ALL("全部"),
    THIS_MONTH("本月"),
    LAST_MONTH("上月"),
    LAST_3M("近3月"),
    THIS_YEAR("今年"),
    CUSTOM("自定义"),
}

@Composable
fun DataManagerScreen(
    onBack: () -> Unit,
    viewModel: DataManagerViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selection by remember { mutableStateOf<SelectionState?>(null) }
    var selectionLoading by remember { mutableStateOf(false) }
    var billRange by remember { mutableStateOf(BillRangePreset.ALL) }
    var customStart by remember { mutableStateOf<String?>(null) }
    var customEnd by remember { mutableStateOf<String?>(null) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var pendingFilePicker by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importFromUri(uri)
    }

    // 外部应用打开 JSON：自动进入导入确认
    LaunchedEffect(Unit) {
        val uri = PendingImport.uri
        if (uri != null) {
            PendingImport.uri = null
            viewModel.importFromUri(uri)
        }
    }
    LaunchedEffect(pendingFilePicker) {
        if (pendingFilePicker) {
            pendingFilePicker = false
            filePicker.launch(arrayOf("application/json"))
        }
    }
    val shareUri = viewModel.shareUri
    LaunchedEffect(shareUri) {
        shareUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享 JSON 文件"))
            viewModel.consumeShare()
        }
    }
    val toast = viewModel.toast
    LaunchedEffect(toast) {
        toast?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "数据导入导出",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                selectionLoading -> CenteredProgress("加载中…")
                selection != null -> SelectionPage(
                    state = selection!!,
                    currentRange = billRange,
                    onToggleProduct = { id ->
                        val s = selection as SelectionState.Products
                        selection = s.copy(checked = toggle(s.checked, id))
                    },
                    onToggleCustomer = { id ->
                        val s = selection as SelectionState.Customers
                        selection = s.copy(checked = toggle(s.checked, id))
                    },
                    onToggleBill = { id ->
                        val s = selection as SelectionState.Bills
                        selection = s.copy(checked = toggle(s.checked, id))
                    },
                    onToggleGroup = { group ->
                        val s = selection as SelectionState.Defaults
                        selection = s.copy(checked = toggleSet(s.checked, group))
                    },
                    onRangeChange = { preset ->
                        billRange = preset
                        scope.launch {
                            selectionLoading = true
                            val (start, end) = rangeOf(preset, customStart, customEnd)
                            val bills = viewModel.loadBills(start, end)
                            selection = SelectionState.Bills(bills, viewModel.billAmounts(bills), emptySet())
                            selectionLoading = false
                        }
                    },
                    onPickStart = { pickingStart = true },
                    onPickEnd = { pickingEnd = true },
                    onClose = { selection = null },
                    onExport = { sel ->
                        when (sel) {
                            is SelectionState.Products -> viewModel.exportSelected(DataCategory.PRODUCTS, sel.checked)
                            is SelectionState.Customers -> viewModel.exportSelected(DataCategory.CUSTOMERS, sel.checked)
                            is SelectionState.Bills -> viewModel.exportSelected(DataCategory.BILLS, sel.checked)
                            is SelectionState.Defaults -> viewModel.exportDefaults(sel.checked)
                        }
                        selection = null
                    },
                )
                else -> CategoryList(
                    counts = viewModel.counts,
                    onExportAll = { viewModel.exportAll(it) },
                    onSelect = { category ->
                        scope.launch {
                            selectionLoading = true
                            selection = when (category) {
                                DataCategory.PRODUCTS ->
                                    SelectionState.Products(viewModel.loadProducts(), emptySet())
                                DataCategory.CUSTOMERS ->
                                    SelectionState.Customers(viewModel.loadCustomers(), emptySet())
                                DataCategory.BILLS -> {
                                    val (start, end) = rangeOf(billRange, customStart, customEnd)
                                    val bills = viewModel.loadBills(start, end)
                                    SelectionState.Bills(bills, viewModel.billAmounts(bills), emptySet())
                                }
                                DataCategory.DEFAULTS ->
                                    SelectionState.Defaults(DefaultsGroup.entries.toSet())
                            }
                            selectionLoading = false
                        }
                    },
                    onImport = { pendingFilePicker = true },
                )
            }
            if (viewModel.busy) {
                Box(
                    modifier = Modifier.fillMaxSize().backgroundScrim(),
                    contentAlignment = Alignment.Center,
                ) {
                    CenteredProgress("处理中…")
                }
            }
        }
    }

    if (pickingStart) {
        DatePickerDialog(
            initial = LocalDate.parse(customStart ?: LocalDate.now().minusDays(30).toString()),
            onConfirm = { date ->
                customStart = date.toString()
                pickingStart = false
                scope.launch {
                    selectionLoading = true
                    val (start, end) = rangeOf(BillRangePreset.CUSTOM, customStart, customEnd)
                    val bills = viewModel.loadBills(start, end)
                    selection = SelectionState.Bills(bills, viewModel.billAmounts(bills), emptySet())
                    selectionLoading = false
                }
            },
            onDismiss = { pickingStart = false },
        )
    }
    if (pickingEnd) {
        DatePickerDialog(
            initial = LocalDate.parse(customEnd ?: LocalDate.now().toString()),
            onConfirm = { date ->
                customEnd = date.toString()
                pickingEnd = false
                scope.launch {
                    selectionLoading = true
                    val (start, end) = rangeOf(BillRangePreset.CUSTOM, customStart, customEnd)
                    val bills = viewModel.loadBills(start, end)
                    selection = SelectionState.Bills(bills, viewModel.billAmounts(bills), emptySet())
                    selectionLoading = false
                }
            },
            onDismiss = { pickingEnd = false },
        )
    }

    viewModel.importPreview?.let { preview ->
        ConfirmDialog(
            title = "确认导入",
            text = "检测到【${preview.category.label}】文件，共 ${preview.count} 条。\n将按合并规则导入：商品/客户按名称去重合并、单据按原样新增、默认信息覆盖对应字段。",
            confirmText = "确认导入",
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::cancelImport,
        )
    }

    viewModel.importResult?.let { result ->
        OverlayResultDialog(
            result = result,
            onDismiss = viewModel::consumeResult,
        )
    }

    viewModel.importError?.let { message ->
        InfoDialog(
            title = "导入失败",
            text = message,
            onDismiss = viewModel::consumeError,
        )
    }
}

private fun <T> toggle(set: Set<T>, value: T): Set<T> =
    if (value in set) set - value else set + value

private fun toggleSet(set: Set<DefaultsGroup>, value: DefaultsGroup): Set<DefaultsGroup> =
    if (value in set) set - value else set + value

private fun rangeOf(preset: BillRangePreset, customStart: String?, customEnd: String?): Pair<String?, String?> {
    val today = LocalDate.now()
    return when (preset) {
        BillRangePreset.ALL -> null to null
        BillRangePreset.THIS_MONTH -> YearMonth.now().atDay(1).toString() to today.toString()
        BillRangePreset.LAST_MONTH -> {
            val ym = YearMonth.now().minusMonths(1)
            ym.atDay(1).toString() to ym.atEndOfMonth().toString()
        }
        BillRangePreset.LAST_3M -> YearMonth.now().minusMonths(2).atDay(1).toString() to today.toString()
        BillRangePreset.THIS_YEAR -> today.withDayOfYear(1).toString() to today.toString()
        BillRangePreset.CUSTOM -> customStart to customEnd
    }
}

@Composable
private fun CenteredProgress(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text(text, style = AppThemeTypography.bodySmall, color = AppThemeColors.onSurfaceVariant)
        }
    }
}

private fun Modifier.backgroundScrim(): Modifier =
    this.background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.15f))

@Composable
private fun CategoryList(
    counts: DataCounts,
    onExportAll: (DataCategory) -> Unit,
    onSelect: (DataCategory) -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ds.screen, vertical = Ds.sm),
        verticalArrangement = Arrangement.spacedBy(Ds.md),
    ) {
        Text(
            "选择一类数据，导出为 JSON 文件（可通过系统分享发送，也可在其他应用中打开并选择“快贝智单”导入）。",
            style = AppThemeTypography.bodySmall,
            color = AppThemeColors.onSurfaceVariant,
        )
        CategoryCard(
            label = "单据",
            count = "${counts.bills} 张",
            onExportAll = { onExportAll(DataCategory.BILLS) },
            onSelect = { onSelect(DataCategory.BILLS) },
            onImport = onImport,
        )
        CategoryCard(
            label = "商品",
            count = "${counts.products} 个",
            onExportAll = { onExportAll(DataCategory.PRODUCTS) },
            onSelect = { onSelect(DataCategory.PRODUCTS) },
            onImport = onImport,
        )
        CategoryCard(
            label = "客户",
            count = "${counts.customers} 位",
            onExportAll = { onExportAll(DataCategory.CUSTOMERS) },
            onSelect = { onSelect(DataCategory.CUSTOMERS) },
            onImport = onImport,
        )
        CategoryCard(
            label = "默认信息",
            count = "含预置单位",
            onExportAll = { onExportAll(DataCategory.DEFAULTS) },
            onSelect = { onSelect(DataCategory.DEFAULTS) },
            onImport = onImport,
        )
        Spacer(Modifier.height(Ds.lg))
    }
}

@Composable
private fun CategoryCard(
    label: String,
    count: String,
    onExportAll: () -> Unit,
    onSelect: () -> Unit,
    onImport: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = AppThemeColors.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(Ds.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = AppThemeTypography.titleSmall, modifier = Modifier.weight(1f))
                Text(count, style = AppThemeTypography.bodySmall, color = AppThemeColors.onSurfaceVariant)
            }
            Spacer(Modifier.height(Ds.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Ds.xs)) {
                SmallTextButton(text = "导出全部", onClick = onExportAll)
                SmallTextButton(text = "选择导出", onClick = onSelect)
                SmallTextButton(text = "导入", onClick = onImport, primary = true)
            }
        }
    }
}

@Composable
private fun SelectionPage(
    state: SelectionState,
    currentRange: BillRangePreset,
    onToggleProduct: (Long) -> Unit,
    onToggleCustomer: (Long) -> Unit,
    onToggleBill: (Long) -> Unit,
    onToggleGroup: (DefaultsGroup) -> Unit,
    onRangeChange: (BillRangePreset) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onClose: () -> Unit,
    onExport: (SelectionState) -> Unit,
) {
    val checkedCount = when (state) {
        is SelectionState.Products -> state.checked.size
        is SelectionState.Customers -> state.checked.size
        is SelectionState.Bills -> state.checked.size
        is SelectionState.Defaults -> state.checked.size
    }
    Scaffold(
        topBar = {
            AppTopBar(
                title = when (state) {
                    is SelectionState.Products -> "选择商品"
                    is SelectionState.Customers -> "选择客户"
                    is SelectionState.Bills -> "选择单据"
                    is SelectionState.Defaults -> "选择默认信息"
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Ds.screen, vertical = Ds.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "已选 $checkedCount 项",
                    style = AppThemeTypography.bodyMedium,
                    color = AppThemeColors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { onExport(state) },
                    enabled = checkedCount > 0,
                ) {
                    Text("导出选中")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state is SelectionState.Bills) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Ds.screen, vertical = Ds.sm),
                    horizontalArrangement = Arrangement.spacedBy(Ds.sm),
                ) {
                    BillRangePreset.entries.forEach { preset ->
                        SelectionChip(
                            text = preset.label,
                            selected = preset == currentRange,
                            onClick = { onRangeChange(preset) },
                        )
                    }
                }
            }
            when (state) {
                is SelectionState.Products -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Ds.screen, vertical = Ds.sm),
                ) {
                    items(state.all, key = { it.id }) { product ->
                        CheckRow(
                            checked = product.id in state.checked,
                            title = product.name,
                            subtitle = listOf(product.spec, product.unit)
                                .filter { it.isNotBlank() }
                                .joinToString("  "),
                            trailing = "¥${Money.format(product.price)}",
                            onToggle = { onToggleProduct(product.id) },
                        )
                    }
                }
                is SelectionState.Customers -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Ds.screen, vertical = Ds.sm),
                ) {
                    items(state.all, key = { it.id }) { customer ->
                        CheckRow(
                            checked = customer.id in state.checked,
                            title = customer.name,
                            subtitle = listOf(customer.type, customer.phone).filter { it.isNotBlank() }.joinToString("  "),
                            trailing = "",
                            onToggle = { onToggleCustomer(customer.id) },
                        )
                    }
                }
                is SelectionState.Bills -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Ds.screen, vertical = Ds.sm),
                ) {
                    items(state.all, key = { it.id }) { bill ->
                        CheckRow(
                            checked = bill.id in state.checked,
                            title = BillNumber.build(bill.docCode, bill.docDate, bill.docSerial),
                            subtitle = listOf(bill.docDate, bill.customerName.ifBlank { "未填写客户" })
                                .joinToString(" · "),
                            trailing = "¥${Money.format(state.amounts[bill.id] ?: 0.0)}",
                            onToggle = { onToggleBill(bill.id) },
                        )
                    }
                }
                is SelectionState.Defaults -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Ds.screen, vertical = Ds.sm),
                ) {
                    DefaultsGroup.entries.forEach { group ->
                        CheckRow(
                            checked = group in state.checked,
                            title = group.label,
                            subtitle = groupSubtitle(group),
                            trailing = "",
                            onToggle = { onToggleGroup(group) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

private fun groupSubtitle(group: DefaultsGroup): String = when (group) {
    DefaultsGroup.TITLE -> "标题后缀、编号代码"
    DefaultsGroup.COMPANY -> "公司名称、客户经理、联系电话"
    DefaultsGroup.OTHER -> "备注、广告文案、水印与显示开关"
    DefaultsGroup.UNITS -> "新增商品的下拉单位选项"
}

@Composable
private fun CheckRow(
    checked: Boolean,
    title: String,
    subtitle: String,
    trailing: String,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Ds.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            state = if (checked) ToggleableState.On else ToggleableState.Off,
            onClick = onToggle,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = AppThemeTypography.bodyMedium)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = AppThemeTypography.bodySmall, color = AppThemeColors.onSurfaceVariant)
            }
        }
        if (trailing.isNotBlank()) {
            Spacer(Modifier.width(Ds.md))
            Text(trailing, style = AppThemeTypography.bodyMedium, color = AppThemeColors.primary)
        }
    }
}

@Composable
private fun OverlayResultDialog(
    result: ImportResultUi,
    onDismiss: () -> Unit,
) {
    top.yukonga.miuix.kmp.overlay.OverlayDialog(
        title = "导入完成",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        DialogScrollColumn {
            Text(
                "成功 ${result.success} 条 / 跳过重复 ${result.skipped} 条 / 失败 ${result.failures.size} 条",
                style = AppThemeTypography.bodyMedium,
            )
            if (result.failures.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    result.failures.take(20).joinToString("\n") { "第${it.first}行：${it.second}" },
                    style = AppThemeTypography.bodySmall,
                    color = AppThemeColors.error,
                )
            }
            Spacer(Modifier.height(Ds.md))
            DialogButtons(
                confirmText = "完成",
                cancelText = null,
                onConfirm = onDismiss,
            )
        }
    }
}
