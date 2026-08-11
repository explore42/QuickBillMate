package com.example.quickbillmate.ui.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.render.InvoiceBitmapCapture
import com.example.quickbillmate.render.RenderInvoice
import com.example.quickbillmate.render.RenderItem
import com.example.quickbillmate.render.StylePresets
import com.example.quickbillmate.ui.home.HomeBill
import com.example.quickbillmate.ui.home.HomeContent
import com.example.quickbillmate.ui.settings.SettingsContent
import com.example.quickbillmate.ui.theme.QuickBillMateTheme

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ThemePreview() {
    QuickBillMateTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("快贝智单 QuickBillMate", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun InvoicePreview() {
    QuickBillMateTheme {
        Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "单据预览",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            InvoiceBitmapCapture(
                invoice = sampleInvoice(),
                params = StylePresets.classic.params,
                onBitmap = { bitmap = it },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun HomeContentPreview() {
    QuickBillMateTheme {
        HomeContent(
            bills = sampleBills(),
            selectionMode = false,
            onSelectGroup = {},
            selectedIds = emptySet(),
            searchQuery = "",
            onSearchQueryChange = {},
            onNewBill = {},
            onOpenBill = {},
            onEnterSelection = {},
            onToggleSelection = {},
            onExitSelection = {},
            onToggleSelectAll = {},
            onCopy = {},
            onEdit = {},
            onExport = {},
            onDeleteRequest = {},
            onConfirmDelete = {},
            onCancelDelete = {},
            showDeleteConfirm = false,
            onDismissDeleteConfirm = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun HomeSelectionPreview() {
    QuickBillMateTheme {
        HomeContent(
            bills = sampleBills(),
            selectionMode = true,
            onSelectGroup = {},
            selectedIds = setOf(1L),
            searchQuery = "",
            onSearchQueryChange = {},
            onNewBill = {},
            onOpenBill = {},
            onEnterSelection = {},
            onToggleSelection = {},
            onExitSelection = {},
            onToggleSelectAll = {},
            onCopy = {},
            onEdit = {},
            onExport = {},
            onDeleteRequest = {},
            onConfirmDelete = {},
            onCancelDelete = {},
            showDeleteConfirm = false,
            onDismissDeleteConfirm = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun SettingsContentPreview() {
    QuickBillMateTheme {
        SettingsContent(
            themeMode = "system",
            defaultCompany = "示例建材有限公司",
            defaultPhone = "13800138000",
            defaultManager = "李经理",
            defaultPresetKey = "classic",
            defaultShowManager = true,
            defaultShowRemark = true,
            defaultShowWatermark = false,
            defaultShowMultiPhones = false,
            defaultShowAd = false,
            defaultRemark = "",
            defaultWatermarkText = "",
            defaultShowContactPhone = false,
            defaultDocCode = "PH",
            defaultTitleSuffix = "单据",
            defaultAdText = "",
            versionName = "1.0",
            presets = emptyList(),
            onThemeModeChange = {},
            onPresetChange = {},
            onDefaultsSave = {},
            onManagePresets = {},
            onOpenUrl = {},
        )
    }
}

private fun sampleInvoice(): RenderInvoice = RenderInvoice(
    customerName = "示例客户",
    customerPhone = "13800000000",
    companyName = "示例建材有限公司",
    contactPhone = "13800138000",
    salesManager = "李经理",
    docCode = "XS",
    docSerial = "482",
    docDate = "2025-11-21",
    discount = 12.0,
    remark = "客户自提",
    items = listOf(
        RenderItem("腻子粉", "YGP800 20kg", "袋", 10.0, 35.0, "20袋/托", ""),
        RenderItem("墙衬", "YGP400 20kg", "袋", 5.0, 28.0, "20袋/托", ""),
        RenderItem("蓝和纸墙面保护膜", "3m*18m", "卷", 2.0, 350.0, "50卷/件", "现货"),
    ),
)

private fun sampleBills(): List<HomeBill> = listOf(
    HomeBill(
        bill = Bill(
            id = 1,
            customerName = "示例客户",
            docCode = "XS",
            docSerial = "482",
            docDate = "2025-11-21",
            companyName = "示例建材有限公司",
        ),
        itemCount = 3,
        receivable = 888.0,
        itemNames = listOf("腻子粉", "墙衬", "蓝和纸墙面保护膜"),
    ),
    HomeBill(
        bill = Bill(
            id = 2,
            customerName = "",
            docCode = "XS",
            docSerial = "037",
            docDate = "2025-11-22",
        ),
        itemCount = 0,
        receivable = 0.0,
    ),
)
