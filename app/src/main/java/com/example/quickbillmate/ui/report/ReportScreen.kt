package com.example.quickbillmate.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.ui.AppViewModelProvider
import com.example.quickbillmate.ui.common.AppTopBar
import com.example.quickbillmate.ui.common.DatePickerDialog
import com.example.quickbillmate.ui.common.EmptyState
import com.example.quickbillmate.ui.common.SelectionChip
import com.example.quickbillmate.ui.theme.AppThemeColors
import com.example.quickbillmate.ui.theme.AppThemeTypography
import com.example.quickbillmate.ui.theme.Ds
import com.example.quickbillmate.util.BillNumber
import com.example.quickbillmate.util.Money
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Report
import java.time.LocalDate

/** 报表明细：某维度（月份/客户/商品）下的单据列表。 */
data class ReportDetail(
    val title: String,
    val bills: List<Bill>,
    val amounts: Map<Long, Double>,
)

@Composable
fun ReportScreen(
    onBack: () -> Unit,
    onOpenBill: (Long) -> Unit,
    viewModel: ReportViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var detail by remember { mutableStateOf<ReportDetail?>(null) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var draftStart by remember { mutableStateOf(viewModel.customStart) }
    var draftEnd by remember { mutableStateOf(viewModel.customEnd) }

    val currentDetail = detail
    if (currentDetail != null) {
        BillDetailView(
            detail = currentDetail,
            onBack = { detail = null },
            onOpenBill = onOpenBill,
        )
        return
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "数据报表",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (viewModel.loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (viewModel.filteredBills.isEmpty()) {
            EmptyState(
                icon = MiuixIcons.Report,
                text = "当前范围暂无单据，去新建一张吧",
                modifier = Modifier.padding(padding),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Ds.screen),
                verticalArrangement = Arrangement.spacedBy(Ds.md),
            ) {
                Spacer(Modifier.height(Ds.xs))
                RangeSelector(
                    preset = viewModel.preset,
                    onSelect = viewModel::selectPreset,
                    customStart = draftStart,
                    customEnd = draftEnd,
                    onPickStart = { pickingStart = true },
                    onPickEnd = { pickingEnd = true },
                )
                viewModel.summary?.let { SummaryCards(it) }
                TabSelector(tab = viewModel.tab, onSelect = viewModel::selectTab)
                when (viewModel.tab) {
                    ReportTab.TIME -> TimeTab(
                        points = viewModel.timePoints,
                        onClick = { key ->
                            val bills = viewModel.filteredBills.filter {
                                (if (key.length == 7) it.docDate.take(7) else it.docDate) == key
                            }
                            detail = ReportDetail(
                                title = key,
                                bills = bills,
                                amounts = viewModel.billAmountMap(bills),
                            )
                        },
                    )
                    ReportTab.CUSTOMER -> RankTab(
                        entries = viewModel.customerRanks,
                        showQty = false,
                        onClick = { name ->
                            val bills = viewModel.filteredBills.filter {
                                it.customerName.trim().ifEmpty { "未填写" } == name
                            }
                            detail = ReportDetail(
                                title = name,
                                bills = bills,
                                amounts = viewModel.billAmountMap(bills),
                            )
                        },
                    )
                    ReportTab.PRODUCT -> RankTab(
                        entries = viewModel.productRanks,
                        showQty = true,
                        onClick = { name ->
                            val bills = viewModel.billsContainingProduct(name)
                            detail = ReportDetail(
                                title = name,
                                bills = bills,
                                amounts = viewModel.billAmountMap(bills),
                            )
                        },
                    )
                }
                Spacer(Modifier.height(Ds.lg))
            }
        }
    }

    if (pickingStart) {
        DatePickerDialog(
            initial = LocalDate.parse(draftStart ?: LocalDate.now().minusDays(30).toString()),
            onConfirm = { date ->
                draftStart = date.toString()
                pickingStart = false
                viewModel.selectCustom(draftStart!!, draftEnd ?: draftStart!!)
            },
            onDismiss = { pickingStart = false },
        )
    }
    if (pickingEnd) {
        DatePickerDialog(
            initial = LocalDate.parse(draftEnd ?: LocalDate.now().toString()),
            onConfirm = { date ->
                draftEnd = date.toString()
                pickingEnd = false
                viewModel.selectCustom(draftStart ?: draftEnd!!, draftEnd!!)
            },
            onDismiss = { pickingEnd = false },
        )
    }
}

@Composable
private fun RangeSelector(
    preset: RangePreset,
    onSelect: (RangePreset) -> Unit,
    customStart: String?,
    customEnd: String?,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Ds.sm)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Ds.sm),
        ) {
            RangePreset.entries.forEach { item ->
                SelectionChip(
                    text = item.label,
                    selected = preset == item,
                    onClick = { onSelect(item) },
                )
            }
        }
        if (preset == RangePreset.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(Ds.sm)) {
                SelectionChip(
                    text = "开始 ${customStart ?: "未选"}",
                    selected = false,
                    onClick = onPickStart,
                )
                SelectionChip(
                    text = "结束 ${customEnd ?: "未选"}",
                    selected = false,
                    onClick = onPickEnd,
                )
            }
        }
    }
}

