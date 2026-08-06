package com.example.quickbillmate.ui.customers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.db.Customer
import com.example.quickbillmate.data.repository.AppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CustomersViewModel(private val repo: AppRepository) : ViewModel() {
    private val query = MutableStateFlow("")

    var queryText by mutableStateOf("")
        private set

    val customers: StateFlow<List<Customer>> = query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { repo.observeCustomers(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(value: String) {
        queryText = value
        query.value = value
    }

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch { repo.saveCustomer(customer) }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch { repo.deleteCustomer(customer) }
    }

    fun toggleFavorite(customer: Customer) {
        viewModelScope.launch {
            repo.saveCustomer(customer.copy(favorite = !customer.favorite))
        }
    }
}
