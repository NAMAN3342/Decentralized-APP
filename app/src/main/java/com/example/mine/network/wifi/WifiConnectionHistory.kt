package com.example.mine.network.wifi

/**
 * Model representing a past Wi-Fi connection session.
 */
data class WifiConnectionHistory(
    val ssid: String,
    val bssid: String,
    val securityType: String,
    val lastConnected: Long,
    val connectionCount: Int,
    val signalStrength: Int,
    val isCurrentlyConnected: Boolean
) {
    fun getSignalBars(): Int = when {
        signalStrength >= -50 -> 5
        signalStrength >= -60 -> 4
        signalStrength >= -70 -> 3
        signalStrength >= -80 -> 2
        signalStrength >= -90 -> 1
        else -> 0
    }
}
