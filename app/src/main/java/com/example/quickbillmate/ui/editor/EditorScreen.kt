package com.example.quickbillmate.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.quickbillmate.ui.common.DatePickerDialog
import com.example.quickbillmate.ui.common.DefaultInfoForm
import com.example.quickbillmate.ui.common.DefaultInfoValues
import com.example.quickbillmate.ui.common.DialogButtons
import com.example.quickbillmate.ui.common.LabeledField
import com.example.quickbillmate.ui.common.LocalHaptics
import com.example.quickbillmate.ui.common.LabeledSwitch
import com.example.quickbillmate.ui.common.PhoneListEditor
import com.example.quickbillmate.ui.common.PhoneSectionHeader
import com.example.quickbillmate.ui.common.SectionCard
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.Ds
import com.example.quickbillmate.util.Money
import com.example.quickbillmate.util.InputLimits
import java.time.LocalDate
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Remove
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog

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
    val haptics = LocalHaptics.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onContactsPermission(granted)
    }

    // 系统返回与左上角 ✕ 一致：先弹「保存并离开 / 直接退出」确认
    BackHandler(enabled = s.loaded) {
        showExitConfirm = true
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (billId == 0L) "新建单据" else "编辑单据",
                navigationIcon = {
                    IconButton(onClick = { showExitConfirm = true }) {
                        Icon(MiuixIcons.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    // 紧凑实心主色按钮，比文本按钮更醒目；保存成功伴随确认触觉
                    Button(
                        onClick = {
                            haptics.confirm()
                            viewModel.saveNow(onBack)
                        },
                        enabled = s.loaded,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        minHeight = 34.dp,
                        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text("保存")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(MiuixIcons.Settings, contentDescription = "单据设置")
                    }
                },
            )
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
                    .padding(horizontal = Ds.screen, vertical = Ds.sm)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(Ds.md),
            ) {
                // 单据预览：置顶、无标题无卡片，保持清爽
                PreviewImage(bitmap = s.preview)

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
                    Spacer(Modifier.height(Ds.md))
                    val editorPhones = remember(s.customerPhone) {
                        val parts = s.customerPhone.split(",").map { it.trim() }
                        if (parts.all { it.isEmpty() }) listOf("") else parts
                    }
                    PhoneSectionHeader(
                        phoneCount = editorPhones.size,
                        onAdd = { viewModel.onCustomerPhoneChange((editorPhones + "").joinToString(",")) },
                    )
                    Spacer(Modifier.height(Ds.xs))
                    PhoneListEditor(
                        phones = editorPhones,
                        onChange = { list -> viewModel.onCustomerPhoneChange(list.joinToString(",")) },
                    )
                    val phoneHint = when {
                        !s.showCustomerPhone && editorPhones.any { it.isNotBlank() } ->
                            "单据不显示客户电话（可在单据设置开启）"
                        editorPhones.size > 1 && !s.showMultiPhones ->
                            "仅显示第一个电话（可在单据设置开启全部）"
                        else -> null
                    }
                    if (phoneHint != null) {
                        Text(
                            phoneHint,
                            style = AppThemeTypography.bodySmall,
                            color = AppThemeColors.onSurfaceVariant,
                        )
                    }
                }

                // 客单信息
                SectionCard("客单信息") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Ds.sm),
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
                            Icon(MiuixIcons.Refresh, contentDescription = "重新生成流水号")
                        }
                    }
                    Spacer(Modifier.height(Ds.md))
                    // 整行可点开日期选择（只读字段，点击即弹窗）
                    TextField(
                        value = s.docDate,
                        onValueChange = {},
                        label = "单据日期",
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(MiuixIcons.ExpandMore, contentDescription = "选择日期")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                    )
                    Spacer(Modifier.height(Ds.md))
                    LabeledSwitch("收藏", s.favorite, viewModel::onFavoriteChange)
                }

                // 商品信息
                SectionCard("商品信息") {
                    val (roundDownLabel, roundDownTarget) = viewModel.roundDownAction()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Ds.md),
                        modifier = Modifier.animateContentSize(),
                    ) {
                        s.items.forEachIndexed { index, row ->
                            ItemCardEditor(
                                row = row,
                                index = index,
                                onUpdate = { updated -> viewModel.updateItem(index) { updated } },
                                onRemove = { viewModel.removeItem(index) },
                            )
                        }
                        // 重要操作在右手侧：空行在左，从商品库添加在右
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Ds.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = { viewModel.addItem() },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Text("添加空行")
                            }
                            Button(
                                onClick = { showProductPicker = true },
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("editor_add_from_library")
                                    .heightIn(min = 48.dp),
                            ) {
                                Icon(MiuixIcons.Add, contentDescription = null)
                                Spacer(Modifier.width(Ds.xs))
                                Text("从商品库添加")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Ds.sm),
                        ) {
                            LabeledField(
                                label = "优惠金额",
                                value = s.discountText,
                                onChange = viewModel::onDiscountChange,
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                text = roundDownLabel,
                                onClick = { viewModel.applyRoundDown() },
                                enabled = roundDownTarget != null,
                                minHeight = Ds.buttonHeight,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Ds.sm))
            }
        }
    }

    if (showDatePicker) {
        val initial = runCatching { LocalDate.parse(s.docDate) }.getOrNull() ?: LocalDate.now()
        DatePickerDialog(
            initial = initial,
            onConfirm = { date ->
                viewModel.onDateChange(date.toString())
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showSettingsDialog) {
        EditorSettingsDialog(
            values = s.toDefaultInfoValues(),
            currentPresetKey = s.presetKey,
            presets = s.presets,
            onSelectPreset = viewModel::selectPreset,
            onManagePresets = {
                showSettingsDialog = false
                onManagePresets()
            },
            onSave = { values ->
                viewModel.applyDefaultInfoValues(values)
                viewModel.saveNow { showSettingsDialog = false }
            },
            onDismiss = { showSettingsDialog = false },
        )
    }

    if (showExitConfirm) {
        ExitConfirmDialog(
            onSaveAndExit = {
                showExitConfirm = false
                viewModel.saveNow(onBack)
            },
            onDiscardAndExit = {
                showExitConfirm = false
                viewModel.discardChanges(onBack)
            },
            onDismiss = { showExitConfirm = false },
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

/** 退出确认：保存并离开为主操作，直接退出为红色警示操作。 */
@Composable
private fun ExitConfirmDialog(
    onSaveAndExit: () -> Unit,
    onDiscardAndExit: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        title = "退出编辑？",
        summary = "「保存并离开」保留当前内容；「直接退出」将丢弃未保存的修改。",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        // 按钮优先级从左到右递增：保存并离开最宽、主色、在最右
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Ds.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "直接退出",
                    style = AppThemeTypography.bodyMedium,
                    color = AppThemeColors.error,
                    modifier = Modifier
                        .clickable(onClick = onDiscardAndExit)
                        .padding(vertical = 10.dp),
                )
            }
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                minHeight = Ds.buttonHeight,
            )
            Button(
                onClick = onSaveAndExit,
                modifier = Modifier.weight(1.4f),
                minHeight = Ds.buttonHeight,
            ) {
                Text("保存并离开")
            }
        }
    }
}

