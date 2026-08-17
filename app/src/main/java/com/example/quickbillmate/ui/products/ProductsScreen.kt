package com.example.quickbillmate.ui.products

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.importexport.ProductJsonCodec
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.DetailLine
import com.example.quickbillmate.ui.common.DialogButtons
import com.example.quickbillmate.ui.common.EmptyState
import com.example.quickbillmate.ui.common.GroupSectionHeader
import com.example.quickbillmate.ui.common.IndexSection
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.InitialCircle
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LabeledSwitch
import com.example.quickbillmate.ui.common.LetterIndexBar
import com.example.quickbillmate.ui.common.SelectionActionBar
import com.example.quickbillmate.ui.common.SearchableTopBar
import com.example.quickbillmate.util.Money
import com.example.quickbillmate.util.InputLimits
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.pressable
import top.yukonga.miuix.kmp.utils.scrollEndHaptic


/** 商品分组：title 为分组标题（收藏 / 字母 / #）。 */
internal data class ProductSection(
    val title: String,
    val products: List<Product>,
)

/**
 * 把已排序的商品列表按「收藏 → 字母 → #」分组。
 * 收藏商品只进“收藏”组，字母组只含非收藏商品，避免同一商品出现在两个分组。
 */
internal fun groupProducts(products: List<Product>, letters: List<String>): List<ProductSection> {
    val favorites = products.filter { it.favorite }
    val byLetter = products.zip(letters).filterNot { (product, _) -> product.favorite }.groupBy { it.second }
    return buildList {
        if (favorites.isNotEmpty()) add(ProductSection("收藏", favorites))
        byLetter.keys.filter { it != "#" }.sorted().forEach { letter ->
            byLetter[letter]?.let { add(ProductSection(letter, it.map { pair -> pair.first })) }
        }
        byLetter["#"]?.let { add(ProductSection("#", it.map { pair -> pair.first })) }
    }
}

