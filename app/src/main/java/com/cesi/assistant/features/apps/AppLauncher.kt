package com.cesi.assistant.features.apps

import android.content.Context
import android.content.Intent

class AppLauncher(private val context: Context) {
    fun launch(query: String): String {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
        val match = apps.firstOrNull {
            it.loadLabel(pm).toString().lowercase().contains(query.lowercase()) ||
            it.activityInfo.packageName.lowercase().contains(query.lowercase())
        } ?: return "Ban sami app ɗin $query ba."
        val launch = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
            ?: return "Ba zan iya buɗe $query ba."
        context.startActivity(launch)
        return "Na buɗe ${match.loadLabel(pm)}."
    }
}
