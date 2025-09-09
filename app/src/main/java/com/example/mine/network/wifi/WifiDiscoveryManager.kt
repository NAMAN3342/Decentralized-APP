package com.example.mine.network.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Handles scanning & discovering Wi-Fi networks (modern APIs).
 */
class WifiDiscoveryManager(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Emits a Flow of available Wi-Fi networks when scanned.
     */
    fun scanForNetworks(): Flow<List<FusionWifiNetwork>> = callbackFlow {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            trySend(emptyList()) // No permission → empty result
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    // ✅ Double check permission before accessing scanResults
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val results = wifiManager.scanResults.map { it.toFusionWifiNetwork() }
                        trySend(results)
                    } else {
                        // No permission → send empty list or ignore
                        trySend(emptyList())
                    }
                }
            }
        }


        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan() // still works, but limited on Android 9+

        awaitClose { context.unregisterReceiver(receiver) }
    }

    /**
     * Check if Wi-Fi is enabled.
     */
    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled

    /**
     * Request current connection (modern way).
     */
    fun getCurrentConnection(): FusionWifiNetwork? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val network = connectivityManager.activeNetwork ?: return null
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return null

        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null

        val wifiInfo = wifiManager.connectionInfo ?: return null
        return FusionWifiNetwork(
            ssid = wifiInfo.ssid.removePrefix("\"").removeSuffix("\""),
            bssid = wifiInfo.bssid ?: "",
            rssi = wifiInfo.rssi,
            frequency = wifiInfo.frequency,
            capabilities = "",
            securityType = "Unknown",
            channel = wifiInfo.networkId,
            isFusionNode = wifiInfo.ssid.contains("Fusion", true)
        )
    }

    /**
     * Extension to convert ScanResult → FusionWifiNetwork
     */
    private fun ScanResult.toFusionWifiNetwork(): FusionWifiNetwork {
        val security = when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA") -> "WPA"
            capabilities.contains("WEP") -> "WEP"
            else -> "Open"
        }
        return FusionWifiNetwork(
            ssid = SSID ?: "",
            bssid = BSSID ?: "",
            rssi = level,
            frequency = frequency,
            capabilities = capabilities,
            securityType = security,
            channel = frequency,
            isFusionNode = (SSID ?: "").contains("Fusion", true)
        )
    }
}
