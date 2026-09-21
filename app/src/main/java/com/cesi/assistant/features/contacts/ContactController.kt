package com.cesi.assistant.features.contacts

import android.content.Context
import android.provider.ContactsContract

class ContactController(private val context: Context) {
    fun search(query: String): String {
        val found=findContact(query)
        return if(found!=null) "${found.first}: ${found.second}" else "Ban sami contact $query ba."
    }
    fun findPhoneNumber(query:String):String?=findContact(query)?.second
    private fun findContact(query:String):Pair<String,String>?{
        val c=context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$query%"),null)
        c?.use{if(it.moveToFirst())return it.getString(0) to it.getString(1)}
        return null
    }
}
