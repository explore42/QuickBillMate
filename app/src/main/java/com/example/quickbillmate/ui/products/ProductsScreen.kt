package com.example.quickbillmate.ui.products

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.Ds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.DetailLine
import com.example.quickbillmate.ui.common.DialogButtons
import com.example.quickbillmate.ui.common.DialogScrollColumn
import com.example.quickbillmate.ui.common.EmptyState
import com.example.quickbillmate.ui.common.GroupSectionHeader
import com.example.quickbillmate.ui.common.IndexSection
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.InitialCircle
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LabeledSwitch
import com.example.quickbillmate.ui.common.LetterIndexBar
import com.example.quickbillmate.ui.common.SelectionActionBar
import com.example.quickbillmate.ui.common.LocalHaptics
import com.example.quickbillmate.ui.common.SearchableTopBar
import com.example.quickbillmate.ui.common.TopBarActionTextButton
import com.example.quickbillmate.util.Money
import com.example.quickbillmate.util.InputLimits
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.utils.SinkFeedback
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
    // 进入商品页时刷新单位预设（设置页可能刚修改过）
    LaunchedEffect(Unit) { viewModel.refreshUnitPresets() }
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
    var editing by remember { mutableStateOf<Product?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
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
                searchPlaceholder = "搜索名称/规格",
                query = viewModel.queryText,
                onQueryChange = viewModel::setQuery,
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
                    Icon(MiuixIcons.Add, contentDescription = "新增商品")
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
                    onExport = null,
                    onDelete = {
                        viewModel.requestDelete()
                        showDeleteConfirm = true
                    },
                )
            }
        },
    ) { padding ->
        if (products.isEmpty()) {
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
                        // 整行可点击（含头像与单价区域），长按进入多选；圆角涟漪+下沉按压
                        val interactionSource = remember { MutableInteractionSource() }
                        val haptics = LocalHaptics.current
                        Row(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .indication(interactionSource, SinkFeedback(sinkAmount = 0.97f))
                                .combinedClickable(
                                    interactionSource = interactionSource,
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
                                            haptics.longPress()
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
            initial = Product(name = "", price = 0.0),
            unitOptions = viewModel.presetUnits,
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
            unitOptions = viewModel.presetUnits,
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

}

/** 单位输入：可自由输入，聚焦或点箭头时下拉展示预置单位（内置 + 设置页自定义）。 */
@Composable
private fun UnitField(
    value: String,
    options: List<String>,
    onChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    val menuOpen = expanded && options.isNotEmpty()

    Box {
        TextField(
            value = value,
            onValueChange = { if (it.length <= InputLimits.UNIT) onChange(it) },
            label = "单位",
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fieldWidth = it.width }
                .onFocusChanged { focused -> expanded = focused.isFocused },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        MiuixIcons.ExpandMore,
                        contentDescription = if (menuOpen) "收起单位" else "展开单位",
                    )
                }
            },
        )
        if (menuOpen) {
            val popupWidth = with(LocalDensity.current) { fieldWidth.toDp() }
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(LocalDensity.current) { 64.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false),
            ) {
                Surface(
                    modifier = Modifier.width(popupWidth),
                    shape = RoundedCornerShape(Ds.md),
                    color = AppThemeColors.surfaceContainerHighest,
                    shadowElevation = 8.dp,
                ) {
                    // 选项可能多于可视高度（内置 + 自定义），用可滚动列表，自定义单位在尾部可达
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(options) { unit ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChange(unit)
                                        expanded = false
                                    }
                                    .padding(horizontal = Ds.md, vertical = 10.dp),
                            ) {
                                Text(unit, style = AppThemeTypography.bodyMedium)
                            }
                        }
                    }
                }
            }
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
    unitOptions: List<String>,
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
        DialogScrollColumn {
            LabeledField(
                "名称*",
                name,
                { if (it.length <= InputLimits.NAME) name = it },
                modifier = Modifier.testTag("product_name"),
            )
            Spacer(Modifier.height(12.dp))
            LabeledField("规格", spec, { if (it.length <= InputLimits.SPEC) spec = it })
            Spacer(Modifier.height(12.dp))
            UnitField(
                value = unit,
                options = unitOptions,
                onChange = { unit = it },
            )
            Spacer(Modifier.height(12.dp))
            LabeledField(
                "单价*",
                price,
                { price = Money.sanitizeAmountInput(it) },
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
                                unit = unit.trim(),
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
