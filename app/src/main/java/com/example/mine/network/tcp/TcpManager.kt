package com.example.mine.network.tcp

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.*
import java.net.Socket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

class TcpManager {
    private val TAG = "TcpManager"

    // State flows
    private val _connectionStatus = MutableStateFlow<TcpConnectionStatus>(TcpConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<TcpConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receivedMessages = MutableStateFlow<List<TcpMessage>>(emptyList())
    val receivedMessages: StateFlow<List<TcpMessage>> = _receivedMessages.asStateFlow()

    private val _connectedNodeId = MutableStateFlow<String?>(null)
    val connectedNodeId: StateFlow<String?> = _connectedNodeId.asStateFlow()

    // TCP objects
    private var socket: Socket? = null
    private var inputStream: BufferedReader? = null
    private var outputStream: PrintWriter? = null
    private val isConnected = AtomicBoolean(false)
    private var connectionJob: Job? = null

    companion object {
        const val DEFAULT_PORT = 18080
        const val CONNECTION_TIMEOUT = 5000 // 5 seconds
        const val READ_TIMEOUT = 3000 // 3 seconds
    }

    /**
     * Connect to a fusion node via TCP
     */
    fun connectToFusionNode(ipAddress: String, port: Int = DEFAULT_PORT) {
        if (isConnected.get()) {
            Log.w(TAG, "⚠️ Already connected")
            return
        }

        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🚀 Connecting to $ipAddress:$port")
                _connectionStatus.value = TcpConnectionStatus.Connecting

                socket = Socket()
                socket?.connect(InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT)
                socket?.soTimeout = READ_TIMEOUT

                if (socket?.isConnected == true) {
                    Log.d(TAG, "✅ Connected")
                    _connectionStatus.value = TcpConnectionStatus.Connected
                    isConnected.set(true)

                    inputStream = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                    outputStream = PrintWriter(socket!!.getOutputStream(), true)

                    startMessageListener()
                    sendHandshake()

                } else {
                    Log.e(TAG, "❌ Socket not connected")
                    _connectionStatus.value = TcpConnectionStatus.Failed("Connection failed")
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error connecting: ${e.message}")
                _connectionStatus.value = TcpConnectionStatus.Failed(e.message ?: "Unknown error")
                disconnect()
            }
        }
    }

    /**
     * Start listener coroutine
     */
    private fun startMessageListener() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isConnected.get() && socket?.isConnected == true) {
                    val message = inputStream?.readLine()
                    if (!message.isNullOrBlank()) {
                        processReceivedMessage(message)
                    }
                    delay(10)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Listener error: ${e.message}")
                if (isConnected.get()) {
                    _connectionStatus.value = TcpConnectionStatus.Failed("Connection lost: ${e.message}")
                    disconnect()
                }
            }
        }
    }

    /**
     * Process received messages
     */
    private fun processReceivedMessage(message: String) {
        Log.d(TAG, "🔍 Received: $message")

        try {
            val json = JSONObject(message)
            val messageType = json.optString("type", "")

            when (messageType) {
                "NODE_ID" -> {
                    val nodeId = json.optString("id", "")
                    if (nodeId.isNotBlank()) {
                        _connectedNodeId.value = nodeId
                        _connectionStatus.value = TcpConnectionStatus.ConnectedWithNodeId(nodeId)
                        addMessage(TcpMessage(TcpMessageType.NODE_ID, nodeId, now(), message))
                    }
                }

                "HANDSHAKE_RESPONSE", "INFO" -> {
                    val nodeId = json.optString("node_id", json.optString("id", ""))
                    if (nodeId.isNotBlank()) {
                        _connectedNodeId.value = nodeId
                        _connectionStatus.value = TcpConnectionStatus.ConnectedWithNodeId(nodeId)
                    }
                }

                else -> {
                    addMessage(TcpMessage(TcpMessageType.OTHER, message, now(), message))
                }
            }

        } catch (e: Exception) {
            Log.w(TAG, "❌ JSON parse failed, treating as plain text: $message")
            val extractedId = extractNodeIdFromPlainText(message)
            if (extractedId.isNotBlank()) {
                _connectedNodeId.value = extractedId
                _connectionStatus.value = TcpConnectionStatus.ConnectedWithNodeId(extractedId)
            }
            addMessage(TcpMessage(TcpMessageType.PLAIN_TEXT, message, now(), message))
        }
    }

    /**
     * Extract node ID from plain text
     */
    private fun extractNodeIdFromPlainText(message: String): String {
        val patterns = listOf(
            Regex("Node ID:\\s*([A-Za-z0-9_-]+)", RegexOption.IGNORE_CASE),
            Regex("NodeID:\\s*([A-Za-z0-9_-]+)", RegexOption.IGNORE_CASE),
            Regex("ID:\\s*([A-Za-z0-9_-]+)", RegexOption.IGNORE_CASE),
            Regex("node_id:\\s*([A-Za-z0-9_-]+)", RegexOption.IGNORE_CASE),
            Regex("FusionNode[_-]?([A-Za-z0-9_-]+)", RegexOption.IGNORE_CASE),
            Regex("ESP32[_-]?([A-Za-z0-9_-]+)", RegexOption.IGNORE_CASE)
        )
        return patterns.firstNotNullOfOrNull { it.find(message)?.groupValues?.get(1) } ?: ""
    }

    /**
     * Add message to flow
     */
    private fun addMessage(msg: TcpMessage) {
        _receivedMessages.value = _receivedMessages.value + msg
    }

    /**
     * Send handshake message
     */
    private fun sendHandshake() {
        try {
            val handshake = JSONObject().apply {
                put("type", "HANDSHAKE")
                put("client", "android")
                put("version", "1.0")
            }
            outputStream?.println(handshake.toString())
            Log.d(TAG, "🤝 Sent handshake: $handshake")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending handshake: ${e.message}")
        }
    }

    /**
     * Send custom message
     */
    fun sendMessage(message: String): Boolean {
        return try {
            if (isConnected.get() && outputStream != null) {
                outputStream?.println(message)
                Log.d(TAG, "📤 Sent: $message")
                true
            } else {
                Log.w(TAG, "⚠️ Cannot send, not connected")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Send error: ${e.message}")
            false
        }
    }

    /**
     * Disconnect safely
     */
    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting...")
        isConnected.set(false)
        connectionJob?.cancel()

        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}")
        }

        inputStream = null
        outputStream = null
        socket = null
        _connectionStatus.value = TcpConnectionStatus.Disconnected
        _connectedNodeId.value = null
        Log.d(TAG, "✅ Disconnected")
    }

    fun isConnected(): Boolean = isConnected.get()
    fun getCurrentNodeId(): String? = _connectedNodeId.value
    fun clearMessages() { _receivedMessages.value = emptyList() }

    private fun now(): Long = System.currentTimeMillis()
}

/* -------------------------------
   Helper classes / types
-------------------------------- */
sealed class TcpConnectionStatus {
    object Disconnected : TcpConnectionStatus()
    object Connecting : TcpConnectionStatus()
    object Connected : TcpConnectionStatus()
    data class ConnectedWithNodeId(val nodeId: String) : TcpConnectionStatus()
    data class Failed(val error: String) : TcpConnectionStatus()
}

enum class TcpMessageType { NODE_ID, OTHER, PLAIN_TEXT }

data class TcpMessage(
    val type: TcpMessageType,
    val content: String,
    val timestamp: Long,
    val rawMessage: String
)
