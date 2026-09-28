package com.cesi.assistant.features.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LocationController(private val context: Context) {
    fun location(): String {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return "Ina bukatar permission na location. Ka ba CESI Location permission sannan ka sake tambaya."

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return "Ban iya samun Location Manager na wayar ba."

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { try { manager.isProviderEnabled(it) } catch (_: Exception) { false } }

        if (providers.isEmpty()) return "Location/GPS a kashe yake. Ka kunna Location a wayar sannan ka sake tambaya."

        for (provider in providers) {
            requestFresh(manager, provider)?.let { return format(it) }
        }

        val cached = providers.mapNotNull {
            try { manager.getLastKnownLocation(it) } catch (_: SecurityException) { null }
        }.maxByOrNull { it.time }

        if (cached != null) return format(cached)
        return "Na samu permission amma ban samu location fix ba. Ka tabbatar Location/GPS a kunne yake sannan ka sake tambaya."
    }

    private fun requestFresh(manager: LocationManager, provider: String): Location? {
        val latch = CountDownLatch(1)
        var result: Location? = null
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                result = location
                latch.countDown()
            }
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        return try {
            manager.requestLocationUpdates(provider, 0L, 0f, listener)
            latch.await(4, TimeUnit.SECONDS)
            result
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        } finally {
            try { manager.removeUpdates(listener) } catch (_: Exception) {}
        }
    }

    private fun format(location: Location): String =
        String.format(Locale.US, "Location ɗinka: latitude %.5f, longitude %.5f. Accuracy kusan %.0f m.",
            location.latitude, location.longitude, location.accuracy)
}