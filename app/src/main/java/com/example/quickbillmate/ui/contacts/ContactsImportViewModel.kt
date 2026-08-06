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

    /** 待确认的同名合并清单（发现同名客户时弹出确认）。 */
    var pendingMerge by mutableStateOf<List<ContactsImporter.Candidate>?>(null)
        private set
    var pendingMergeConflictCount by mutableStateOf(0)
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

    fun load() {
        viewModelScope.launch {
            loading = true
            candidates = withContext(Dispatchers.IO) {
                ContactsImporter.query(app)
            }
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

    fun importSelected() {
        val list = selectedList()
        if (list.isEmpty()) return
        viewModelScope.launch {
            val existing = repo.getCustomers()
            val conflicts = list.filter { candidate ->
                existing.any { it.name == candidate.name } &&
                    !existing.any { it.name == candidate.name && it.phone == candidate.phone }
            }
            if (conflicts.isNotEmpty()) {
                pendingMerge = list
                pendingMergeConflictCount = conflicts.size
            } else {
                doImport(list, mergeSameName = false)
            }
        }
    }

    fun importWithMerge(mergeSameName: Boolean) {
        val list = pendingMerge ?: return
        pendingMerge = null
        pendingMergeConflictCount = 0
        doImport(list, mergeSameName)
    }

    fun cancelMerge() {
        pendingMerge = null
        pendingMergeConflictCount = 0
    }

    private fun doImport(list: List<ContactsImporter.Candidate>, mergeSameName: Boolean) {
        viewModelScope.launch {
            importing = true
            importResult = withContext(Dispatchers.IO) {
                repo.importContactCandidates(list, mergeSameName)
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
