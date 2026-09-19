package com.cesi.assistant.features.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

class CallController(private val context: Context) {
    fun call(target: String): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) return "Ina bukatar permission na kira."

        var number = target
        if (!target.matches(Regex("[+0-9][0-9 ()-]{5,}"))) {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$target%"),
                null
            )
            cursor?.use { if (it.moveToFirst()) number = it.getString(0) }
        }
        if (!number.matches(Regex("[+0-9][0-9 ()-]{5,}"))) return "Ban sami lambar $target ba."

        context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${number.replace(" ", "")}")))
        return "Ina kira $target."
    }
}
