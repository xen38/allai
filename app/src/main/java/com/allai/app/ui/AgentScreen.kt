package com.allai.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.allai.app.model.Message
import com.allai.app.model.Role

@Composable
fun AgentScreen() {
    val messages = remember { mutableStateListOf<Message>() }
    val input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(message = msg)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, bottom = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(text = "Type a message…") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == Role.User
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.allaiUserBubble() else MaterialTheme.colorScheme.allaiAgentBubble()
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

private fun ColorScheme.allaiUserBubble(): Color = Color(0xFF1E293B)
private fun ColorScheme.allaiAgentBubble(): Color = Color(0xFF1E1B4B)