/** 单据预览：无标题、无卡片，仅圆角图片。 */
@Composable
private fun PreviewImage(bitmap: android.graphics.Bitmap?) {
    if (bitmap == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Ds.md))
            .testTag("editor_preview"),
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "单据预览",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 客户名称：既是输入框也是下拉框。 */
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
            TextField(
                value = value,
                onValueChange = {
                    if (it.length <= InputLimits.NAME) {
                        onChange(it)
                        expanded = true
                    }
                },
                label = "客户名称",
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { fieldWidth = it.width }
                    .onFocusChanged { focused -> expanded = focused.isFocused },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            MiuixIcons.ExpandMore,
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
                    properties = PopupProperties(focusable = false),
                ) {
                    Surface(
                        modifier = Modifier.width(popupWidth),
                        shape = RoundedCornerShape(Ds.md),
                        color = AppThemeColors.surfaceContainerHighest,
                        shadowElevation = 8.dp,
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(suggestions) { suggestion ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelect(suggestion)
                                            expanded = false
                                        }
                                        .padding(horizontal = Ds.md, vertical = 10.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            suggestion.name,
                                            style = AppThemeTypography.bodyMedium,
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
                                            style = AppThemeTypography.bodySmall,
                                            color = AppThemeColors.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!contactsGranted) {
            Text(
                text = "开启通讯录权限可联想联系人",
                style = AppThemeTypography.bodySmall,
                color = AppThemeColors.onSurfaceVariant,
            )
            TextButton(
                text = "授权通讯录",
                onClick = onGrantContacts,
            )
        }
    }
}

