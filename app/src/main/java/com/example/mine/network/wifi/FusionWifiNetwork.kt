package com.example.mine.network.wifi

/**
 * Model representing a discovered Wi-Fi network.
 */
data class FusionWifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String,
    val securityType: String,
    val channel: Int,
    val isFusionNode: Boolean = false
) {
    fun getSignalStrength(): String = when {
        rssi >= -50 -> "Excellent"
        rssi >= -60 -> "Very Good"
        rssi >= -70 -> "Good"
        rssi >= -80 -> "Fair"
        rssi >= -90 -> "Poor"
        else -> "Very Poor"
    }

    fun getBand(): String = when (frequency) {
        in 2412..2484 -> "2.4 GHz"
        in 5170..5825 -> "5 GHz"
        in 5925..7125 -> "6 GHz"
        else -> "Unknown"
    }

    fun isSecure(): Boolean = securityType !in listOf("Open", "Unknown")

    fun isRecommended(): Boolean = rssi >= -70 && isSecure()
}
