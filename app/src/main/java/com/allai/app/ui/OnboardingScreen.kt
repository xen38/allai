package com.allai.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.allai.app.theme.AllaiTheme
import com.allai.app.settings.SettingsState
import com.allai.app.network.LlmClient

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val settings = SettingsState(context)
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("nvidia/nemotron-3-ultra-550b-a55b") }
    var baseUrl by remember { mutableStateOf("https://integrate.api.nvidia.com/v1") }
    var provider by remember { mutableStateOf("custom") }
    var dropdownOpen by remember { mutableStateOf(false) }

    val providers = listOf("openai", "anthropic", "google", "custom")
    val scope = remember { CoroutineScope(Dispatchers.Main) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon / Logo
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🫧", fontSize = 64.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Allai",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your personal AI companion that lives inside your device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Provider selection
        Text(
            text = "LLM Provider",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        DropdownMenu(
            expanded = dropdownOpen,
            onDismissRequest = { dropdownOpen = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            providers.forEach { p ->
                DropdownMenuItem(
                    text = { Text(text = p.uppercase()) },
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
            shape = MaterialTheme.shapes.large
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

        Spacer(modifier = Modifier.height(16.dp))

        // API Key
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(text = "API Key") },
            placeholder = { Text(text = "sk-… or nvapi-…") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Model
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text(text = "Model") },
            placeholder = { Text(text = "nvidia/nemotron-3-ultra-550b-a55b") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Base URL
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text(text = "Base URL") },
            placeholder = { Text(text = "https://integrate.api.nvidia.com/v1") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (apiKey.isNotBlank()) {
                    settings.save(
                        providerName = provider,
                        apiKey = apiKey,
                        model = model,
                        baseUrl = baseUrl,
                        systemPrompt = settings.systemPrompt
                    )
                    scope.launch {
                        try {
                            val models = LlmClient.fetchModels(settings.provider)
                            if (models.isNotEmpty()) {
                                settings.model = models.first()
                            }
                        } catch (e: Exception) { }
                    }
                    onComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            enabled = apiKey.isNotBlank()
        ) {
            Text(text = "Get Started")
        }
    }
}
