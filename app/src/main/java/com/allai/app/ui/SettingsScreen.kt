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
    var dropdownOpen by remember { mutableStateOf(false) }

    val providers = listOf("openai", "anthropic", "google", "custom")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.titleLarge)

        Text(text = "LLM Provider", style = MaterialTheme.typography.labelLarge)

        DropdownMenu(
            expanded = dropdownOpen,
            onDismissRequest = { dropdownOpen = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            providers.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.uppercase()) },
                    onClick = {
                        provider = p
                        dropdownOpen = false
                    }
                )
            }
        }

        OutlinedButton(
            onClick = { dropdownOpen = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = provider.uppercase())
                Text(
                    text = "▼",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
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
            placeholder = { Text(text = "Nemotron 3 Ultra 550B A55B") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text(text = "Base URL") },
            placeholder = { Text(text = "https://api.nvidia.com/v1") },
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
            shape = MaterialTheme.shapes.medium,
            enabled = apiKey.isNotBlank()
        ) {
            Text(text = "Save")
        }
    }
}