@Composable
fun ProductsScreen(
    onSelectionModeChange: (Boolean) -> Unit = {},
    scrollToTopTick: Int = 0,
    viewModel: ProductsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val products by viewModel.products.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) listState.animateScrollToItem(0)
    }
    val letters = remember(products) { products.map { it.pinyinInitial } }
    val grouped = remember(products, letters) { groupProducts(products, letters) }
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
                index += 1 + section.products.size
            }
        }
    }
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showTemplate by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Product?>(null) }

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
    val exportError = viewModel.exportError
    LaunchedEffect(exportError) {
        exportError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeExportError()
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importFromUri(uri)
    }

    val shareJsonUri = viewModel.shareJsonUri
    LaunchedEffect(shareJsonUri) {
        shareJsonUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享商品 JSON"))
            viewModel.consumeShareJson()
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
                            products.isNotEmpty() && products.all { it.id in viewModel.selectedIds }
                        TextButton(
                            text = if (allVisibleSelected) "取消全选" else "全选",
                            onClick = { viewModel.toggleSelectAll() },
                            modifier = Modifier.testTag("select_all_toggle"),
                        )
                    },
                )
            } else {
            SearchableTopBar(
                title = "快贝智单",
                searchPlaceholder = "搜索名称/规格",
                query = viewModel.queryText,
                onQueryChange = viewModel::setQuery,
                actions = {
                    OverlayIconDropdownMenu(
                        entry = DropdownEntry(
                            items = listOf(
                                DropdownItem(
                                    text = "导出 JSON",
                                    onClick = {
                                        showMenu = false
                                        viewModel.exportProducts()
                                    },
                                ),
                                DropdownItem(
                                    text = "导入 JSON",
                                    onClick = {
                                        showMenu = false
                                        showImportDialog = true
                                    },
                                ),
                                DropdownItem(
                                    text = "从剪贴板导入",
                                    onClick = {
                                        showMenu = false
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val text = cm.primaryClip
                                            ?.getItemAt(0)
                                            ?.coerceToText(context)
                                            ?.toString()
                                            ?.trim()
                                        viewModel.importFromClipboard(text.orEmpty())
                                    },
                                ),
                            )
                        ),
                        onExpandedChange = { showMenu = it },
                    ) {
                        Icon(MiuixIcons.More, contentDescription = "更多")
                    }
                },
            )
            }
        },
        floatingActionButton = {
            if (!viewModel.selectionMode) {
                FloatingActionButton(
                    onClick = { showNewDialog = true },
                    modifier = Modifier
                        .pressable(interactionSource = remember { MutableInteractionSource() })
                        // 底部导航栏改为覆盖层后，FAB 手动抬到栏上方
                        .padding(bottom = 84.dp),
                ) {
                    Icon(MiuixIcons.Add, contentDescription = "新增商品")
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
        if (viewModel.importing) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "正在导入…",
                        style = AppThemeTypography.bodySmall,
                        color = AppThemeColors.onSurfaceVariant,
                    )
                }
            }
        } else if (products.isEmpty()) {
            EmptyState(
                icon = MiuixIcons.Edit,
                text = if (viewModel.queryText.isNotBlank()) "没有找到匹配的商品" else "还没有商品，点击右下角新增",
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
                                    section.products.isNotEmpty() &&
                                        section.products.all { it.id in viewModel.selectedIds },
                                onSelectGroup = if (viewModel.selectionMode) {
                                    { viewModel.selectGroup(section.products.map { it.id }.toSet()) }
                                } else {
                                    null
                                },
                            )
                        }
                        items(
                            section.products,
                            key = { it.id },
                            contentType = { "productCard" },
                        ) { product ->
                        // 整行可点击（含头像与单价区域），长按进入多选
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (viewModel.selectionMode) {
                                            viewModel.toggleSelection(product.id)
                                        } else {
                                            detail = product
                                        }
                                    },
                                    onLongClick = {
                                        if (viewModel.selectionMode) {
                                            viewModel.toggleSelection(product.id)
                                        } else {
                                            viewModel.enterSelection(product.id)
                                        }
                                    },
                                )
                                .padding(start = 0.dp, end = 32.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (viewModel.selectionMode) {
                                Checkbox(
                                    state = if (product.id in viewModel.selectedIds) ToggleableState.On else ToggleableState.Off,
                                    onClick = { viewModel.toggleSelection(product.id) },
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            InitialCircle(product.name.trim().firstOrNull()?.toString() ?: "?")
                            Spacer(Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(product.name, style = AppThemeTypography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = listOf(product.spec, product.unit)
                                        .filter { it.isNotBlank() }
                                        .joinToString("  "),
                                    style = AppThemeTypography.bodySmall,
                                    color = AppThemeColors.onSurfaceVariant,
                                )
                            }
                            Text(
                                "¥${Money.format(product.price)}",
                                style = AppThemeTypography.titleSmall,
                                color = AppThemeColors.primary,
                            )

                        }
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
        ProductEditDialog(
            initial = Product(name = "", unit = "桶", price = 0.0),
            onSave = { product ->
                viewModel.saveProduct(product)
                showNewDialog = false
            },
            onDismiss = { showNewDialog = false },
        )
    }

    detail?.let { product ->
        ProductDetailDialog(
            product = product,
            onToggleFavorite = { newValue ->
                val updated = product.copy(favorite = newValue)
                viewModel.saveProduct(updated)
                detail = updated
            },
            onEdit = {
                detail = null
                editing = product
            },
            onDismiss = { detail = null },
        )
    }

    editing?.let { product ->
        ProductEditDialog(
            initial = product,
            onSave = { updated ->
                viewModel.saveProduct(updated)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除商品",
            destructive = true,
            text = "确定删除选中的 ${viewModel.selectedIds.size} 条商品吗？此操作不可恢复。",
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

    if (showImportDialog) {
        OverlayDialog(
            title = "导入 JSON",
            summary = "从 JSON 文件批量导入商品。支持 application/json 或文本文件，文件不超过 2MB。",
            show = true,
            onDismissRequest = { showImportDialog = false },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = "查看模板",
                    onClick = {
                        showImportDialog = false
                        showTemplate = true
                    },
                )
                TextButton(
                    text = "选择文件",
                    onClick = {
                        showImportDialog = false
                        filePicker.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }

    if (showTemplate) {
        OverlayDialog(
            title = "JSON 模板",
            show = true,
            onDismissRequest = { showTemplate = false },
        ) {
            Column {
                Text(
                    text = ProductJsonCodec.templateText(),
                    fontFamily = FontFamily.Monospace,
                    style = AppThemeTypography.bodySmall,
                    modifier = Modifier.heightIn(max = 220.dp),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    text = "复制模板",
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("json", ProductJsonCodec.templateText()))
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        text = "关闭",
                        onClick = { showTemplate = false },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }

    viewModel.importResult?.let { result ->
        val failureText = result.failures.take(20).joinToString("\n") { "第${it.first}行：${it.second}" }
        OverlayDialog(
            title = "导入完成",
            show = true,
            onDismissRequest = viewModel::consumeImportResult,
        ) {
            Column {
                Text("成功 ${result.success} 条 / 跳过重复 ${result.skipped} 条 / 失败 ${result.failures.size} 条")
                if (failureText.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        failureText,
                        style = AppThemeTypography.bodySmall,
                        color = AppThemeColors.error,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        text = "完成",
                        onClick = viewModel::consumeImportResult,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }

    viewModel.importError?.let { message ->
        OverlayDialog(
            title = "导入失败",
            summary = message,
            show = true,
            onDismissRequest = viewModel::consumeImportError,
        ) {
            TextButton(
                text = "完成",
                onClick = viewModel::consumeImportError,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }

    viewModel.exportFileName?.let { fileName ->
        OverlayDialog(
            title = "导出完成",
            summary = "已保存到下载目录：$fileName",
            show = true,
            onDismissRequest = viewModel::consumeExport,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = "分享",
                    onClick = {
                        viewModel.shareExportedJson()
                        viewModel.consumeExport()
                    },
                )
                TextButton(
                    text = "完成",
                    onClick = viewModel::consumeExport,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }

    viewModel.exportError?.let { message ->
        OverlayDialog(
            title = "导出失败",
            summary = message,
            show = true,
            onDismissRequest = viewModel::consumeExport,
        ) {
            TextButton(
                text = "完成",
                onClick = viewModel::consumeExport,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
private fun ProductDetailDialog(
    product: Product,
    onToggleFavorite: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        title = "商品详情",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column {
            DetailLine("名称", product.name)
            if (product.spec.isNotBlank()) DetailLine("规格", product.spec)
            if (product.unit.isNotBlank()) DetailLine("单位", product.unit)
            DetailLine("单价", "¥${Money.format(product.price)}")
            if (product.pack.isNotBlank()) DetailLine("包装规格", product.pack)
            if (product.note.isNotBlank()) DetailLine("备注", product.note)
            Spacer(Modifier.height(6.dp))
            LabeledSwitch("收藏", product.favorite, onToggleFavorite)
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
private fun ProductEditDialog(
    initial: Product,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var spec by remember { mutableStateOf(initial.spec) }
    var unit by remember { mutableStateOf(initial.unit) }
    var price by remember { mutableStateOf(if (initial.price == 0.0) "" else Money.format(initial.price)) }
    var pack by remember { mutableStateOf(initial.pack) }
    var note by remember { mutableStateOf(initial.note) }
    var favorite by remember { mutableStateOf(initial.favorite) }
    var error by remember { mutableStateOf<String?>(null) }

    OverlayDialog(
        title = if (initial.id == 0L) "新增商品" else "编辑商品",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            LabeledField(
                "名称*",
                name,
                { if (it.length <= InputLimits.NAME) name = it },
                modifier = Modifier.testTag("product_name"),
            )
            Spacer(Modifier.height(12.dp))
            LabeledField("规格", spec, { if (it.length <= InputLimits.SPEC) spec = it })
            Spacer(Modifier.height(12.dp))
            LabeledField("单位", unit, { if (it.length <= InputLimits.UNIT) unit = it })
            Spacer(Modifier.height(12.dp))
            LabeledField(
                "单价*",
                price,
                { price = it },
                modifier = Modifier.testTag("product_price"),
                keyboardType = KeyboardType.Decimal,
            )
            Spacer(Modifier.height(12.dp))
            LabeledField("包装规格", pack, { if (it.length <= InputLimits.PACK) pack = it })
            Spacer(Modifier.height(12.dp))
            LabeledField("备注", note, { if (it.length <= InputLimits.REMARK) note = it })
            Spacer(Modifier.height(12.dp))
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
                    val priceValue = price.toDoubleOrNull()
                    error = when {
                        name.isBlank() -> "名称不能为空"
                        priceValue == null || priceValue < 0 -> "单价必须是非负数字"
                        else -> null
                    }
                    if (error == null) {
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                spec = spec.trim(),
                                unit = unit.trim().ifBlank { "桶" },
                                price = priceValue ?: 0.0,
                                pack = pack.trim(),
                                note = note.trim(),
                                favorite = favorite,
                            )
                        )
                    }
                },
            )
        }
    }
}
