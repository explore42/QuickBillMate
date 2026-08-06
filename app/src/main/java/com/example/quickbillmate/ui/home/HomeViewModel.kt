package com.example.quickbillmate.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.Bill
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.util.BillNumber
import com.example.quickbillmate.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeBill(val bill: Bill, val itemCount: Int, val receivable: Double) {
    val docNumber: String
        get() = BillNumber.build(bill.docCode, bill.docDate, bill.docSerial)

    val receivableText: String
        get() = Money.format(receivable)
}

class HomeViewModel(private val repo: AppRepository) : ViewModel() {
    private val _bills = MutableStateFlow<List<HomeBill>>(emptyList())
    val bills: StateFlow<List<HomeBill>> = _bills

    init {
        viewModelScope.launch {
            repo.observeRecentBills().collect { list ->
                _bills.value = list.map { bill ->
                    val items = repo.getItems(bill.id)
                    val total = items.sumOf {
                        if (it.qty <= 0) 0.0 else Money.round2(it.qty * it.price)
                    }
                    HomeBill(
                        bill = bill,
                        itemCount = items.size,
                        receivable = Math.max(0.0, Money.round2(total - bill.discount)),
                    )
                }
            }
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch { repo.deleteBill(bill) }
    }
}
