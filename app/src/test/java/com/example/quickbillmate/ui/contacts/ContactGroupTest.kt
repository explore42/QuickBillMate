package com.example.quickbillmate.ui.contacts

import com.example.quickbillmate.importexport.ContactsImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactGroupTest {

    @Test
    fun `imported group goes last with check key`() {
        val candidates = listOf(
            ContactsImporter.Candidate("陈静", "13800000000"),
            ContactsImporter.Candidate("李娜", "13900000000"),
            ContactsImporter.Candidate("王强", "13700000000"),
            ContactsImporter.Candidate("李娜", "13800000000"),
            ContactsImporter.Candidate("陈静", "13800000005"),
        )
        val imported = setOf("李娜" to "13800000000", "陈静" to "13800000005")
        val letters = mapOf("陈静" to "C", "李娜" to "L", "王强" to "W")

        val grouped = groupContacts(
            candidates,
            { it.name to it.phone in imported },
            { letters[it.name] ?: "#" },
        )

        assertEquals(listOf("C", "L", "W", "✓"), grouped.map { it.key })
        // 已导入组内按拼音首字母排序：陈(C) 在 李(L) 前
        assertEquals(listOf("陈静", "李娜"), grouped.last().items.map { it.name })
        assertTrue(grouped.last().imported)
    }
}
