package com.cesi.assistant

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionRequestActivity : Activity() {
    companion object {
        const val EXTRA_KIND = "permission_kind"
        const val KIND_CONTACTS = "contacts"
        const val KIND_CALL = "call"
        const val KIND_LOCATION = "location"
        const val KIND_CALLER_ID = "caller_id"
        private const val REQUEST_CODE = 9101
    }

    private lateinit var kind: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kind = intent.getStringExtra(EXTRA_KIND).orEmpty()

        val title = when (kind) {
            KIND_CONTACTS -> "CESI needs Contacts access"
            KIND_CALL -> "CESI needs Calling access"
            KIND_LOCATION -> "CESI needs Location access"
            KIND_CALLER_ID -> "CESI needs Caller ID access"
            else -> "CESI needs permission"
        }
        val message = when (kind) {
            KIND_CONTACTS -> "Allow Contacts so CESI can find people by name and read saved phone numbers."
            KIND_CALL -> "Allow Phone/Calling so CESI can place calls when you ask it to."
            KIND_LOCATION -> "Allow Location so CESI can tell you where you are."
            KIND_CALLER_ID -> "Allow Phone state, Call log and Contacts access so CESI can identify incoming callers when Android provides the number."
            else -> "Allow the required permission to continue."
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 40)
        }
        root.addView(TextView(this).apply {
            text = title
            textSize = 22f
            setPadding(0, 0, 0, 18)
        })
        root.addView(TextView(this).apply {
            text = message
            textSize = 16f
            setPadding(0, 0, 0, 24)
        })
        root.addView(Button(this).apply {
            text = "Allow permission"
            setOnClickListener { requestForKind() }
        })
        root.addView(Button(this).apply {
            text = "Cancel"
            setOnClickListener { finish() }
        })
        setContentView(root)

        // Start with the Android permission dialog when this activity was opened by CESI.
        window.decorView.post { requestForKind() }
    }

    private fun requestForKind() {
        val permissions = when (kind) {
            KIND_CONTACTS -> arrayOf(Manifest.permission.READ_CONTACTS)
            KIND_CALL -> arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS)
            KIND_LOCATION -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            KIND_CALLER_ID -> arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS
            )
            else -> emptyArray()
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            finish()
            return
        }
        ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) finish()
    }
}
