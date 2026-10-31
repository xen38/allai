package com.allai.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.allai.app.theme.AllaiTheme
import com.allai.app.settings.SettingsState

@Composable
fun SettingsScreen(settings: SettingsState) {
    var provider by remember { mutableStateOf(settings.providerName) }
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var model by remember { mutableStateOf(settings.model) }
    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var systemPrompt by remember { mutableStateOf(settings.systemPrompt) }

    val providers = listOf("openai", "anthropic", "google", "custom")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.titleLarge)

        Text(text = "LLM Provider", style = MaterialTheme.typography.labelLarge)
        androidx.compose.material3.DropdownMenu(
            expanded = true,
            onDismissRequest = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            providers.forEach { p ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(p.uppercase()) },
                    onClick = { provider = p }
                )
            }
        }

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(text = "API Key") },
            placeholder = { Text(text = "sk-…") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text(text = "Model") },
            placeholder = { Text(text = "gpt-4o") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text(text = "Base URL") },
            placeholder = { Text(text = "https://api.openai.com/v1/chat/completions") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Divider()

        Text(text = "System Prompt", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = systemPrompt,
            onValueChange = { systemPrompt = it },
            label = { Text(text = "System Prompt") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = MaterialTheme.shapes.medium
        )

        Button(
            onClick = {
                settings.save(
                    providerName = provider,
                    apiKey = apiKey,
                    model = model,
                    baseUrl = baseUrl,
                    systemPrompt = systemPrompt
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(text = "Save")
        }
    }
}
