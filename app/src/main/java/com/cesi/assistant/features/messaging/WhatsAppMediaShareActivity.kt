package com.cesi.assistant.features.messaging

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/**
 * Lets Android's system picker grant temporary access to one user-selected file,
 * then opens WhatsApp with that file and a caption. CESI never scans the gallery
 * or requests broad photo/video permissions for this flow.
 *
 * WhatsApp owns the final destination and Send/Post action. CESI does not claim
 * the media was delivered or that a status was published.
 */
class WhatsAppMediaShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) launchPicker()
    }

    private fun launchPicker() {
        val kind = intent.getStringExtra(EXTRA_MEDIA_KIND).orEmpty()
        val mimeType = when (kind) {
            "image" -> "image/*"
            "video" -> "video/*"
            else -> "*/*"
        }
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            if (kind == "any") {
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            }
        }

        try {
            startActivityForResult(picker, REQUEST_PICK_MEDIA)
        } catch (_: Exception) {
            Toast.makeText(this, "Ba a iya buɗe media picker ba.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @Deprecated("Deprecated by Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK_MEDIA) return

        val uri: Uri? = if (resultCode == RESULT_OK) data?.data else null
        if (uri == null) {
            Toast.makeText(this, "Ba a zaɓi media ba; babu abin da aka tura.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val actualMime = contentResolver.getType(uri)
            ?: when (intent.getStringExtra(EXTRA_MEDIA_KIND)) {
                "image" -> "image/*"
                "video" -> "video/*"
                else -> "application/octet-stream"
            }
        val caption = intent.getStringExtra(EXTRA_CAPTION).orEmpty().trim()
        val share = Intent(Intent.ACTION_SEND).apply {
            type = actualMime
            putExtra(Intent.EXTRA_STREAM, uri)
            if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newUri(contentResolver, "CESI selected WhatsApp media", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage(WHATSAPP_PACKAGE)
        }

        if (share.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "Ban sami WhatsApp da zai karɓi wannan media ba.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            startActivity(share)
            val asStatus = intent.getBooleanExtra(EXTRA_AS_STATUS, false)
            val message = if (asStatus) {
                "WhatsApp ya buɗe da media da caption. Idan My Status ya bayyana, zaɓe shi; ka duba caption sannan ka danna Post da kanka."
            } else {
                "WhatsApp ya buɗe da media da caption. Zaɓi chat ɗin da ya dace, ka duba caption sannan ka danna Send da kanka."
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Ban iya miƙa media zuwa WhatsApp ba. Babu abin da CESI ya aika.", Toast.LENGTH_LONG).show()
        } finally {
            finish()
        }
    }

    companion object {
        const val EXTRA_CAPTION = "cesi.whatsapp.media.CAPTION"
        const val EXTRA_MEDIA_KIND = "cesi.whatsapp.media.KIND"
        const val EXTRA_AS_STATUS = "cesi.whatsapp.media.AS_STATUS"
        private const val REQUEST_PICK_MEDIA = 7301
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
    }
}
