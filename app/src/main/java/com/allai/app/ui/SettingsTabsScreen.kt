package com.allai.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.allai.app.memory.MemoryStore
import com.allai.app.model.Memory
import com.allai.app.model.LearnedAction
import com.allai.app.theme.AllaiTheme
import com.allai.app.settings.SettingsState
import com.allai.app.theme.*
import com.allai.app.network.LlmClient
import com.allai.app.plugin.PluginManager

import kotlin.collections.MutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabsScreen(settings: SettingsState, onTabSelected: (Int) -> Unit) {
    var selected by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(AllaiBackground)) {
        TopAppBar(
            title = { Text(text = "Settings", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AllaiBackground),
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                when (selected) {
                    0 -> ProviderSettingsScreen(settings)
                    1 -> AgentSettingsScreen(settings)
                    2 -> SecuritySettingsScreen()
                    3 -> AccountSettingsScreen()
                    4 -> IntegrationsSettingsScreen()
                }
            }
        }

        PrimaryTabRow(
            selectedTabIndex = selected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selected == 0,
                onClick = { selected = 0 },
                text = { Text(text = "Provider", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            Tab(
                selected = selected == 1,
                onClick = { selected = 1 },
                text = { Text(text = "Agent", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            Tab(
                selected = selected == 2,
                onClick = { selected = 2 },
                text = { Text(text = "Security", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            Tab(
                selected = selected == 3,
                onClick = { selected = 3 },
                text = { Text(text = "Account", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            Tab(
                selected = selected == 4,
                onClick = { selected = 4 },
                text = { Text(text = "Integrations", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }
    }
}

@Composable
fun Modifier.glassCard(): Modifier = this
    .background(
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(
                Color(0xFF1A102E).copy(alpha = 0.8f),
                Color(0xFF0F0A1E).copy(alpha = 0.9f)
            )
        ),
        MaterialTheme.shapes.large
    )
    .border(
        1.dp,
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(
                Color(0xFF7B2CBF).copy(alpha = 0.4f),
                Color(0xFF00D4FF).copy(alpha = 0.4f)
            )
        ),
        MaterialTheme.shapes.large
    )

@Composable
fun ProviderSettingsScreen(settings: SettingsState) {
    var provider by remember { mutableStateOf(settings.providerName) }
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var model by remember { mutableStateOf(settings.model) }
    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var systemPrompt by remember { mutableStateOf(settings.systemPrompt) }
    var dropdownOpen by remember { mutableStateOf(false) }

    val providers = listOf("openai", "anthropic", "google", "custom")
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "LLM Provider",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Configure which AI provider Allai uses to think.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth().glassCard(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A102E))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Provider",
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
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = provider.uppercase(), color = MaterialTheme.colorScheme.onSurface)
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
                    placeholder = { Text(text = "https://integrate.api.nvidia.com/v1/chat/completions") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )

                HorizontalDivider()

                Text(
                    text = "System Prompt",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        scope.launch {
                            try {
                                val models = com.allai.app.network.LlmClient.fetchModels(settings.provider)
                                if (models.isNotEmpty()) {
                                    model = models.first()
                                    settings.model = model
                                }
                            } catch (e: Exception) { }
                        }
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
    val context = LocalContext.current
    val store = MemoryStore.getInstance(context)
    val memoryList = remember { mutableStateListOf<Memory>().apply { addAll(store.getMemories()) } }
    val actionList = remember { mutableStateListOf<LearnedAction>().apply { addAll(store.getLearnedActions()) } }
    var tab by remember { mutableIntStateOf(0) }
    var showingAddMemory by remember { mutableStateOf(false) }
    var newMemTitle by remember { mutableStateOf("") }
    var newMemDesc by remember { mutableStateOf("") }
    var newMemAction by remember { mutableStateOf("") }
    var showingSkillsDialog by remember { mutableStateOf(false) }
    var skillsDialogValue by remember { mutableStateOf(settings.systemPrompt) }

    Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Agent",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Memories, skills, and everything the Allai agent knows about you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PrimaryTabRow(
            selectedTabIndex = tab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text(text = "Memories", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text(text = "Learned Actions", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }

        when (tab) {
            0 -> MemoriesTab(store, memoryList)
            1 -> LearnedActionsTab(store, actionList)
        }

        if (tab == 0) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${memoryList.size} memories",
                    style = MaterialTheme.typography.bodySmall,
                    color = AllaiOnSurfaceVariant
                )
                Button(onClick = { showingAddMemory = true }) {
                    Text(text = "Add Memory")
                }
            }
        }

        if (showingAddMemory) {
            AddMemoryDialog(
                onDismiss = { showingAddMemory = false },
                onSave = { title, desc, action ->
                    store.addMemory(
                        Memory(
                            id = java.util.UUID.randomUUID().toString(),
                            title = title,
                            description = desc,
                            action = action,
                            arguments = ""
                        )
                    )
                    showingAddMemory = false
                    memoryList.clear()
                    memoryList.addAll(store.getMemories())
                },
                title = newMemTitle,
                desc = newMemDesc,
                action = newMemAction,
                onTitleChange = { newMemTitle = it },
                onDescChange = { newMemDesc = it },
                onActionChange = { newMemAction = it }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().glassCard(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A102E))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Skills",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Teach Allai new capabilities it can perform on your phone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        val current = settings.systemPrompt
                        skillsDialogValue = if (skillsDialogValue.isEmpty()) current else ""
                        showingSkillsDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(text = "Manage Skills")
                }

                if (showingSkillsDialog) {
                    SkillsDialog(
                        onDismiss = { showingSkillsDialog = false },
                        onSave = { skillsDialogValue = it; settings.systemPrompt = it; showingSkillsDialog = false },
                        value = skillsDialogValue,
                        onValueChange = { skillsDialogValue = it }
                    )
                }

                Text(
                    text = "Custom Instructions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun MemoriesTab(
    store: MemoryStore,
    memoryList: MutableList<Memory>
) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text(
            text = "Memories",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Allai learns from your actions and stores memories so it can replicate them.",
            style = MaterialTheme.typography.bodyMedium,
            color = AllaiOnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = memoryList as List<Memory>,
                key = { it.id }
            ) { memory: Memory ->
                MemoryCard(
                    memory = memory,
                    store = store,
                    onRefresh = { memoryList.clear(); memoryList.addAll(store.getMemories()) }
                )
            }
        }
    }
}

@Composable
private fun LearnedActionsTab(
    store: MemoryStore,
    actionList: MutableList<LearnedAction>
) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text(
            text = "Learned Actions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Actions Allai has learned from your phone usage and can replicate.",
            style = MaterialTheme.typography.bodyMedium,
            color = AllaiOnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = actionList as List<LearnedAction>,
                key = { it.id }
            ) { action: LearnedAction ->
                LearnedActionCard(
                    action = action,
                    store = store,
                    onRefresh = { actionList.clear(); actionList.addAll(store.getLearnedActions()) }
                )
            }
        }
    }
}

@Composable
private fun SecuritySettingsScreen() {
    Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Security",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "All permissions and security-related settings for Allai.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth().glassCard(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A102E))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val ctx = LocalContext.current
                SecurityRow(title = "Accessibility", subtitle = "Read/interact with any app screen", key = "perm_accessibility", onAction = { enabled ->
                    if (enabled) {
                        ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                })
                SecurityRow(title = "Camera", subtitle = "Take photos and scan", key = "perm_camera", onAction = { })
                SecurityRow(title = "Microphone", subtitle = "Voice commands", key = "perm_microphone", onAction = { })
                SecurityRow(title = "Contacts", subtitle = "Read and message contacts", key = "perm_contacts", onAction = { })
                SecurityRow(title = "SMS", subtitle = "Send and read text messages", key = "perm_sms", onAction = { })
                SecurityRow(title = "Files", subtitle = "Read and write device storage", key = "perm_files", onAction = { })
                SecurityRow(title = "Location", subtitle = "Know where you are", key = "perm_location", onAction = { })
                SecurityRow(title = "Install Apps", subtitle = "Install new applications", key = "perm_install", onAction = { })
                SecurityRow(title = "Overlay", subtitle = "Show floating window on top", key = "perm_overlay", onAction = { enabled ->
                    if (enabled) {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                })
            }
        }
    }
}

@Composable
private fun SecurityRow(title: String, subtitle: String, key: String, onAction: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("allai_permissions", android.content.Context.MODE_PRIVATE)
    var enabled by remember(key) { mutableStateOf(prefs.getBoolean(key, true)) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { v ->
                enabled = v
                prefs.edit().putBoolean(key, v).apply()
                onAction(v)
            },
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
    var signedInEmail by remember { mutableStateOf("") }
    var emailField by remember { mutableStateOf("") }
    var passwordField by remember { mutableStateOf("") }

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

    Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Account",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Log in with your Google account like on any AI provider.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth().glassCard(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A102E))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Sign in",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (googleAccount != null) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Signed in as:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = googleAccount!!.displayName ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = googleAccount!!.email ?: "No email",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

                HorizontalDivider()

                Text(
                    text = "Or sign in with email",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = emailField,
                    onValueChange = { emailField = it },
                    label = { Text(text = "Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                OutlinedTextField(
                    value = passwordField,
                    onValueChange = { passwordField = it },
                    label = { Text(text = "Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                Button(
                    onClick = {
                        val email = emailField
                        val password = passwordField
                        if (email.isNotBlank() && password.isNotBlank()) {
                            signedInEmail = email
                            emailField = ""
                            passwordField = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(text = "Sign in")
                }
                if (signedInEmail.isNotEmpty()) {
                    Text(
                        text = "Signed in with email: $signedInEmail",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun IntegrationsSettingsScreen() {
    Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Integrations",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "All the possible integrations Allai can connect to on Android.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth().glassCard(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A102E))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                IntegrationRow(title = "Shell / Terminal", subtitle = "Run commands on your device", key = "int_terminal")
                IntegrationRow(title = "File System", subtitle = "Read and write files", key = "int_filesystem")
                IntegrationRow(title = "Contacts", subtitle = "Access your contacts", key = "int_contacts")
                IntegrationRow(title = "SMS", subtitle = "Send text messages", key = "int_sms")
                IntegrationRow(title = "App Launcher", subtitle = "Open any installed app", key = "int_launcher")
                IntegrationRow(title = "MCP Servers", subtitle = "Connect to Model Context Protocol servers", key = "int_mcp")
                IntegrationRow(title = "Accessibility", subtitle = "Interact with other apps' UI", key = "int_accessibility")
            }
        }
    }
}

@Composable
private fun IntegrationRow(title: String, subtitle: String, key: String) {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("allai_integrations", android.content.Context.MODE_PRIVATE)
    var enabled by remember(key) { mutableStateOf(prefs.getBoolean(key, true)) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { v ->
                enabled = v
                prefs.edit().putBoolean(key, v).apply()
            },
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    title: String,
    desc: String,
    action: String,
    onTitleChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onActionChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Memory") },
        text = {
            Column(modifier = Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(text = "Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = onDescChange,
                    label = { Text(text = "Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = action,
                    onValueChange = onActionChange,
                    label = { Text(text = "Action (e.g. shell, sms, launcher)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onSave(title, desc, action)
            }) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
private fun SkillsDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    value: String,
    onValueChange: (String) -> Unit
) {
    var skillText by remember { mutableStateOf(value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Manage Skills / Agent Instructions") },
        text = {
            OutlinedTextField(
                value = skillText,
                onValueChange = { skillText = it; onValueChange(it) },
                label = { Text(text = "Skill / Instruction") },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                maxLines = 8
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(skillText) }) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}


@Composable
fun MemoryCard(
    memory: Memory,
    store: MemoryStore,
    onRefresh: () -> Unit
) {
    var fav by remember { mutableStateOf(memory.favorite) }
    var showingEdit by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(memory.title) }
    var editDesc by remember { mutableStateOf(memory.description) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A102E).copy(alpha = 0.8f),
                        Color(0xFF0F0A1E).copy(alpha = 0.9f)
                    )
                ),
                MaterialTheme.shapes.large
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF7B2CBF).copy(alpha = 0.4f),
                        Color(0xFF00D4FF).copy(alpha = 0.4f)
                    )
                ),
                MaterialTheme.shapes.large
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A102E))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = memory.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AllaiOnSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {
                        store.toggleFavorite(memory.id)
                        fav = !fav
                        onRefresh()
                    }) {
                        Text(text = if (fav) "\u2605" else "\u2606", color = Color(0xFFFF4D4D))
                    }
                    IconButton(onClick = {
                        editTitle = memory.title
                        editDesc = memory.description
                        showingEdit = true
                    }) {
                        Text(text = "\u270F", color = AllaiSecondary)
                    }
                    IconButton(onClick = {
                        store.deleteMemory(memory.id)
                        onRefresh()
                    }) {
                        Text(text = "\uD83D\uDDD1", color = Color(0xFFFF4D4D))
                    }
                }
            }
            Text(
                text = memory.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AllaiOnSurfaceVariant
            )
        }
    }

    if (showingEdit) {
        EditMemoryDialog(
            onDismiss = { showingEdit = false },
            onSave = { title, desc ->
                store.updateMemory(memory.id, title, desc)
                showingEdit = false
                onRefresh()
            },
            title = editTitle,
            desc = editDesc,
            onTitleChange = { editTitle = it },
            onDescChange = { editDesc = it }
        )
    }
}

@Composable
fun LearnedActionCard(
    action: LearnedAction,
    store: MemoryStore,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A102E).copy(alpha = 0.8f),
                        Color(0xFF0F0A1E).copy(alpha = 0.9f)
                    )
                ),
                MaterialTheme.shapes.large
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF7B2CBF).copy(alpha = 0.4f),
                        Color(0xFF00D4FF).copy(alpha = 0.4f)
                    )
                ),
                MaterialTheme.shapes.large
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A102E))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = action.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = AllaiOnSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {
                        val result = PluginManager.run(action.tool, action.arguments)
                        if (result.ok) {
                            store.recordAction(action.name, action.description, action.tool, action.arguments)
                            onRefresh()
                        }
                    }) {
                        Text(text = "\u25B6", color = AllaiSecondary)
                    }
                    IconButton(onClick = {
                        store.deleteLearnedAction(action.id)
                        onRefresh()
                    }) {
                        Text(text = "\uD83D\uDDD1", color = Color(0xFFFF4D4D))
                    }
                }
            }
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AllaiOnSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Tool: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = AllaiTertiary
                )
                Text(
                    text = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(action.lastUsed)),
                    style = MaterialTheme.typography.bodySmall,
                    color = AllaiOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    title: String,
    desc: String,
    onTitleChange: (String) -> Unit,
    onDescChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Memory") },
        text = {
            Column(modifier = Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(text = "Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = onDescChange,
                    label = { Text(text = "Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onSave(title, desc)
            }) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
