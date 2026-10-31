package com.allai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.allai.app.theme.AllaiTheme
import com.allai.app.ui.AgentScreen
import com.allai.app.ui.TerminalScreen
import com.allai.app.ui.SettingsScreen
import com.allai.app.ui.Scaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AllaiTheme {
                MainNav()
            }
        }
    }
}

@Composable
private fun MainNav() {
    var selected by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                    label = { Text("Chat") },
                    selected = selected == 0,
                    onClick = { selected = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    label = { Text("Terminal") },
                    selected = selected == 1,
                    onClick = { selected = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = selected == 2,
                    onClick = { selected = 2 }
                )
            }
        }
    ) {
        when (selected) {
            0 -> AgentScreen()
            1 -> TerminalScreen()
            else -> SettingsScreen()
        }
    }
}
