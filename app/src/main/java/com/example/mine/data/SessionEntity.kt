package com.example.mine.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val contactId: String,
    val publicKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis()
)
