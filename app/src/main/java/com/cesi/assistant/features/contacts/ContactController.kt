package com.cesi.assistant.features.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

class ContactController(private val context: Context) {
    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun search(query: String): String {
        if (!hasContactsPermission()) return "Ina bukatar permission na Contacts."
        return try {
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
                if (it.moveToFirst()) return it.getString(0) + ": " + it.getString(1)
            }
            "Ban sami contact $query ba."
        } catch (_: SecurityException) {
            "Ina bukatar permission na Contacts."
        }
    }

    fun findContactName(number: String): String? {
        if (number.isBlank() || !hasContactsPermission()) return null
        val incomingDigits = digits(number)
        if (incomingDigits.isBlank()) return null

        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val savedName = it.getString(0).orEmpty()
                    val savedNumber = digits(it.getString(1).orEmpty())
                    if (savedNumber.isNotBlank() && numbersMatch(incomingDigits, savedNumber)) {
                        return savedName
                    }
                }
                null
            }
        } catch (_: SecurityException) {
            null
        } ?: null
    }

    private fun digits(value: String): String = value.filter(Char::isDigit)

    private fun numbersMatch(a: String, b: String): Boolean {
        if (a == b) return true
        val aa = a.takeLast(10)
        val bb = b.takeLast(10)
        return aa.length >= 7 && bb.length >= 7 && aa == bb
    }
}
