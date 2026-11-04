package com.allai.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allai.app.model.Message
import com.allai.app.model.Role
import com.allai.app.settings.SettingsState
import com.allai.app.theme.AllaiTheme
import com.allai.app.theme.AllaiBackground
import com.allai.app.ui.AgentScreen
import com.allai.app.ui.MemoriesScreen
import com.allai.app.ui.OnboardingScreen
import com.allai.app.ui.SettingsTabsScreen
import com.allai.app.ui.TerminalScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsState(this)
        setContent {
            AllaiTheme {
                MainNav(settings)
            }
        }
    }
}

@Composable
private fun MainNav(settings: SettingsState) {
    val context = LocalContext.current
    val prefs: SharedPreferences = context.getSharedPreferences("allai_onboarding", android.content.Context.MODE_PRIVATE)
    var onboardingComplete by remember { mutableStateOf(prefs.getBoolean("onboarding_complete", false)) }

    if (!onboardingComplete || settings.apiKey.isBlank()) {
        OnboardingScreen(onComplete = {
            prefs.edit().putBoolean("onboarding_complete", true).apply()
            onboardingComplete = true
        })
    } else {
        var selected by remember { mutableIntStateOf(0) }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (selected) {
                    0 -> AgentScreen(settings)
                    1 -> TerminalScreen()
                    2 -> SessionsScreen(settings)
                    3 -> SettingsTabsScreen(settings, { selected = it })
                }
            }

            BottomBar(selected = selected, onTabSelected = { selected = it })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionsScreen(settings: SettingsState) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("allai_sessions", Context.MODE_PRIVATE)
    val gson: com.google.gson.Gson = remember { com.google.gson.Gson() }
    val sessions = remember {
        mutableStateListOf<SessionInfo>().also {
            val json = prefs.getString("sessions", "[]") ?: "[]"
            val type = object : com.google.gson.reflect.TypeToken<List<SessionInfo>>() {}.type
            val parsed = gson.fromJson(json, type) ?: emptyList<SessionInfo>()
            it.addAll(parsed)
        }
    }
    var newSession by remember { mutableStateOf(false) }
    var sessionTitle by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(AllaiBackground)) {
        TopAppBar(
            title = { Text(text = "Sessions", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AllaiBackground),
            modifier = Modifier.fillMaxWidth(),
            actions = {
                IconButton(onClick = { newSession = true }) {
                    Text(text = "✚", fontSize = 20.sp)
                }
            }
        )

        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "No sessions yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Start a chat from the Chat tab to create a session.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSessionSelected(session, settings) },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A102E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = session.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "${session.messageCount} messages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(session.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (newSession) {
            AlertDialog(
                onDismissRequest = { newSession = false },
                title = { Text(text = "New Session") },
                text = {
                    OutlinedTextField(
                        value = sessionTitle,
                        onValueChange = { sessionTitle = it },
                        label = { Text(text = "Session title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (sessionTitle.isNotBlank()) {
                            val id = java.util.UUID.randomUUID().toString()
                            val session = SessionInfo(id = id, title = sessionTitle, messageCount = 0, timestamp = System.currentTimeMillis(), messages = emptyList())
                            sessions.add(session)
                            saveSessions(sessions, prefs, gson)
                            newSession = false
                            sessionTitle = ""
                        }
                    }) {
                        Text(text = "Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { newSession = false }) {
                        Text(text = "Cancel")
                    }
                }
            )
        }
    }
}

private fun onSessionSelected(session: SessionInfo, settings: SettingsState) {
    // Load session messages into AgentScreen chat; handled via navigating to Chat with preloaded messages
    // For now, switch to Chat tab (agent will show; session restore wired via SessionStore if extended)
}

private fun saveSessions(sessions: List<SessionInfo>, prefs: android.content.SharedPreferences, gson: com.google.gson.Gson) {
    prefs.edit().putString("sessions", gson.toJson(sessions)).apply()
}

data class SessionInfo(
    val id: String,
    val title: String,
    val messageCount: Int,
    val timestamp: Long,
    val messages: List<Message> = emptyList()
)

@Composable
private fun BottomBar(selected: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .padding(bottom = 16.dp)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomTab(
            modifier = Modifier.weight(1f),
            text = "Chat",
            selected = selected == 0,
            onClick = { onTabSelected(0) }
        )
        BottomTab(
            modifier = Modifier.weight(1f),
            text = "Terminal",
            selected = selected == 1,
            onClick = { onTabSelected(1) }
        )
        BottomTab(
            modifier = Modifier.weight(1f),
            text = "Sessions",
            selected = selected == 2,
            onClick = { onTabSelected(2) }
        )
        BottomTab(
            modifier = Modifier.weight(1f),
            text = "Settings",
            selected = selected == 3,
            onClick = { onTabSelected(3) }
        )
    }
}


@Composable
private fun BottomTab(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (selected) {
                Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
