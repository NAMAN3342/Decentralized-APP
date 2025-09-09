package com.example.mine.network.wifi

/**
 * Aggregated Wi-Fi usage statistics.
 */
data class WifiUsageStatistics(
    val totalNetworks: Int,
    val currentlyConnected: String,
    val mostFrequentNetwork: String,
    val totalConnectionTime: Long,
    val averageSignalStrength: Double,
    val networksBySecurityType: Map<String, List<WifiConnectionHistory>>,
    val lastConnectedNetwork: String
) {
    fun getFormattedTotalConnectionTime(): String {
        val hours = totalConnectionTime / (1000 * 60 * 60)
        val minutes = (totalConnectionTime / (1000 * 60)) % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun getAverageSignalBars(): Int = when {
        averageSignalStrength >= -50 -> 5
        averageSignalStrength >= -60 -> 4
        averageSignalStrength >= -70 -> 3
        averageSignalStrength >= -80 -> 2
        averageSignalStrength >= -90 -> 1
        else -> 0
    }

    fun getMostCommonSecurityType(): String =
        networksBySecurityType.maxByOrNull { it.value.size }?.key ?: "Unknown"
}
