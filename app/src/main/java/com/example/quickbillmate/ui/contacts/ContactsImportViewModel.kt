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
import com.example.quickbillmate.util.PhoneUtil
import com.example.quickbillmate.util.Pinyin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 通讯录导入分组：title 为分组标题，key 为索引标识；imported 组置于末尾并灰化。 */
data class ContactSection(
    val title: String,
    val key: String,
    val items: List<ContactsImporter.Candidate>,
    val imported: Boolean = false,
)

/** 把联系人按「字母组 → # → 已导入组」分组（已导入组始终在最后）。 */
internal fun groupContacts(
    candidates: List<ContactsImporter.Candidate>,
    isImported: (ContactsImporter.Candidate) -> Boolean,
    letterOf: (ContactsImporter.Candidate) -> String,
): List<ContactSection> {
    val importable = candidates.filterNot { isImported(it) }
    val imported = candidates.filter { isImported(it) }
        .sortedWith(compareBy { Pinyin.letterSortKey(letterOf(it)) })
    val byLetter = importable.groupBy { letterOf(it) }
    return buildList {
        byLetter.keys.filter { it != "#" }.sorted().forEach { letter ->
            byLetter[letter]?.let { add(ContactSection(letter, letter, it)) }
        }
        byLetter["#"]?.let { add(ContactSection("#", "#", it)) }
        if (imported.isNotEmpty()) {
            add(ContactSection("已导入", "✓", imported, imported = true))
        }
    }
}

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

    /** 客户库中 (名称, 规范化电话) 集合，用于判定已导入。 */
    private var existingPhones: Set<Pair<String, String>> = emptySet()
    private var letters: Map<String, String> = emptyMap()

    val filtered: List<ContactsImporter.Candidate>
        get() {
            val q = query.trim()
            return if (q.isBlank()) candidates else candidates.filter {
                it.name.contains(q) || it.phone.contains(q)
            }
        }

    fun isImported(candidate: ContactsImporter.Candidate): Boolean =
        (candidate.name to candidate.phone) in existingPhones

    /** 分组后的展示数据（含索引键）。 */
    fun sections(): List<ContactSection> = groupContacts(
        filtered,
        ::isImported,
        { letters[keyOf(it.name, it.phone)] ?: "#" },
    )

    fun onQueryChange(value: String) {
        query = value
    }

    fun load() {
        viewModelScope.launch {
            loading = true
            val contacts = withContext(Dispatchers.Default) {
                ContactsImporter.query(app)
            }
            val customers = repo.getCustomers()
            existingPhones = customers
                .flatMap { c -> PhoneUtil.splitPhones(c.phone).map { c.name to it } }
                .toSet()
            letters = withContext(Dispatchers.Default) {
                contacts.associate { keyOf(it.name, it.phone) to Pinyin.firstLetter(it.name) }
            }
            candidates = contacts
            loading = false
        }
    }

    fun toggle(key: String, selectedNow: Boolean) {
        val candidate = candidates.firstOrNull { keyOf(it.name, it.phone) == key } ?: return
        if (isImported(candidate)) return
        selected = if (selectedNow) selected + key else selected - key
    }

    fun toggleAll(list: List<ContactsImporter.Candidate>, checkAll: Boolean) {
        val keys = list.filterNot { isImported(it) }.map { keyOf(it.name, it.phone) }
        selected = if (checkAll) selected + keys else selected - keys.toSet()
    }

    fun selectedList(): List<ContactsImporter.Candidate> =
        candidates.filter { keyOf(it.name, it.phone) in selected }

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
        load()
    }

    private fun keyOf(name: String, phone: String) = "$name\u0000$phone"
}