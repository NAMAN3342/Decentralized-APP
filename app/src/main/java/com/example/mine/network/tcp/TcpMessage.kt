package com.example.mine.network.tcp

data class TcpMessage(
    val content: String,
    val type: TcpMessageType,
    val rawMessage: String = content,
    val timestamp: Long = System.currentTimeMillis(),
    val json: String? = null
)


