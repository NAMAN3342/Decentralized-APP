package com.example.mine.network.wifi

/**
 * Represents current Wi-Fi connection status.
 */
sealed class WifiConnectionStatus {
    object Disconnected : WifiConnectionStatus()
    object Scanning : WifiConnectionStatus()
    object Connecting : WifiConnectionStatus()
    data class Connected(val network: FusionWifiNetwork) : WifiConnectionStatus()
    data class Failed(val reason: String) : WifiConnectionStatus()
}
