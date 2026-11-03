package com.allai.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.allai.app.model.Message
import com.allai.app.model.Role
import com.allai.app.agent.AgentLoop
import com.allai.app.model.LlmProvider
import com.allai.app.settings.SettingsState
import com.allai.app.floating.FloatingWindowService
import com.allai.app.network.LlmClient
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.allai.app.theme.*

@Composable
fun AgentScreen(settings: SettingsState) {
    val messages = remember { mutableStateListOf<Message>() }
    var input by remember { mutableStateOf("") }
    var currentModel by remember { mutableStateOf(settings.model) }
    var showingModelPicker by remember { mutableStateOf(false) }
    var availableModels = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val initialized = remember { mutableStateOf(false) }
    if (!initialized.value) {
        initialized.value = true
        messages.add(Message(Role.System, settings.systemPrompt))
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    val agentLoop = remember { AgentLoop() }

    val fetchModels = {
        scope.launch {
            try {
                val models = LlmClient.fetchModels(settings.provider)
                if (models.isNotEmpty()) {
                    availableModels.clear()
                    availableModels.addAll(models)
                }
            } catch (e: Exception) {
                // Ignore fetch errors, will use static fallback
            }
        }
    }

    val sendMessage = {
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
                    val reply = agentLoop.run(config, messages.toList())
                    messages.add(Message(Role.Agent, reply))
                } catch (e: Exception) {
                    messages.add(Message(Role.Agent, "Error: ${e.message}"))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AllaiBackground)) {
        val context = LocalContext.current

        // Top bar: context button (left) + bubble button (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 8.dp, 16.dp, 0.dp)
        ) {
            // Context button - top left
            IconButton(
                onClick = { /* show context menu with token consumption */ }
            ) {
                Text(text = "📊", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bubble button - top right
            IconButton(
                onClick = {
                    if (!Settings.canDrawOverlays(context)) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } else {
                        FloatingWindowService.start(context)
                    }
                }
            ) {
                Text(text = "🫧", fontSize = 18.sp)
            }
        }

        // Chat area - full empty box top-to-bottom
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 56.dp, 16.dp, 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

        // Bottom: model selector + chatbox + controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp, 16.dp, 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column {
                // Model selector button attached above chatbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Model selector + workspace + new session buttons row
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Model selector button
                        OutlinedButton(
                            onClick = {
                                fetchModels()
                                showingModelPicker = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentModel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "▼",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // Workspace button
                        IconButton(
                            onClick = { },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text(text = "🏢", fontSize = 18.sp)
                        }

                        // New session button
                        IconButton(
                            onClick = {
                                messages.clear()
                                messages.add(Message(Role.System, settings.systemPrompt))
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text(text = "📄", fontSize = 18.sp)
                        }
                    }

                    // Send button
                    IconButton(
                        onClick = { sendMessage() },
                        modifier = Modifier.size(40.dp),
                        enabled = input.isNotBlank()
                    ) {
                        Text(text = "▶", color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Chatbox input
                Row(modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .padding(vertical = 10.dp)
                            .background(
                                Color(0xFF1A102E),
                                MaterialTheme.shapes.large
                            )
                            .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
                            .padding(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp)
                    )
                }
            }
        }

        // Model picker dropdown (overlay)
        if (showingModelPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x000000).copy(alpha = 0.3f))
                    .clickable { showingModelPicker = false },
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp, 56.dp, 16.dp, 0.dp)
                        .width(240.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val models = if (availableModels.isNotEmpty()) availableModels else listOf(
                        "meta/llama-3.1-8b-instruct",
                        "meta/llama-3.1-70b-instruct",
                        "meta/llama-3.1-405b-instruct",
                        "nvidia/nemotron-3-405b-a55b",
                        "nvidia/nemotron-3-ultra-550b-a55b",
                        "google/gemma-2-27b-it",
                        "mistralai/mistral-7b-instruct-v0.3",
                        "anthropic/claude-3.5-sonnet",
                        "openai/gpt-4o",
                        "custom"
                    )

                    models.forEach { model ->
                        TextButton(
                            onClick = {
                                currentModel = model
                                settings.model = model
                                showingModelPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = model,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (model == currentModel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (model == currentModel) {
                                    Text(text = "✓", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
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
                    .padding(16.dp, 10.dp)
                    .background(
                        Brush.linearGradient(colors = listOf(Color(0xFF4A148C), Color(0xFF7B2CBF))),
                        MaterialTheme.shapes.large
                    )
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
                    .padding(16.dp, 10.dp)
                    .background(
                        Brush.linearGradient(colors = listOf(Color(0xFF003366), Color(0xFF00D4FF))),
                        MaterialTheme.shapes.large
                    )
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
                    .background(Color(0xFF1A102E), MaterialTheme.shapes.large)
                    .border(1.dp, Color(0xFF00D4FF), MaterialTheme.shapes.large)
                    .padding(horizontal = 4.dp),
                color = Color(0xFF00D4FF),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
