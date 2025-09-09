package com.example.mine.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mine.viewmodel.WifiViewModel
import com.example.mine.network.wifi.FusionWifiNetwork

@Composable
fun WifiScreen(
    wifiViewModel: WifiViewModel = viewModel(),
    onNetworkClick: (FusionWifiNetwork) -> Unit = {}
) {
    val networks by wifiViewModel.availableNetworks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Available Wi-Fi Networks",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = { wifiViewModel.startScan() },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Rescan")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (networks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No networks found. Try rescanning.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(networks) { network ->
                    WifiNetworkItem(network, onClick = { onNetworkClick(network) })
                }
            }
        }
    }
}

@Composable
fun WifiNetworkItem(
    network: FusionWifiNetwork,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(text = network.ssid, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Signal: ${network.rssi} dBm | Security: ${network.securityType}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
