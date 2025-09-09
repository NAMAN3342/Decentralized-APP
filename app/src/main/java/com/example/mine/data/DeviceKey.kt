package com.example.mine.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_keys")
data class DeviceKey(
    @PrimaryKey val deviceId: String,
    val publicKey: String,
    val privateKey: String,
    val createdAt: Long = System.currentTimeMillis()
)
