package com.cesi.assistant.features.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LocationController(private val context: Context) {
    fun location(): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return "Ina bukatar permission na location."
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // First try a fresh fix on Android 11+ so CESI does not depend only
        // on a possibly empty/stale last-known-location cache.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            for (provider in providers) {
                try {
                    if (!lm.isProviderEnabled(provider)) continue
                    val latch = CountDownLatch(1)
                    var result: Location? = null
                    val callbackExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
                    try {
                        lm.getCurrentLocation(
                            provider,
                            null,
                            callbackExecutor
                        ) { location ->
                            result = location
                            latch.countDown()
                        }
                        if (latch.await(3, TimeUnit.SECONDS)) {
                            result?.let { return format(it) }
                        }
                    } finally {
                        callbackExecutor.shutdownNow()
                    }
                } catch (_: SecurityException) {
                    break
                } catch (_: Exception) {
                    // Try the next provider.
                }
            }
        }

        // Fallback for older Android versions or when a fresh fix is unavailable.
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                return format(loc)
            } catch (_: SecurityException) {
                break
            } catch (_: Exception) {}
        }

        return "Ban samu sabon location ba. Ka tabbatar Location/GPS a kunne yake, sannan ka sake tambaya."
    }

    private fun format(location: Location): String =
        String.format(
            Locale.US,
            "Location ɗinka: latitude %.5f, longitude %.5f.",
            location.latitude,
            location.longitude
        )
}
