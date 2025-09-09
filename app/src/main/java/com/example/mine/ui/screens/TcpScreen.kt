package com.example.mine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mine.viewmodel.TcpViewModel
import com.example.mine.network.tcp.TcpMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TcpScreen(
    tcpViewModel: TcpViewModel = viewModel()
) {
    val messages by tcpViewModel.receivedMessages.collectAsState()
    val connectionStatus by tcpViewModel.connectionStatus.collectAsState()
    val connectedNodeId by tcpViewModel.connectedNodeId.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "TCP Messages",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Connection status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Status: ${getStatusText(connectionStatus)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                connectedNodeId?.let { nodeId ->
                    Text(
                        text = "Connected to: $nodeId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                TcpMessageItem(msg)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Connection controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { tcpViewModel.disconnect() },
                enabled = tcpViewModel.isConnected()
            ) {
                Text("Disconnect")
            }
            
            Button(
                onClick = { tcpViewModel.clearMessages() }
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
fun TcpMessageItem(message: TcpMessage) {
    // Generate a readable label from the enum
    val typeLabel = remember(message.type) {
        message.type.name
            .lowercase(Locale.getDefault())
            .replace('_', ' ')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getStatusText(status: com.example.mine.network.tcp.TcpConnectionStatus): String {
    return when (status) {
        is com.example.mine.network.tcp.TcpConnectionStatus.Connected -> "Connected"
        is com.example.mine.network.tcp.TcpConnectionStatus.Connecting -> "Connecting..."
        is com.example.mine.network.tcp.TcpConnectionStatus.Disconnected -> "Disconnected"
        is com.example.mine.network.tcp.TcpConnectionStatus.Failed -> "Failed: ${status.error}"
        is com.example.mine.network.tcp.TcpConnectionStatus.ConnectedWithNodeId -> "Connected to ${status.nodeId}"
    }
}
