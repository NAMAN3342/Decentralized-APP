package com.example.mine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mine.network.wifi.FusionWifiNetwork
import com.example.mine.network.wifi.WifiDiscoveryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WifiViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiDiscoveryManager = WifiDiscoveryManager(application)

    private val _wifiNetworks = MutableStateFlow<List<FusionWifiNetwork>>(emptyList())
    val wifiNetworks: StateFlow<List<FusionWifiNetwork>> = _wifiNetworks

    private val _isWifiEnabled = MutableStateFlow(wifiDiscoveryManager.isWifiEnabled())
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled

    private val _currentConnection = MutableStateFlow<FusionWifiNetwork?>(wifiDiscoveryManager.getCurrentConnection())
    val currentConnection: StateFlow<FusionWifiNetwork?> = _currentConnection

    fun refreshNetworks() {
        viewModelScope.launch {
            wifiDiscoveryManager.scanForNetworks()
                .catch { e -> e.printStackTrace() }
                .collectLatest { results ->
                    _wifiNetworks.value = results
                }
        }
    }

    fun checkWifiStatus() {
        _isWifiEnabled.value = wifiDiscoveryManager.isWifiEnabled()
        _currentConnection.value = wifiDiscoveryManager.getCurrentConnection()
    }
}
