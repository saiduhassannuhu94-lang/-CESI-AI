package com.cesi.assistant.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkGate(context: Context) {
    private val connectivity =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isInternetAvailable(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun requiredInternetMessage(action: String): String =
        "Wannan aikin ($action) yana bukatar internet. Ka kunna Mobile Data ko Wi-Fi sannan ka sake cewa umarnin."
}
