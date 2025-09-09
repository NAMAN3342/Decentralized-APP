package com.example.mine.data

import java.util.*

/**
 * Represents a chat message with metadata.
 */
data class Message(
    val id: String = UUID.randomUUID().toString(), // unique message ID
    val senderId: Int,             // device ID of sender
    val content: String,           // actual message text
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.PENDING,
    val isEncrypted: Boolean = false,  // true if encrypted
    val isCompressed: Boolean = false  // true if compressed
)
