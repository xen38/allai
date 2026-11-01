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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.allai.app.theme.*

@Composable
fun AgentScreen(settings: SettingsState) {
    val messages = remember { mutableStateListOf<Message>() }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val agent = remember { AgentLoop() }
    val focusRequester = remember { FocusRequester() }

    val initialized = remember { mutableStateOf(false) }
    if (!initialized.value) {
        initialized.value = true
        messages.add(Message(Role.System, settings.systemPrompt))
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
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
                    val reply = agent.run(config, messages.toList())
                    messages.add(Message(Role.Agent, reply))
                } catch (e: Exception) {
                    messages.add(Message(Role.Agent, "Error: ${e.message}"))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AllaiBackground)) {
        val context = LocalContext.current

        BubbleButton(
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
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 72.dp, 16.dp, 80.dp),
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

        Box(
            modifier = Modifier
                .padding(16.dp, 16.dp, 16.dp, 16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column {
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
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        sendMessage()
                    }) {
                        Text(text = "▶", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp)
        ) {
            Text(text = "🫧", fontSize = 18.sp)
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
