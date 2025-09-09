package com.example.mine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ChatMessage(
    val id: Int,
    val text: String,
    val isSentByMe: Boolean
)

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onShowCommunicationProof: () -> Unit
) {
    val messages = remember {
        mutableStateListOf(
            ChatMessage(1, "Hey! How are you?", true),
            ChatMessage(2, "I'm good! What about you?", false),
            ChatMessage(3, "Working on our secure chat 😎", true)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ChatHeader(onBack = onBack, onShowCommunicationProof = onShowCommunicationProof)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            reverseLayout = false
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        MessageInput { text ->
            val newId = (messages.maxOfOrNull { it.id } ?: 0) + 1
            messages.add(ChatMessage(newId, text, true))
        }
    }
}
