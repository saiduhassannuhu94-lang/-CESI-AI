package com.cesi.assistant.features.contacts

import android.content.Context
import android.provider.ContactsContract

class ContactController(private val context: Context) {
    fun search(query: String): String {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$query%"),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val name = it.getString(0)
                val number = it.getString(1)
                return "$name: $number"
            }
        }
        return "Ban sami contact $query ba."
    }
}
