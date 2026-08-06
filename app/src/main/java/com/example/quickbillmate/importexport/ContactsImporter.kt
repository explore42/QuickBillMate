package com.example.quickbillmate.importexport

import android.content.Context
import android.provider.ContactsContract

object ContactsImporter {
    data class Candidate(val name: String, val phone: String)

    /** 读取通讯录中有电话号码的联系人；同一联系人多个号码拆分为多条候选。 */
    fun query(context: Context): List<Candidate> {
        val result = mutableListOf<Candidate>()
        val resolver = context.contentResolver
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx)?.trim().orEmpty()
                val number = cursor.getString(numIdx)?.replace(" ", "")?.trim().orEmpty()
                if (name.isNotBlank() && number.isNotBlank()) {
                    result.add(Candidate(name, number))
                }
            }
        }
        return result.distinctBy { it.name to it.phone }
    }
}
