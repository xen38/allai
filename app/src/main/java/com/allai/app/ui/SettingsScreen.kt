package com.allai.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(text = "LLM API key") },
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
            label = { Text(text = "Base URL (optional)") },
            placeholder = { Text(text = "https://api.openai.com/v1") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(text = "Save")
        }
    }
}
