package com.cesi.assistant.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android 14+ blocks microphone foreground services from BOOT_COMPLETED.
 * CESI therefore does not attempt to start its microphone services here.
 * The user must open CESI once after boot to enable the background assistant.
 */
class CesiBootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        // Intentionally no microphone foreground-service start here.
    }
}
