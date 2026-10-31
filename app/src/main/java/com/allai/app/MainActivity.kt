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
import com.allai.app.settings.SettingsState
import com.allai.app.theme.AllaiTheme
import com.allai.app.ui.AgentScreen
import com.allai.app.ui.TerminalScreen
import com.allai.app.ui.SettingsScreen
import com.allai.app.ui.Scaffold

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
    var selected by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Text(text = "Chat") },
                    label = { Text("Chat") },
                    selected = selected == 0,
                    onClick = { selected = 0 }
                )
                NavigationBarItem(
                    icon = { Text(text = "Term") },
                    label = { Text("Terminal") },
                    selected = selected == 1,
                    onClick = { selected = 1 }
                )
                NavigationBarItem(
                    icon = { Text(text = "Set") },
                    label = { Text("Settings") },
                    selected = selected == 2,
                    onClick = { selected = 2 }
                )
            }
        }
    ) {
        when (selected) {
            0 -> AgentScreen(settings)
            1 -> TerminalScreen()
            else -> SettingsScreen(settings)
        }
    }
}
