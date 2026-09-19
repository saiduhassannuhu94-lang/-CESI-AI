package com.cesi.assistant.features.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import java.util.Locale

class AppLauncher(private val context: Context) {

    fun launch(query: String): String {
        val requested = query.trim()

        if (requested.isBlank()) {
            return "Wane app kake son in bude?"
        }

        val pm = context.packageManager

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps: List<ResolveInfo> =
            pm.queryIntentActivities(launcherIntent, 0)

        val normalized = normalize(requested)

        val match = apps
            .map { info ->
                Triple(
                    info,
                    info.loadLabel(pm).toString(),
                    info.activityInfo.packageName
                )
            }
            .sortedBy { (_, label, packageName) ->
                when {
                    normalize(label) == normalized -> 0
                    normalize(label).startsWith(normalized) -> 1
                    normalize(label).contains(normalized) -> 2
                    normalize(packageName).contains(normalized) -> 3
                    else -> 4
                }
            }
            .firstOrNull { (_, label, packageName) ->
                normalize(label).contains(normalized) ||
                normalize(packageName).contains(normalized)
            }

        if (match == null) {
            return "Ban sami app ɗin $requested ba. Ka faɗi sunan app ɗin kamar WhatsApp, YouTube ko Chrome."
        }

        val info = match.first

        val launchIntent =
            pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                ?: return "Na sami ${match.second}, amma Android bai bani damar buɗe shi ba."

        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            "Na buɗe ${match.second}."
        } catch (_: Exception) {
            "Na sami ${match.second}, amma ban iya buɗe shi ba."
        }
    }

    private fun normalize(value: String): String {
        return value
            .lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9]+"), "")
    }
}
