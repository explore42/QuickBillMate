package com.example.quickbillmate.ui.products

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.importexport.ProductJsonCodec
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.EmptyState
import com.example.quickbillmate.ui.common.GroupSectionHeader
import com.example.quickbillmate.ui.common.IndexSection
import com.example.quickbillmate.ui.common.InitialCircle
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LabeledSwitch
import com.example.quickbillmate.ui.common.LetterIndexBar
import com.example.quickbillmate.ui.common.SearchableTopBar
import com.example.quickbillmate.util.Money
import com.example.quickbillmate.util.Pinyin

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
    scrollToTopTick: Int = 0,
    viewModel: ProductsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val products by viewModel.products.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) listState.animateScrollToItem(0)
    }
    val letters = remember(products) { products.map { Pinyin.firstLetter(it.name) } }
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
    var pendingDelete by remember { mutableStateOf<Product?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showTemplate by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            SearchableTopBar(
                title = "商品",
                searchPlaceholder = "搜索名称/规格",
                query = viewModel.queryText,
                onQueryChange = viewModel::setQuery,
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("导出 JSON") },
                            onClick = {
                                showMenu = false
                                viewModel.exportProducts()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("导入 JSON") },
                            onClick = {
                                showMenu = false
                                showImportDialog = true
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新增商品")
            }
        },
    ) { padding ->
        if (products.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Edit,
                text = if (viewModel.queryText.isNotBlank()) "没有找到匹配的商品" else "还没有商品，点击右下角新增",
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
                            )
                        }
                        items(
                            section.products,
                            key = { it.id },
                            contentType = { "productCard" },
                        ) { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 0.dp, end = 32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            InitialCircle(product.name.trim().firstOrNull()?.toString() ?: "?")
                            Spacer(Modifier.width(12.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .combinedClickable(
                                        onClick = { editing = product },
                                        onLongClick = { pendingDelete = product },
                                    )
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(product.name, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = listOf(product.spec, product.unit)
                                        .filter { it.isNotBlank() }
                                        .joinToString("  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "¥${Money.format(product.price)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
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

    pendingDelete?.let { product ->
        ConfirmDialog(
            title = "删除商品",
            text = "确定删除商品“${product.name}”吗？",
            onConfirm = {
                viewModel.deleteProduct(product)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入 JSON") },
            text = { Text("从 JSON 文件批量导入商品。支持 application/json 或文本文件，文件不超过 2MB。") },
            confirmButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    filePicker.launch(arrayOf("application/json", "text/plain", "*/*"))
                }) { Text("选择文件") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    showTemplate = true
                }) { Text("查看模板") }
            },
        )
    }

    if (showTemplate) {
        AlertDialog(
            onDismissRequest = { showTemplate = false },
            title = { Text("JSON 模板") },
            text = {
                Column {
                    Text(
                        text = ProductJsonCodec.templateText(),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.height(220.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("json", ProductJsonCodec.templateText()))
                    }) {
                        Text("复制模板")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplate = false }) { Text("关闭") }
            },
        )
    }

    viewModel.importResult?.let { result ->
        val failureText = result.failures.take(20).joinToString("\n") { "第${it.first}行：${it.second}" }
        AlertDialog(
            onDismissRequest = viewModel::consumeImportResult,
            title = { Text("导入完成") },
            text = {
                Column {
                    Text("成功 ${result.success} 条 / 跳过重复 ${result.skipped} 条 / 失败 ${result.failures.size} 条")
                    if (failureText.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            failureText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::consumeImportResult) { Text("完成") }
            },
        )
    }

    viewModel.importError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::consumeImportError,
            title = { Text("导入失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeImportError) { Text("完成") }
            },
        )
    }

    viewModel.exportFileName?.let { fileName ->
        AlertDialog(
            onDismissRequest = viewModel::consumeExport,
            title = { Text("导出完成") },
            text = { Text("已保存到下载目录：$fileName") },
            confirmButton = {
                TextButton(onClick = viewModel::consumeExport) { Text("完成") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.shareExportedJson()
                    viewModel.consumeExport()
                }) { Text("分享") }
            },
        )
    }

    viewModel.exportError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::consumeExport,
            title = { Text("导出失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeExport) { Text("完成") }
            },
        )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "新增商品" else "编辑商品") },
        text = {
            Column {
                LabeledField(
                    "名称*",
                    name,
                    { name = it },
                    modifier = Modifier.testTag("product_name"),
                )
                Spacer(Modifier.height(8.dp))
                LabeledField("规格", spec, { spec = it })
                Spacer(Modifier.height(8.dp))
                LabeledField("单位", unit, { unit = it })
                Spacer(Modifier.height(8.dp))
                LabeledField(
                    "单价*",
                    price,
                    { price = it },
                    modifier = Modifier.testTag("product_price"),
                    keyboardType = KeyboardType.Decimal,
                )
                Spacer(Modifier.height(8.dp))
                LabeledField("包装规格", pack, { pack = it })
                Spacer(Modifier.height(8.dp))
                LabeledField("备注", note, { note = it })
                Spacer(Modifier.height(8.dp))
                LabeledSwitch("收藏", favorite, { favorite = it })
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
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
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