@Composable
private fun SuggestionTag(text: String) {
    Surface(
        shape = RoundedCornerShape(Ds.xs + 2.dp),
        color = AppThemeColors.secondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = AppThemeTypography.labelSmall,
            color = AppThemeColors.onSecondaryContainer,
        )
    }
}

/**
 * 「单据设置」弹窗：图片样式选择（含管理入口）+ 默认信息表单，仅作用于当前单据。
 */
@Composable
private fun EditorSettingsDialog(
    values: DefaultInfoValues,
    currentPresetKey: String,
    presets: List<StylePreset>,
    onSelectPreset: (String) -> Unit,
    onManagePresets: () -> Unit,
    onSave: (DefaultInfoValues) -> Unit,
    onDismiss: () -> Unit,
) {
    var local by remember(values) { mutableStateOf(values) }

    OverlayDialog(
        title = "单据设置",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            PresetInlineList(
                currentKey = currentPresetKey,
                presets = presets,
                onSelect = onSelectPreset,
                onManage = onManagePresets,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = Ds.sm))
            DefaultInfoForm(values = local, onChange = { local = it })
            Spacer(Modifier.height(Ds.md))
            DialogButtons(
                confirmText = "保存",
                onCancel = onDismiss,
                onConfirm = { onSave(local) },
            )
        }
    }
}

@Composable
private fun PresetInlineList(
    currentKey: String,
    presets: List<StylePreset>,
    onSelect: (String) -> Unit,
    onManage: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = Ds.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("图片样式", style = AppThemeTypography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                text = "管理预设 ›",
                style = AppThemeTypography.labelMedium,
                color = AppThemeColors.primary,
                modifier = Modifier.clickable(onClick = onManage),
            )
        }
        Spacer(Modifier.height(Ds.xs))
        StylePresets.builtIns.forEach { preset ->
            PresetRow(
                name = preset.name,
                tag = "内置",
                selected = currentKey == preset.key,
                onClick = { onSelect(preset.key) },
            )
        }
        if (presets.isNotEmpty()) {
            Text("我的预设", style = AppThemeTypography.labelMedium)
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
            style = AppThemeTypography.labelSmall,
            color = AppThemeColors.onSurfaceVariant,
        )
    }
}

/**
 * 单个商品编辑卡片：名称独立一行（序号 + 删除），数量带 ± 步进，
 * 其余字段按权重网格排布，小屏自动收缩而不是横向滚动。
 */
