package com.example.quickbillmate.render

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

data class InvoiceRenderRequest(
    val id: Long,
    val invoice: RenderInvoice,
    val params: StyleParams,
)

/**
 * 单据渲染请求总线：各 ViewModel 提交请求，根组合中的 [InvoiceRenderEngine]
 * 消费请求并离屏捕获位图，调用方通过 [await] 取回结果。
 */
object InvoiceRenderBus {
    private var nextId = 1L
    private val _pending = MutableStateFlow<List<InvoiceRenderRequest>>(emptyList())
    private val _results = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())

    val pending: StateFlow<List<InvoiceRenderRequest>> = _pending.asStateFlow()
    val results: StateFlow<Map<Long, Bitmap>> = _results.asStateFlow()

    /** 提交渲染请求，返回可用于取回结果的请求 ID。 */
    fun enqueue(invoice: RenderInvoice, params: StyleParams): Long {
        val id = nextId++
        _pending.update { it + InvoiceRenderRequest(id, invoice, params) }
        return id
    }

    /** 引擎回调：写入结果并从待处理队列移除。 */
    fun deliver(id: Long, bitmap: Bitmap) {
        _results.update { it + (id to bitmap) }
        _pending.update { list -> list.filterNot { it.id == id } }
    }

    /** 等待请求渲染完成并取回位图；超时或失败返回 null。捕获器含最多 3 秒的排版稳定等待，超时上限需大于其与重试之和。 */
    suspend fun await(id: Long): Bitmap? {
        val result = withTimeoutOrNull(8000) {
            results.first { it.containsKey(id) }[id]
        }
        _results.update { it - id }
        return result
    }
}

/** 全局渲染引擎：常驻 App 根组合，消费 [InvoiceRenderBus] 中的请求。 */
@Composable
fun InvoiceRenderEngine() {
    val pending by InvoiceRenderBus.pending.collectAsState()
    pending.forEach { request ->
        key(request.id) {
            InvoiceBitmapCapture(
                invoice = request.invoice,
                params = request.params,
                onBitmap = { InvoiceRenderBus.deliver(request.id, it) },
            )
        }
    }
}
