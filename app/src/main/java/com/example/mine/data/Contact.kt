package com.example.mine.data

data class Contact(
    val id: String,
    val name: String,
    val publicKey: String,
    val deviceId: String,
    val lastSeen: Long = System.currentTimeMillis()
)
