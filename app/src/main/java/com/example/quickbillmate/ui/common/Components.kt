package com.example.quickbillmate.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.quickbillmate.R
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.Ds
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup

/** 应用小 Logo：使用 APP 图标同款 PNG。 */
@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_app_logo),
        contentDescription = null,
        modifier = modifier.clearAndSetSemantics { },
    )
}

/** 可折叠式分区卡片（Miuix 平滑圆角卡片）。 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = AppThemeColors.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(Ds.lg)) {
            Text(
                text = title,
                style = AppThemeTypography.titleSmall,
                color = AppThemeColors.primary,
            )
            Spacer(Modifier.height(Ds.md))
            content()
        }
    }
}

/** 带标签的单行文本输入框（Miuix TextField，无 M3 错误态时手动渲染提示文本）。 */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    val field = @Composable {
        TextField(
            value = value,
            onValueChange = onChange,
            modifier = modifier,
            label = label.ifEmpty { placeholder },
            useLabelAsPlaceholder = label.isEmpty() && placeholder.isNotEmpty(),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
    }
    if (supportingText != null) {
        Column(modifier = modifier) {
            field()
            if (isError || supportingText.isNotBlank()) {
                Text(
                    text = supportingText,
                    style = AppThemeTypography.bodySmall,
                    color = if (isError) AppThemeColors.error else AppThemeColors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                )
            }
        }
    } else {
        field()
    }
}

/** 整行可点击的开关。 */
@Composable
fun LabeledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = AppThemeTypography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 详情对话框里的只读字段行：标签 + 值。 */
@Composable
fun DetailLine(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            style = AppThemeTypography.bodyMedium,
            color = AppThemeColors.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            value,
            style = AppThemeTypography.bodyMedium,
        )
    }
}

/**
 * 统一顶部标题栏（Miuix 风格自绘）：独立底色、标准窗口边距适配全面屏，
 * 支持 Logo 标题、返回图标与右侧操作区。
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    showLogo: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppThemeColors.surfaceContainer)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    navigationIcon()
                }
            }
            Row(
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showLogo) {
                    AppLogo(Modifier.size(26.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(title, fontWeight = FontWeight.Bold, style = AppThemeTypography.titleMedium)
            }
            actions()
            Spacer(Modifier.width(8.dp))
        }
    }
}

/**
 * 带搜索的标题栏：默认显示标题 + 右侧搜索图标；
 * 点击图标后标题变为紧凑搜索框（返回图标收起，输入内容后出现清除叉号）。
 */
@Composable
fun SearchableTopBar(
    title: String,
    searchPlaceholder: String,
    query: String,
    onQueryChange: (String) -> Unit,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    var searching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(searching) {
        if (searching) {
            focusRequester.requestFocus()
        }
    }
    BackHandler(enabled = searching) {
        searching = false
        onQueryChange("")
    }

    fun closeSearch() {
        searching = false
        onQueryChange("")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppThemeColors.surfaceContainer)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                searching -> IconButton(onClick = { closeSearch() }) {
                    Icon(MiuixIcons.Back, contentDescription = "返回")
                }
                navigationIcon != null -> navigationIcon()
            }
            if (searching) {
                CompactSearchField(
                    query = query,
                    placeholder = searchPlaceholder,
                    onQueryChange = onQueryChange,
                    focusRequester = focusRequester,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Row(
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppLogo(Modifier.size(26.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.Bold, style = AppThemeTypography.titleMedium)
                }
                actions()
                IconButton(onClick = { searching = true }) {
                    Icon(MiuixIcons.Basic.Search, contentDescription = "搜索")
                }
            }
        }
    }
}

