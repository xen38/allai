package com.allai.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.allai.app.theme.AllaiTheme
import com.allai.app.settings.SettingsState

@Composable
fun SettingsTabsScreen(settings: SettingsState, onTabSelected: (Int) -> Unit) {
    var selected by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            when (selected) {
                0 -> ProviderSettingsScreen(settings)
                1 -> AgentSettingsScreen(settings)
                2 -> SecuritySettingsScreen()
                3 -> AccountSettingsScreen()
                4 -> IntegrationsSettingsScreen()
            }
        }

        TabRow(
            selectedTabIndex = selected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selected == 0,
                onClick = { selected = 0; onTabSelected(0) },
                text = { Text(text = "Provider") }
            )
            Tab(
                selected = selected == 1,
                onClick = { selected = 1; onTabSelected(1) },
                text = { Text(text = "Agent") }
            )
            Tab(
                selected = selected == 2,
                onClick = { selected = 2; onTabSelected(2) },
                text = { Text(text = "Security") }
            )
            Tab(
                selected = selected == 3,
                onClick = { selected = 3; onTabSelected(3) },
                text = { Text(text = "Account") }
            )
            Tab(
                selected = selected == 4,
                onClick = { selected = 4; onTabSelected(4) },
                text = { Text(text = "Integrations") }
            )
        }
    }
}

@Composable
fun ProviderSettingsScreen(settings: SettingsState) {
    var provider by remember { mutableStateOf(settings.providerName) }
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var model by remember { mutableStateOf(settings.model) }
    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var systemPrompt by remember { mutableStateOf(settings.systemPrompt) }
    var dropdownOpen by remember { mutableStateOf(false) }

    val providers = listOf("openai", "anthropic", "google", "custom")

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "LLM Provider", style = MaterialTheme.typography.titleLarge)
        Text(text = "Configure which AI provider Allai uses to think.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Provider", style = MaterialTheme.typography.labelLarge)

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
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(text = "Model") },
                    placeholder = { Text(text = "nvidia/nemotron-3-ultra-550b-a55b") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(text = "Base URL") },
                    placeholder = { Text(text = "https://integrate.api.nvidia.com/v1") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
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
                    shape = MaterialTheme.shapes.large,
                    enabled = apiKey.isNotBlank()
                ) {
                    Text(text = "Save Provider Settings")
                }
            }
        }
    }
}

@Composable
fun AgentSettingsScreen(settings: SettingsState) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Agent", style = MaterialTheme.typography.titleLarge)
        Text(text = "Memories, skills, and everything the Allai agent knows about you.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Memories", style = MaterialTheme.typography.labelLarge)
                Text(text = "Allai learns from your actions and stores memories so it can replicate them.", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(text = "Open Memories Tab")
                }

                Text(text = "Skills", style = MaterialTheme.typography.labelLarge)
                Text(text = "Teach Allai new capabilities it can perform on your phone.", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(text = "Manage Skills")
                }

                Text(text = "Custom Instructions", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = settings.systemPrompt,
                    onValueChange = { settings.systemPrompt = it },
                    label = { Text(text = "Agent Instructions") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    }
}

@Composable
fun SecuritySettingsScreen() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Security", style = MaterialTheme.typography.titleLarge)
        Text(text = "All permissions and security-related settings for Allai.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Permissions", style = MaterialTheme.typography.labelLarge)
                SecurityRow(title = "Accessibility", subtitle = "Read/interact with any app screen", enabled = true, onToggle = { })
                SecurityRow(title = "Camera", subtitle = "Take photos and scan", enabled = true, onToggle = { })
                SecurityRow(title = "Microphone", subtitle = "Voice commands", enabled = true, onToggle = { })
                SecurityRow(title = "Contacts", subtitle = "Read and message contacts", enabled = true, onToggle = { })
                SecurityRow(title = "SMS", subtitle = "Send and read text messages", enabled = true, onToggle = { })
                SecurityRow(title = "Files", subtitle = "Read and write device storage", enabled = true, onToggle = { })
                SecurityRow(title = "Location", subtitle = "Know where you are", enabled = true, onToggle = { })
                SecurityRow(title = "Install Apps", subtitle = "Install new applications", enabled = true, onToggle = { })
                SecurityRow(title = "Overlay", subtitle = "Show floating window on top", enabled = true, onToggle = { })
            }
        }
    }
}

@Composable
private fun SecurityRow(title: String, subtitle: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

@Composable
fun AccountSettingsScreen() {
    val context = LocalContext.current
    var googleAccount by remember { mutableStateOf<GoogleSignInAccount?>(null) }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                googleAccount = task.result
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Account", style = MaterialTheme.typography.titleLarge)
        Text(text = "Log in with your Google account like on any AI provider.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Sign in", style = MaterialTheme.typography.labelLarge)

                if (googleAccount != null) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Signed in as:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = googleAccount!!.displayName ?: "Unknown", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = googleAccount!!.email ?: "No email", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = {
                                googleSignInClient.signOut()
                                googleAccount = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(text = "Sign out")
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            val intent = googleSignInClient.signInIntent
                            signInLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔑", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Continue with Google", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Divider()

                Text(text = "Or sign in with email", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text(text = "Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text(text = "Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(text = "Sign in")
                }
            }
        }
    }
}

@Composable
fun IntegrationsSettingsScreen() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Integrations", style = MaterialTheme.typography.titleLarge)
        Text(text = "All the possible integrations Allai can connect to on Android.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                IntegrationRow(title = "Shell / Terminal", subtitle = "Run commands on your device", enabled = true)
                IntegrationRow(title = "File System", subtitle = "Read and write files", enabled = true)
                IntegrationRow(title = "Contacts", subtitle = "Access your contacts", enabled = true)
                IntegrationRow(title = "SMS", subtitle = "Send text messages", enabled = true)
                IntegrationRow(title = "App Launcher", subtitle = "Open any installed app", enabled = true)
                IntegrationRow(title = "MCP Servers", subtitle = "Connect to Model Context Protocol servers", enabled = true)
                IntegrationRow(title = "Accessibility", subtitle = "Interact with other apps' UI", enabled = true)
            }
        }
    }
}

@Composable
private fun IntegrationRow(title: String, subtitle: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { },
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}
