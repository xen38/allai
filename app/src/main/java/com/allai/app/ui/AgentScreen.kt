package com.allai.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import com.allai.app.model.Message
import com.allai.app.model.Role

@Composable
fun AgentScreen() {
    val messages = remember { mutableStateListOf<Message>() }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp, 16.dp, 16.dp, 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.hashCode().toString() }) { msg ->
                MessageBubble(message = msg)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp, 16.dp, 16.dp, 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(text = "Type a message…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (input.isNotBlank()) {
                        val text = input
                        input = ""
                        messages.add(Message(Role.User, text))
                        scope.launch {
                            try {
                                val reply = com.allai.app.network.LlmClient.sendMessage(messages.toList())
                                messages.add(Message(Role.Agent, reply))
                            } catch (e: Exception) {
                                messages.add(Message(Role.Agent, "Error: ${e.message}"))
                            }
                        }
                    }
                }) {
                    Text(text = "Send")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == Role.User
    val bubbleColor = if (isUser) Color(0xFF1E293B) else Color(0xFF1E1B4B)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column {
            Text(
                text = message.text,
                modifier = Modifier
                    .padding(12.dp, 8.dp)
                    .background(bubbleColor, MaterialTheme.shapes.large)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