/** 紧凑搜索框：40dp 高、14sp 文字，文字完整显示不被裁剪。 */
@Composable
fun CompactSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppThemeColors.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = AppThemeTypography.bodyMedium.copy(
                color = AppThemeColors.onSurface,
            ),
            cursorBrush = SolidColor(AppThemeColors.primary),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        MiuixIcons.Basic.Search,
                        contentDescription = null,
                        tint = AppThemeColors.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                placeholder,
                                style = AppThemeTypography.bodyMedium,
                                color = AppThemeColors.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(MiuixIcons.Basic.Close, contentDescription = "清除搜索")
                        }
                    }
                }
            },
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppThemeColors.outline,
            modifier = Modifier.height(48.dp).width(48.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = text,
            style = AppThemeTypography.bodyMedium,
            color = AppThemeColors.outline,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String = "删除",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    OverlayDialog(
        title = title,
        summary = text,
        show = true,
        onDismissRequest = onDismiss,
    ) {
        DialogButtons(
            confirmText = confirmText,
            onCancel = onDismiss,
            onConfirm = onConfirm,
            destructive = destructive,
        )
    }
}

@Composable
fun InfoDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    confirmText: String = "知道了",
) {
    OverlayDialog(
        title = title,
        summary = text,
        show = true,
        onDismissRequest = onDismiss,
    ) {
        DialogButtons(
            confirmText = confirmText,
            cancelText = null,
            onConfirm = onDismiss,
        )
    }
}

/**
 * 对话框标准按钮区：次级操作（取消）为文本按钮，主操作为实心主色按钮且更宽，
 * 通过视觉权重体现操作优先级；破坏性操作（删除/放弃）确认按钮使用错误色警示。
 */
@Composable
fun DialogButtons(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "确定",
    cancelText: String? = "取消",
    onCancel: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Ds.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cancelText != null && onCancel != null) {
            TextButton(
                text = cancelText,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                minHeight = Ds.buttonHeight,
            )
        }
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(if (cancelText != null && onCancel != null) 1.5f else 1f),
            minHeight = Ds.buttonHeight,
            colors = if (destructive) {
                ButtonDefaults.buttonColors(
                    color = AppThemeColors.error,
                    contentColor = Color.White,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Text(confirmText)
        }
    }
}

/** 可选标签 Chip：选中态主色容器，未选中浅色容器，用于类型/字体等快速选择。 */
@Composable
fun SelectionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        color = if (selected) {
            AppThemeColors.primaryContainer
        } else {
            AppThemeColors.surfaceContainerHigh
        },
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = AppThemeTypography.labelMedium,
            color = if (selected) {
                AppThemeColors.onPrimaryContainer
            } else {
                AppThemeColors.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = Ds.md, vertical = 6.dp),
        )
    }
}

/** 紧凑文本按钮（32dp 高、更小内边距），用于对话框底部与顶栏次要操作。 */
@Composable
fun SmallTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minHeight = 32.dp,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = if (primary) {
            ButtonDefaults.textButtonColorsPrimary()
        } else {
            ButtonDefaults.textButtonColors()
        },
    )
}

/** 电话标签：浅色圆角胶囊 + 电话图标，点击拨号（多电话场景逐个拨打）。 */
@Composable
fun PhoneTag(
    phone: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(AppThemeColors.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            MiuixIcons.Phone,
            contentDescription = null,
            tint = AppThemeColors.onPrimaryContainer,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            phone,
            style = AppThemeTypography.bodySmall,
            color = AppThemeColors.onPrimaryContainer,
        )
    }
}

/** 只读小标签（浅色小圆角容器），用于列表行/详情里的类型、来源等标记。 */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = AppThemeColors.secondaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = AppThemeTypography.labelSmall,
            color = AppThemeColors.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Miuix 风格动作菜单：基于 Scaffold 内置的 MiuixPopupUtils（OverlayListPopup）。 */
@Composable
fun MiuixMenuPopup(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    if (!expanded) return
    OverlayListPopup(
        show = true,
        onDismissRequest = onDismiss,
    ) {
        ListPopupColumn {
            items.forEachIndexed { index, (label, action) ->
                DropdownImpl(
                    text = label,
                    optionSize = items.size,
                    isSelected = false,
                    index = index,
                    onSelectedIndexChange = { action() },
                )
            }
        }
    }
}
