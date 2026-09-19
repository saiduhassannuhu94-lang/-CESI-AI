package com.cesi.assistant.features.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.util.Locale

class LocationController(private val context: Context) {
    fun location(): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return "Ina bukatar permission na location."
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                return String.format(
                    Locale.US,
                    "Location ɗinka kusan latitude %.5f, longitude %.5f.",
                    loc.latitude, loc.longitude
                )
            } catch (_: SecurityException) {}
        }
        return "Ban samu last known location ba."
    }
}
