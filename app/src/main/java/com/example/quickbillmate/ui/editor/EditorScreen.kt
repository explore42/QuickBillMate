package com.example.quickbillmate.ui.editor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LabeledSwitch
import com.example.quickbillmate.ui.common.SectionCard
import com.example.quickbillmate.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    billId: Long,
    onBack: () -> Unit,
    onManagePresets: () -> Unit,
    viewModel: EditorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(Unit) {
        if (billId == 0L) viewModel.createNew() else viewModel.load(billId)
    }

    val s = viewModel.state
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showPresetPicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }
    var storageDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onContactsPermission(granted)
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.exportToGallery() else storageDenied = true
    }

    fun doExport() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.exportToGallery() else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            viewModel.exportToGallery()
        }
    }

    val shareOutcome = s.shareOutcome
    LaunchedEffect(shareOutcome) {
        shareOutcome?.let { outcome ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, outcome.shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享清单"))
            viewModel.consumeShareOutcome()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (billId == 0L) "新建销售清单" else "编辑销售清单") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showPresetPicker = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "样式预设")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { doExport() },
                    modifier = Modifier.weight(1.4f),
                    enabled = !s.exporting,
                ) {
                    Text(if (s.exporting) "导出中…" else "导出图片")
                }
                OutlinedButton(
                    onClick = { viewModel.shareNow() },
                    modifier = Modifier.weight(1f),
                    enabled = !s.exporting,
                ) {
                    Text("分享")
                }
                OutlinedButton(
                    onClick = { viewModel.loadSample() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_sample"),
                ) {
                    Text("示例")
                }
                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("清空")
                }
            }
        },
    ) { padding ->
        if (!s.loaded) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 客户信息
                SectionCard("客户信息") {
                    CustomerField(
                        value = s.customerName,
                        onChange = viewModel::onCustomerNameChange,
                        suggestions = s.suggestions,
                        contactsGranted = s.contactsGranted,
                        onGrantContacts = {
                            permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        },
                        onSelect = viewModel::selectSuggestion,
                    )
                    Spacer(Modifier.height(8.dp))
                    LabeledField(
                        label = "客户电话",
                        value = s.customerPhone,
                        onChange = viewModel::onCustomerPhoneChange,
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 公司信息
                SectionCard("公司信息") {
                    LabeledField(
                        label = "公司名称",
                        value = s.companyName,
                        onChange = viewModel::onCompanyNameChange,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LabeledField(
                        label = "联系电话",
                        value = s.contactPhone,
                        onChange = viewModel::onContactPhoneChange,
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LabeledField(
                        label = "业务经理",
                        value = s.salesManager,
                        onChange = viewModel::onManagerChange,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 客单信息
                SectionCard("客单信息") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledField(
                            label = "编号代码",
                            value = s.docCode,
                            onChange = viewModel::onDocCodeChange,
                            modifier = Modifier.weight(1f),
                        )
                        LabeledField(
                            label = "流水号",
                            value = s.docSerial,
                            onChange = viewModel::onSerialChange,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.regenerateSerial() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "重新生成流水号")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = s.docDate,
                        onValueChange = {},
                        label = { Text("单据日期") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "选择日期")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LabeledField(
                        label = "优惠金额",
                        value = s.discountText,
                        onChange = viewModel::onDiscountChange,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LabeledField(
                        label = "备注",
                        value = s.remark,
                        onChange = viewModel::onRemarkChange,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LabeledField(
                        label = "清单标题后缀",
                        value = s.titleSuffix,
                        onChange = viewModel::onTitleSuffixChange,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LabeledField(
                        label = "底部说明文案",
                        value = s.disclaimer,
                        onChange = viewModel::onDisclaimerChange,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 商品信息
                SectionCard("商品信息") {
                    s.items.forEachIndexed { index, row ->
                        ItemRowEditor(
                            row = row,
                            index = index,
                            count = s.items.size,
                            onUpdate = { updated -> viewModel.updateItem(index) { updated } },
                            onRemove = { viewModel.removeItem(index) },
                            onMoveUp = { viewModel.moveItem(index, -1) },
                            onMoveDown = { viewModel.moveItem(index, 1) },
                        )
                        if (index != s.items.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { viewModel.addItem() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("添加商品行")
                        }
                        FilledTonalButton(
                            onClick = { showProductPicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_add_from_library"),
                        ) {
                            Text("从商品库添加")
                        }
                    }
                }

                // 显示选项
                SectionCard("显示选项") {
                    LabeledSwitch("显示业务经理", s.showManager, viewModel::onShowManagerChange)
                    LabeledSwitch("显示备注", s.showRemark, viewModel::onShowRemarkChange)
                    LabeledSwitch("显示水印", s.showWatermark, viewModel::onShowWatermarkChange)
                }

                // 清单预览
                PreviewCard(bitmap = s.preview)
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                LocalDate.parse(s.docDate)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .toString()
                        viewModel.onDateChange(date)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showPresetPicker) {
        PresetPickerDialog(
            currentKey = s.presetKey,
            presets = s.presets,
            onSelect = { key ->
                viewModel.selectPreset(key)
                showPresetPicker = false
            },
            onManage = {
                showPresetPicker = false
                onManagePresets()
            },
            onDismiss = { showPresetPicker = false },
        )
    }

    if (showProductPicker) {
        ProductPickerSheet(
            products = s.products,
            onPick = { product ->
                viewModel.addProductToItems(product)
                showProductPicker = false
            },
            onDismiss = { showProductPicker = false },
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "清空表单",
            text = "确定清空所有字段与商品行吗？",
            confirmText = "清空",
            onConfirm = {
                viewModel.clearAll()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false },
        )
    }

    if (storageDenied) {
        AlertDialog(
            onDismissRequest = { storageDenied = false },
            title = { Text("需要存储权限") },
            text = { Text("保存图片到相册需要存储权限，请到系统设置中授权后重试。") },
            confirmButton = {
                TextButton(onClick = { storageDenied = false }) { Text("知道了") }
            },
        )
    }

    s.exportOutcome?.let { outcome ->
        AlertDialog(
            onDismissRequest = viewModel::consumeExportOutcome,
            title = { Text(if (outcome.saved) "导出成功" else "导出失败") },
            text = { Text(outcome.message) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeExportOutcome) { Text("完成") }
            },
            dismissButton = {
                if (outcome.saved && outcome.shareUri != null) {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, outcome.shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享清单"))
                        viewModel.consumeExportOutcome()
                    }) { Text("分享") }
                }
            },
        )
    }
}

@Composable
private fun CustomerField(
    value: String,
    onChange: (String) -> Unit,
    suggestions: List<CustomerSuggestion>,
    contactsGranted: Boolean,
    onGrantContacts: () -> Unit,
    onSelect: (CustomerSuggestion) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text("客户名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (suggestions.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    suggestions.forEach { suggestion ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(suggestion) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    suggestion.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (suggestion.fromContacts) {
                                    Text(
                                        "通讯录",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            if (suggestion.phone.isNotBlank() || suggestion.type.isNotBlank()) {
                                Text(
                                    listOf(suggestion.type, suggestion.phone)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        } else if (!contactsGranted) {
            Text(
                text = "开启通讯录权限可联想联系人",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onGrantContacts) { Text("授权通讯录") }
        }
    }
}

@Composable
private fun ItemRowEditor(
    row: ItemRow,
    index: Int,
    count: Int,
    onUpdate: (ItemRow) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "第 ${index + 1} 行",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
            }
            IconButton(onClick = onMoveDown, enabled = index < count - 1) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "删除行")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RowField("名称", row.name, { onUpdate(row.copy(name = it)) }, Modifier.width(130.dp))
            RowField("规格", row.spec, { onUpdate(row.copy(spec = it)) }, Modifier.width(100.dp))
            RowField("单位", row.unit, { onUpdate(row.copy(unit = it)) }, Modifier.width(70.dp))
            RowField("数量", row.qtyText, { onUpdate(row.copy(qtyText = it)) }, Modifier.width(80.dp), KeyboardType.Decimal)
            RowField("单价", row.priceText, { onUpdate(row.copy(priceText = it)) }, Modifier.width(90.dp), KeyboardType.Decimal)
            RowField("包装", row.pack, { onUpdate(row.copy(pack = it)) }, Modifier.width(100.dp))
            RowField("备注", row.note, { onUpdate(row.copy(note = it)) }, Modifier.width(100.dp))
            Column(modifier = Modifier.width(90.dp)) {
                Text(
                    text = "金额",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = Money.format(row.amount()),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun RowField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
    )
}

@Composable
private fun PreviewCard(bitmap: android.graphics.Bitmap?) {
    var expanded by remember { mutableStateOf(true) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) "清单预览（点击折叠）" else "清单预览（点击展开）",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "清单预览",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_preview"),
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetPickerDialog(
    currentKey: String,
    presets: List<StylePreset>,
    onSelect: (String) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择样式预设") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("内置预设", style = MaterialTheme.typography.labelMedium)
                StylePresets.builtIns.forEach { preset ->
                    PresetRow(
                        name = preset.name,
                        tag = "内置",
                        selected = currentKey == preset.key,
                        onClick = { onSelect(preset.key) },
                    )
                }
                if (presets.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Text("我的预设", style = MaterialTheme.typography.labelMedium)
                    presets.forEach { preset ->
                        PresetRow(
                            name = preset.name,
                            tag = "自定义",
                            selected = currentKey == "custom:${preset.id}",
                            onClick = { onSelect("custom:${preset.id}") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onManage) { Text("管理预设") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun PresetRow(
    name: String,
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(name, modifier = Modifier.weight(1f))
        Text(
            tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductPickerSheet(
    products: List<Product>,
    onPick: (Product) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    val filtered = remember(products, query) {
        if (query.isBlank()) products else products.filter {
            it.name.contains(query.trim()) || it.spec.contains(query.trim())
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("从商品库添加", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索商品") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.height(360.dp),
            ) {
                items(filtered.size, key = { filtered[it].id }) { index ->
                    val p = filtered[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(p) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                listOf(p.spec, p.unit).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "¥${Money.format(p.price)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
