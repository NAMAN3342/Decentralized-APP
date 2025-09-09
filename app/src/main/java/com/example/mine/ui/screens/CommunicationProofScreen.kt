package com.example.mine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mine.data.CommunicationProof
import com.example.mine.data.ProofType
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun CommunicationProofScreen(
    proofs: List<CommunicationProof>,
    dateFormat: SimpleDateFormat
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Communication Proofs",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (proofs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No proofs available",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(proofs) { proof ->
                    ProofCard(proof = proof, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
fun ProofCard(proof: CommunicationProof, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Proof type + status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (proof.proofType) {
                            ProofType.WIFI -> Icons.Default.Wifi
                            ProofType.BLUETOOTH -> Icons.Default.Bluetooth
                            else -> Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = if (proof.success) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = proof.proofType.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = if (proof.success) "Success" else "Failed",
                    color = if (proof.success) Color(0xFF10B981) else Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(Icons.Default.Devices, "Device Name", proof.deviceName ?: "Unknown", Color(0xFF06B6D4))
            InfoRow(Icons.Default.Fingerprint, "Device ID", proof.deviceId.toString(), Color(0xFF8B5CF6))

            val connType = proof.connectionType?.lowercase(Locale.getDefault()) ?: "unknown"
            InfoRow(
                icon = if (connType == "wifi") Icons.Default.Wifi else Icons.Default.Bluetooth,
                label = "Connection Type",
                value = connType.uppercase(Locale.getDefault()),
                color = if (connType == "wifi") Color(0xFF8B5CF6) else Color(0xFF06B6D4)
            )

            InfoRow(Icons.Default.Speed, "Latency", "${proof.latency} ms", Color(0xFF3B82F6))
            InfoRow(Icons.Default.AccessTime, "Timestamp", dateFormat.format(proof.timestamp), Color(0xFF9CA3AF))

            if (!proof.success && proof.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Error: ${proof.errorMessage}",
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
fun ProofLoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing))
    )

    Icon(
        imageVector = Icons.Default.Sync,
        contentDescription = null,
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer(rotationZ = rotation),
        tint = Color(0xFF3B82F6)
    )
}
