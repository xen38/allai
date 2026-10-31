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
import com.allai.app.agent.AgentLoop
import com.allai.app.model.LlmProvider
import com.allai.app.plugin.PluginManager
import com.allai.app.settings.SettingsState

@Composable
fun AgentScreen(settings: SettingsState) {
    val messages = remember { mutableStateListOf<Message>() }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val agent = remember { AgentLoop() }

    val initialized = remember { mutableStateOf(false) }
    if (!initialized.value) {
        initialized.value = true
        messages.add(Message(Role.System, settings.systemPrompt))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp, 16.dp, 16.dp, 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.hashCode().toString() }) { msg ->
                when (msg.role) {
                    Role.User -> UserBubble(msg.text)
                    Role.Agent -> AgentBubble(msg.text)
                    Role.Tool -> ToolBubble(msg.text)
                    Role.System -> {}
                }
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
                    placeholder = { Text(text = "Message Allai…") },
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
                                val config = AgentLoop.AgentConfig(
                                    provider = settings.provider,
                                    systemPrompt = settings.systemPrompt
                                )
                                val reply = agent.run(config, messages.toList())
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
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column {
            Text(
                text = text,
                modifier = Modifier
                    .padding(12.dp, 8.dp)
                    .background(Color(0xFF1E293B), MaterialTheme.shapes.large)
                    .padding(horizontal = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AgentBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column {
            Text(
                text = text,
                modifier = Modifier
                    .padding(12.dp, 8.dp)
                    .background(Color(0xFF1E1B4B), MaterialTheme.shapes.large)
                    .padding(horizontal = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ToolBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column {
            Text(
                text = text,
                modifier = Modifier
                    .padding(12.dp, 8.dp)
                    .background(Color(0xFF14532D), MaterialTheme.shapes.large)
                    .padding(horizontal = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
