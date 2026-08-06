package com.example.quickbillmate.ui.contacts

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillmate.data.repository.AppRepository
import com.example.quickbillmate.data.repository.ContactImportOutcome
import com.example.quickbillmate.importexport.ContactsImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsImportViewModel(
    private val app: Application,
    private val repo: AppRepository,
) : ViewModel() {

    var loading by mutableStateOf(true)
        private set
    var query by mutableStateOf("")
        private set
    var candidates by mutableStateOf<List<ContactsImporter.Candidate>>(emptyList())
        private set
    var selected by mutableStateOf<Set<String>>(emptySet())
        private set
    var importing by mutableStateOf(false)
        private set
    var importResult by mutableStateOf<ContactImportOutcome?>(null)
        private set

    /** 客户库中已有的客户姓名集合，用于给同名联系人显示“合并”徽标。 */
    var existingCustomerNames by mutableStateOf<Set<String>>(emptySet())
        private set

    val filtered: List<ContactsImporter.Candidate>
        get() {
            val q = query.trim()
            return if (q.isBlank()) candidates else candidates.filter {
                it.name.contains(q) || it.phone.contains(q)
            }
        }

    fun onQueryChange(value: String) {
        query = value
    }

    fun isMergeCandidate(candidate: ContactsImporter.Candidate): Boolean =
        candidate.name in existingCustomerNames

    fun load() {
        viewModelScope.launch {
            loading = true
            val contacts = withContext(Dispatchers.IO) {
                ContactsImporter.query(app)
            }
            existingCustomerNames = repo.getCustomers().map { it.name }.toSet()
            candidates = contacts
            loading = false
        }
    }

    fun toggle(key: String, selectedNow: Boolean) {
        selected = if (selectedNow) selected + key else selected - key
    }

    fun toggleAll(list: List<ContactsImporter.Candidate>, checkAll: Boolean) {
        val keys = list.map { keyOf(it.name, it.phone) }
        selected = if (checkAll) selected + keys else selected - keys.toSet()
    }

    fun selectedList(): List<ContactsImporter.Candidate> =
        candidates.filter { keyOf(it.name, it.phone) in selected }

    /** 直接按合并语义导入选中的联系人，不再弹“跳过”确认。 */
    fun importSelected() {
        val list = selectedList()
        if (list.isEmpty()) return
        viewModelScope.launch {
            importing = true
            importResult = withContext(Dispatchers.IO) {
                repo.importContactCandidates(list)
            }
            importing = false
        }
    }

    fun consumeResult() {
        importResult = null
        selected = emptySet()
    }

    private fun keyOf(name: String, phone: String) = "$name\u0000$phone"
}
