package app.repdawn.android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Every optional remote call must pass this foreground-only gate. */
class WifiGate(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    fun hasValidatedWifi(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    fun requireValidatedWifi() { check(hasValidatedWifi()) { "Connect to validated Wi-Fi before using an optional online service." } }
}
