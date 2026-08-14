package com.example.quickbillmate.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.Product
import com.example.quickbillmate.data.db.StylePreset
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.DefaultInfoForm
import com.example.quickbillmate.ui.common.DefaultInfoValues
import com.example.quickbillmate.ui.common.ConfirmDialog
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LabeledSwitch
import com.example.quickbillmate.ui.common.PhoneListEditor
import com.example.quickbillmate.ui.common.SectionCard
import com.example.quickbillmate.util.Money
import com.example.quickbillmate.util.InputLimits
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
    var showPresetDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onContactsPermission(granted)
    }

    // 返回 = 立即自动保存后退出；流水号非法时弹“放弃修改?”确认
    BackHandler(enabled = s.loaded) {
        viewModel.saveOnExit(onBack, onInvalid = { showDiscardConfirm = true })
    }



    Scaffold(
        topBar = {
            AppTopBar(
                title = if (billId == 0L) "新建单据" else "编辑单据",
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveOnExit(onBack, onInvalid = { showDiscardConfirm = true })
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showPresetDialog = true }) {
                        Text("图片样式")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // 键盘弹出时把“不保存/保存”操作栏抬到输入法上方，避免被遮挡
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showDiscardConfirm = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("不保存")
                }
                Button(
                    onClick = { viewModel.saveNow(onBack) },
                    modifier = Modifier.weight(1.4f),
                ) {
                    Text("保存")
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
                    Text("客户电话", style = MaterialTheme.typography.titleSmall)
                    val editorPhones = remember(s.customerPhone) {
                        val parts = s.customerPhone.split(",").map { it.trim() }
                        if (parts.all { it.isEmpty() }) listOf("") else parts
                    }
                    PhoneListEditor(
                        phones = editorPhones,
                        onChange = { list -> viewModel.onCustomerPhoneChange(list.joinToString(",")) },
                    )
                    val phoneHint = when {
                        editorPhones.size == 1 -> "可添加多个电话"
                        editorPhones.size > 1 && !s.showMultiPhones -> "仅显示第一个电话（可在右上角设置开启全部）"
                        else -> null
                    }
                    if (phoneHint != null) {
                        Text(
                            phoneHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 客单信息
                SectionCard("客单信息") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LabeledField(
                            label = "流水号",
                            value = s.docSerial,
                            onChange = viewModel::onSerialChange,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { if (!it.isFocused) viewModel.validateSerial() },
                            isError = s.serialError != null,
                            supportingText = s.serialError,
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
                    LabeledSwitch("收藏", s.favorite, viewModel::onFavoriteChange)
                }

                // 商品信息
                SectionCard("商品信息") {
                    val totalCents = s.items.sumOf { Math.round(it.amount() * 100.0) }
                    val discountCents = Math.round((s.discountText.toDoubleOrNull() ?: 0.0) * 100.0)
                    val receivableCents = totalCents - discountCents
                    val jiaoFenCents = receivableCents % 100
                    val yuanOnesCents = receivableCents % 1000
                    val currentDiscount = s.discountText.toDoubleOrNull() ?: 0.0
                    // 三步抹零：先抹分/角，再抹元，最后清除抹零
                    val roundDownLabel: String
                    val roundDownAction: (() -> Unit)?
                    when {
                        jiaoFenCents != 0L -> {
                            roundDownLabel = "抹分角"
                            roundDownAction = {
                                viewModel.onDiscountChange(Money.format((discountCents + jiaoFenCents) / 100.0))
                            }
                        }
                        yuanOnesCents != 0L -> {
                            roundDownLabel = "抹元"
                            roundDownAction = {
                                viewModel.onDiscountChange(Money.format((discountCents + yuanOnesCents) / 100.0))
                            }
                        }
                        currentDiscount != 0.0 -> {
                            roundDownLabel = "清除抹零"
                            roundDownAction = { viewModel.onDiscountChange(Money.format(0.0)) }
                        }
                        else -> {
                            roundDownLabel = "抹零"
                            roundDownAction = null
                        }
                    }
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
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LabeledField(
                            label = "优惠",
                            value = s.discountText,
                            onChange = viewModel::onDiscountChange,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = { roundDownAction?.invoke() },
                            enabled = roundDownAction != null,
                        ) {
                            Text(roundDownLabel)
                        }
                    }
                }

                // 单据预览
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

    if (showPresetDialog) {
        PresetPickerDialog(
            currentKey = s.presetKey,
            presets = s.presets,
            onSelect = { key ->
                viewModel.selectPreset(key)
                showPresetDialog = false
            },
            onManage = {
                showPresetDialog = false
                onManagePresets()
            },
            onDismiss = { showPresetDialog = false },
        )
    }

    if (showSettingsDialog) {
        EditorDefaultInfoDialog(
            values = s.toDefaultInfoValues(),
            onSave = { values ->
                viewModel.applyDefaultInfoValues(values)
                viewModel.saveNow { showSettingsDialog = false }
            },
            onDismiss = { showSettingsDialog = false },
        )
    }

    if (showDiscardConfirm) {
        ConfirmDialog(
            title = "放弃修改？",
            text = "未保存的修改将被丢弃，且无法恢复。",
            confirmText = "放弃",
            onConfirm = {
                showDiscardConfirm = false
                viewModel.discardChanges(onBack)
            },
            onDismiss = { showDiscardConfirm = false },
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
}

/** 客户名称：既是输入框也是下拉框。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerField(
    value: String,
    onChange: (String) -> Unit,
    suggestions: List<CustomerSuggestion>,
    contactsGranted: Boolean,
    onGrantContacts: () -> Unit,
    onSelect: (CustomerSuggestion) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    val menuOpen = expanded && suggestions.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    if (it.length <= InputLimits.NAME) {
                        onChange(it)
                        expanded = true
                    }
                },
                label = { Text("客户名称") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { fieldWidth = it.width }
                    .onFocusChanged { focused -> expanded = focused.isFocused },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = if (menuOpen) "收起" else "展开",
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
                ) {
                    Surface(
                        modifier = Modifier.width(popupWidth),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(suggestions) { suggestion ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    suggestion.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                if (suggestion.fromDb) {
                                                    SuggestionTag(text = "客户库")
                                                }
                                            }
                                            val subtitle = listOf(suggestion.type, suggestion.phone)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" · ")
                                            if (subtitle.isNotBlank()) {
                                                Text(
                                                    subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSelect(suggestion)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (!contactsGranted) {
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
private fun SuggestionTag(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** 右上角“图片样式”选择弹窗：选择预设，可进入管理页。 */
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
        title = { Text("选择图片样式") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PresetInlineList(
                    currentKey = currentKey,
                    presets = presets,
                    onSelect = onSelect,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        dismissButton = {
            TextButton(onClick = onManage) { Text("管理") }
        },
    )
}

/** 右上角“设置”弹窗：复用默认信息表单，仅修改当前单据。 */
@Composable
private fun EditorDefaultInfoDialog(
    values: DefaultInfoValues,
    onSave: (DefaultInfoValues) -> Unit,
    onDismiss: () -> Unit,
) {
    var local by remember(values) { mutableStateOf(values) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DefaultInfoForm(values = local, onChange = { local = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(local) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun PresetInlineList(
    currentKey: String,
    presets: List<StylePreset>,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(start = 4.dp)) {
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
            RowField("名称", row.name, { if (it.length <= InputLimits.NAME) onUpdate(row.copy(name = it)) }, Modifier.width(130.dp))
            RowField("规格", row.spec, { if (it.length <= InputLimits.SPEC) onUpdate(row.copy(spec = it)) }, Modifier.width(100.dp))
            RowField("单位", row.unit, { if (it.length <= InputLimits.UNIT) onUpdate(row.copy(unit = it)) }, Modifier.width(70.dp))
            RowField(
                "数量",
                row.qtyText,
                { value -> onUpdate(row.copy(qtyText = value.filter(Char::isDigit))) },
                Modifier.width(80.dp),
                KeyboardType.Number,
            )
            RowField("单价", row.priceText, { onUpdate(row.copy(priceText = it)) }, Modifier.width(90.dp), KeyboardType.Decimal)
            RowField("包装", row.pack, { if (it.length <= InputLimits.PACK) onUpdate(row.copy(pack = it)) }, Modifier.width(100.dp))
            RowField("备注", row.note, { if (it.length <= InputLimits.REMARK) onUpdate(row.copy(note = it)) }, Modifier.width(100.dp))
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "单据预览",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "单据预览",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_preview"),
                )
            }
        }
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