@Composable
private fun SummaryCards(summary: ReportSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(Ds.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Ds.sm)) {
            SummaryCard("单据数", "${summary.billCount}", Modifier.weight(1f))
            SummaryCard("总金额", "¥${Money.format(summary.totalAmount)}", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Ds.sm)) {
            SummaryCard("客单价", "¥${Money.format(summary.avgPerBill)}", Modifier.weight(1f))
            SummaryCard("客户数", "${summary.customerCount}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.defaultColors(color = AppThemeColors.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(Ds.md)) {
            Text(
                label,
                style = AppThemeTypography.labelSmall,
                color = AppThemeColors.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = AppThemeTypography.titleMedium,
                color = AppThemeColors.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TabSelector(
    tab: ReportTab,
    onSelect: (ReportTab) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Ds.sm)) {
        ReportTab.entries.forEach { item ->
            SelectionChip(
                text = item.label,
                selected = tab == item,
                onClick = { onSelect(item) },
            )
        }
    }
}

@Composable
private fun TimeTab(
    points: List<TimePoint>,
    onClick: (String) -> Unit,
) {
    if (points.isEmpty()) {
        EmptyState(icon = MiuixIcons.Report, text = "暂无数据")
        return
    }
    AmountColumnChart(points)
    Spacer(Modifier.height(Ds.sm))
    Column(verticalArrangement = Arrangement.spacedBy(Ds.xs)) {
        points.forEach { point ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Ds.md))
                    .clickable { onClick(point.key) }
                    .padding(horizontal = Ds.md, vertical = Ds.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    point.label,
                    style = AppThemeTypography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${point.billCount} 单",
                    style = AppThemeTypography.bodySmall,
                    color = AppThemeColors.onSurfaceVariant,
                )
                Spacer(Modifier.width(Ds.md))
                Text(
                    "¥${Money.format(point.amount)}",
                    style = AppThemeTypography.bodyMedium,
                    color = AppThemeColors.primary,
                )
            }
        }
    }
}

@Composable
private fun AmountColumnChart(points: List<TimePoint>) {
    val primary = AppThemeColors.primary
    val chart = rememberCartesianChart(
        rememberColumnCartesianLayer(
            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                LineComponent(fill = fill(primary)),
            ),
        ),
    )
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        modelProducer.runTransaction {
            columnSeries { series(points.map { it.amount }) }
        }
    }
    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    )
}

@Composable
private fun RankTab(
    entries: List<RankEntry>,
    showQty: Boolean,
    onClick: (String) -> Unit,
) {
    if (entries.isEmpty()) {
        EmptyState(icon = MiuixIcons.Report, text = "暂无数据")
        return
    }
    val top = ReportAggregator.topWithOther(entries)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        DonutChart(top, Modifier.size(180.dp))
    }
    Spacer(Modifier.height(Ds.md))
    val maxAmount = entries.firstOrNull()?.amount?.takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(Ds.sm)) {
        entries.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Ds.md))
                    .clickable { onClick(entry.name) }
                    .padding(horizontal = Ds.md, vertical = Ds.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${index + 1}",
                    style = AppThemeTypography.labelMedium,
                    color = AppThemeColors.onSurfaceVariant,
                    modifier = Modifier.width(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.name, style = AppThemeTypography.bodyMedium)
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(AppThemeColors.surfaceContainerHighest),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction(entry.amount, maxAmount))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AppThemeColors.primary),
                        )
                    }
                }
                Spacer(Modifier.width(Ds.md))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "¥${Money.format(entry.amount)}",
                        style = AppThemeTypography.bodyMedium,
                        color = AppThemeColors.primary,
                    )
                    Text(
                        if (showQty) {
                            "${entry.billCount} 单 · ${Money.format(entry.qty)} 件"
                        } else {
                            "${entry.billCount} 单"
                        },
                        style = AppThemeTypography.labelSmall,
                        color = AppThemeColors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun progressFraction(value: Double, max: Double): Float =
    if (max <= 0) 0f else (value / max).coerceIn(0.02, 1.0).toFloat()

@Composable
private fun DonutChart(
    entries: List<RankEntry>,
    modifier: Modifier = Modifier,
) {
    val colors = listOf(
        AppThemeColors.primary,
        AppThemeColors.secondaryContainer,
        AppThemeColors.primaryContainer,
        AppThemeColors.surfaceContainerHighest,
        AppThemeColors.outline,
    )
    val total = entries.sumOf { it.amount }.takeIf { it > 0 } ?: 1.0
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val diameter = size.minDimension
            val stroke = 26.dp.toPx()
            val arcSize = diameter - stroke
            var startAngle = -90f
            entries.forEachIndexed { index, entry ->
                val sweep = (entry.amount / total * 360f).toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep.coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = Offset((diameter - arcSize) / 2, (diameter - arcSize) / 2),
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                startAngle += sweep
            }
        }
        Text(
            "¥${Money.format(total)}",
            style = AppThemeTypography.titleSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BillDetailView(
    detail: ReportDetail,
    onBack: () -> Unit,
    onOpenBill: (Long) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = detail.title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (detail.bills.isEmpty()) {
            EmptyState(icon = MiuixIcons.Report, text = "该维度暂无单据", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = Ds.screen, vertical = Ds.sm),
            ) {
                items(detail.bills, key = { it.id }) { bill ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Ds.md))
                            .clickable { onOpenBill(bill.id) }
                            .padding(vertical = Ds.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                BillNumber.build(bill.docCode, bill.docDate, bill.docSerial),
                                style = AppThemeTypography.bodyMedium,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                listOf(bill.docDate, bill.customerName.ifBlank { "未填写客户" })
                                    .joinToString(" · "),
                                style = AppThemeTypography.bodySmall,
                                color = AppThemeColors.onSurfaceVariant,
                            )
                        }
                        Text(
                            "¥${Money.format(detail.amounts[bill.id] ?: 0.0)}",
                            style = AppThemeTypography.bodyMedium,
                            color = AppThemeColors.primary,
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
