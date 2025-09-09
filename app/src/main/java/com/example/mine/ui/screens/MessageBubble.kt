package com.example.mine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MessageBubble(message: ChatMessage) {
    val bubbleColor = if (message.isSentByMe) MaterialTheme.colorScheme.primary else Color(0xFF374151)
    val textColor = if (message.isSentByMe) Color.White else Color.White

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isSentByMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(bubbleColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
                .widthIn(max = 250.dp)
        ) {
            Text(text = message.text, color = textColor)
        }
    }
}