@Composable
private fun ItemCardEditor(
    row: ItemRow,
    index: Int,
    onUpdate: (ItemRow) -> Unit,
    onRemove: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val qtyValue = row.qtyText.toIntOrNull() ?: 0
    Surface(
        shape = RoundedCornerShape(Ds.md + 4.dp),
        color = AppThemeColors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().testTag("editor_item_card"),
    ) {
        Column(
            modifier = Modifier.padding(Ds.md),
            verticalArrangement = Arrangement.spacedBy(Ds.sm),
        ) {
            // 第一行：序号 + 名称 + 删除
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Ds.sm),
            ) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = AppThemeColors.primaryContainer,
                    modifier = Modifier.size(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            style = AppThemeTypography.labelSmall,
                            color = AppThemeColors.onPrimaryContainer,
                        )
                    }
                }
                TextField(
                    value = row.name,
                    onValueChange = { if (it.length <= InputLimits.NAME) onUpdate(row.copy(name = it)) },
                    label = "名称",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(MiuixIcons.Delete, contentDescription = "删除行")
                }
            }
            // 第二行：数量（± 步进）× 单价 = 金额
            Row(
                horizontalArrangement = Arrangement.spacedBy(Ds.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepperButton(
                    symbol = "−",
                    contentDescription = "数量减一",
                    enabled = qtyValue > 1,
                    onClick = {
                        haptics.tick()
                        onUpdate(row.copy(qtyText = ((qtyValue - 1).coerceAtLeast(1)).toString()))
                    },
                )
                TextField(
                    value = row.qtyText,
                    onValueChange = { value -> onUpdate(row.copy(qtyText = value.filter(Char::isDigit))) },
                    label = "数量",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                StepperButton(
                    symbol = null,
                    icon = MiuixIcons.Add,
                    contentDescription = "数量加一",
                    enabled = true,
                    onClick = {
                        haptics.tick()
                        onUpdate(row.copy(qtyText = (qtyValue + 1).toString()))
                    },
                )
                Spacer(Modifier.width(Ds.xs))
                TextField(
                    value = row.priceText,
                    onValueChange = { onUpdate(row.copy(priceText = Money.sanitizeAmountInput(it))) },
                    label = "单价",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1.3f),
                )
                Column(modifier = Modifier.width(84.dp)) {
                    Text(
                        text = "金额",
                        style = AppThemeTypography.labelSmall,
                        color = AppThemeColors.onSurfaceVariant,
                    )
                    // 数量/单价变化时金额上下滑动过渡
                    AnimatedContent(
                        targetState = Money.format(row.amount()),
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInVertically { it } + fadeIn()) togetherWith
                                    (slideOutVertically { -it } + fadeOut())
                            } else {
                                (slideInVertically { -it } + fadeIn()) togetherWith
                                    (slideOutVertically { it } + fadeOut())
                            }
                        },
                        label = "amount",
                    ) { amount ->
                        Text(
                            text = amount,
                            style = AppThemeTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppThemeColors.primary,
                        )
                    }
                }
            }
            // 第三行：规格 / 单位（包装不再在单据上编辑，商品库字段保留）
            Row(
                horizontalArrangement = Arrangement.spacedBy(Ds.sm),
            ) {
                TextField(
                    value = row.spec,
                    onValueChange = { if (it.length <= InputLimits.SPEC) onUpdate(row.copy(spec = it)) },
                    label = "规格",
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                )
                TextField(
                    value = row.unit,
                    onValueChange = { if (it.length <= InputLimits.UNIT) onUpdate(row.copy(unit = it)) },
                    label = "单位",
                    singleLine = true,
                    modifier = Modifier.weight(0.8f),
                )
            }
            // 第四行：备注
            TextField(
                value = row.note,
                onValueChange = { if (it.length <= InputLimits.REMARK) onUpdate(row.copy(note = it)) },
                label = "备注",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 数量步进圆形按钮：surface 底 + primary 前景，与卡片背景形成相对层次；symbol 非空时显示文字符号。 */
@Composable
private fun StepperButton(
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    symbol: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val tint = if (enabled) AppThemeColors.primary else AppThemeColors.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = AppThemeColors.surface,
        modifier = Modifier.size(32.dp),
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (symbol != null) {
                Text(
                    text = symbol,
                    style = AppThemeTypography.titleMedium,
                    color = tint,
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                )
            }
        }
    }
}

@Composable
private fun ProductPickerSheet(
    products: List<Product>,
    onPick: (Product) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHaptics.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(products, query) {
        if (query.isBlank()) products else products.filter {
            it.name.contains(query.trim()) || it.spec.contains(query.trim())
        }
    }
    OverlayBottomSheet(
        show = true,
        title = "从商品库添加",
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Ds.lg)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = "搜索商品",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Ds.sm))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 440.dp),
            ) {
                items(filtered.size, key = { filtered[it].id }) { index ->
                    val p = filtered[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.tick()
                                onPick(p)
                            }
                            .padding(vertical = Ds.rowVertical),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, style = AppThemeTypography.bodyLarge)
                            Text(
                                listOf(p.spec, p.unit).filter { it.isNotBlank() }.joinToString(" · "),
                                style = AppThemeTypography.bodySmall,
                                color = AppThemeColors.onSurfaceVariant,
                            )
                        }
                        Text(
                            "¥${Money.format(p.price)}",
                            style = AppThemeTypography.bodyMedium,
                            color = AppThemeColors.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Ds.md))
        }
    }
}
