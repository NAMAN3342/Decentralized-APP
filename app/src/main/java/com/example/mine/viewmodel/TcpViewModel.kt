package com.example.mine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mine.network.tcp.TcpConnectionStatus
import com.example.mine.network.tcp.TcpManager
import com.example.mine.network.tcp.TcpMessage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TcpViewModel : ViewModel() {

    private val tcpManager = TcpManager()

    val connectionStatus: StateFlow<TcpConnectionStatus> = tcpManager.connectionStatus
    val receivedMessages: StateFlow<List<TcpMessage>> = tcpManager.receivedMessages
    val connectedNodeId: StateFlow<String?> = tcpManager.connectedNodeId

    fun connect(ip: String, port: Int = TcpManager.DEFAULT_PORT) {
        tcpManager.connectToFusionNode(ip, port)
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            tcpManager.sendMessage(message)
        }
    }

    fun disconnect() {
        tcpManager.disconnect()
    }

    fun clearMessages() {
        tcpManager.clearMessages()
    }

    fun isConnected(): Boolean = tcpManager.isConnected()
    fun getNodeId(): String? = tcpManager.getCurrentNodeId()
}
