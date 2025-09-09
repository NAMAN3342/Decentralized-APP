package com.example.mine.data

import java.util.Date

data class CommunicationProof(
    val proofType: ProofType,
    val success: Boolean,
    val deviceName: String? = null,
    val deviceId: Int,
    val connectionType: String? = null,
    val latency: Long = 0,
    val timestamp: Date,
    val errorMessage: String? = null
)
